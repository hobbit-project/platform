/**
 * This file is part of platform-controller.
 *
 * platform-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * platform-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with platform-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.controller.gitlab;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.Pagination;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;
import org.gitlab.api.query.ProjectsQuery;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Created by Timofey Ermilov on 17/10/2016.
 */
public class GitlabControllerImpl implements GitlabController {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabControllerImpl.class);

    private static final String ISO_6801_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssX";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_6801_DATE_PATTERN);

    public static final String GITLAB_URL_KEY = "GITLAB_URL";
    public static final String GITLAB_TOKEN_KEY = "GITLAB_TOKEN";

    // Gitlab URL and access token
    private static final String GITLAB_URL = System.getProperty(GITLAB_URL_KEY, "https://git.project-hobbit.eu/");
    private static final String GITLAB_TOKEN = System.getenv(GITLAB_TOKEN_KEY);
    private static final String GITLAB_DEFAULT_GUEST_TOKEN = "fykySfxWaUyCS1xxTSVy";

    // HobbitConfig filenames
    private static final String SYSTEM_CONFIG_FILENAME = "system.ttl";
    private static final String BENCHMARK_CONFIG_FILENAME = "benchmark.ttl";

    private static final int MAX_PARSING_ERRORS = 50;
    private static final int MAX_SIZE_OF_PROJECT_VISIBILITY_CHACHE = 50;
    private static final int VISIBILITY_CACHE_ELEMENT_LIFETIME_IN_SECS = 30;

    protected static final String GITLAB_VISIBILITY_PUBLIC = "public";
    protected static final String GITLAB_VISIBILITY_PROTECTED = "internal";
    protected static final String GITLAB_VISIBILITY_PRIVATE = "private";

    // gitlab api
    private GitlabAPI api;
    // projects refresh timer
    private Timer timer;
    private int repeatInterval = 60 * 1000; // every 1 min
    private boolean projectsFetched = false; // indicates whether projects was
                                             // fetched first time
    private List<Runnable> readyRunnable;
    private Date mostRecentLastActivityAt;
    // projects map
    private Map<String, Project> projects;
    private Set<String> parsingErrors = new HashSet<String>();
    private Deque<String> sortedParsingErrors = new LinkedList<String>();
    private LoadingCache<String, Set<String>> visibleProjectsCache;

    public GitlabControllerImpl() {
        this(GITLAB_TOKEN, true, true);
    }

    public GitlabControllerImpl(boolean startFetchingProjects, boolean useCache) {
        this(GITLAB_TOKEN, startFetchingProjects, useCache);
    }

    public GitlabControllerImpl(String token, boolean startFetchingProjects, boolean useCache) {
        if (token == null || token.isEmpty()) {
            // use default "guest" token, to use openly available projects
            token = GITLAB_DEFAULT_GUEST_TOKEN;
        }
        api = GitlabAPI.connect(GITLAB_URL, token);
        timer = new Timer();
        projects = new HashMap<>();
        readyRunnable = new ArrayList<>();

        if (useCache) {
            visibleProjectsCache = CacheBuilder.newBuilder()
                    .expireAfterAccess(VISIBILITY_CACHE_ELEMENT_LIFETIME_IN_SECS, TimeUnit.SECONDS)
                    .maximumSize(MAX_SIZE_OF_PROJECT_VISIBILITY_CHACHE).build(new CacheLoader<String, Set<String>>() {
                        @Override
                        public Set<String> load(String mail) throws Exception {
                            return getProjectsOfUser(mail);
                        }
                    });
        } else {
            visibleProjectsCache = null;
        }

        // start fetching projects
        if (startFetchingProjects) {
            startFetchingProjects();
        }
    }

    public void runAfterFirstFetch(Runnable r) {
        readyRunnable.add(r);
    }

    public void startFetchingProjects() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fetchProjects();
            }
        }, 0, repeatInterval);
    }

    protected void fetchProjects() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Project> newProjects = Collections.EMPTY_MAP;
            try {
                ProjectsQuery query = new ProjectsQuery();
                // FIXME: this approach may lead to desynchronization between local and remote project list:
                // - time is not reliable
                // - when a project is removed from GitLab, we may not see that
                if (mostRecentLastActivityAt != null) {
                    query.append("last_activity_after", dateFormat.format(mostRecentLastActivityAt));
                }
                LOGGER.info("Projects query: '{}'", query);
                // https://docs.gitlab.com/ee/api/projects.html#list-all-projects
                // Get a list of all visible projects across GitLab for the authenticated user.
                // When accessed without authentication, only public projects with simple fields are returned.
                List<GitlabProject> gitProjects = api.getProjects(query);

                newProjects = gitProjects.parallelStream()
                        // Map every gitlab project to a project object (or null_)
                        .map(p -> gitlabToProject(p))
                        // Filter all projects that couldn't be converted
                        .filter(p -> p != null)
                        // Filter all projects that didn't contain any benchmark or system definition
                        .filter(p -> ((p.benchmarkModel != null)
                                && (p.benchmarkModel.contains(null, RDF.type, HOBBIT.Benchmark)))
                                || ((p.systemModel != null)
                                        && (p.systemModel.contains(null, RDF.type, HOBBIT.SystemInstance))))
                        // Put remaining projects in a map
                        .collect(Collectors.toMap(p -> p.getName(), Function.identity()));
            } catch (Exception | Error e) {
                LOGGER.error("Couldn't get GitLab projects from {}.", GITLAB_URL, e);
                // Do not replace previously fetched project list.
                return;
            }

            if (projects == null) {
                // This is the first fetching of projects -> we might
                // have
                // to notify threads that are waiting for that
                projects = newProjects;
                synchronized (this) {
                    this.notifyAll();
                }
            } else {
                // update cached version
                projects.putAll(newProjects);
            }
            // indicate that projects were fetched
            if (!projectsFetched) {
                projectsFetched = true;
                for (Runnable r : readyRunnable) {
                    r.run();
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Got an uncatched throwable.", t);
        }
    }

    /**
     * Collects the URIs of benchmarks and systems defined within the given project
     * element and streams them as pairs of the benchmark and system URIs as keys
     * and the given project object as value.
     *
     * @param p
     *            the project from which the benchmark and system URIs should be
     *            read
     * @return a stream of pairs of found URIs and the given project object
     */
    protected static Stream<Pair<String, Project>> listUris(Project p) {
        List<Resource> benchmarks = RdfHelper.getSubjectResources(p.benchmarkModel, RDF.type, HOBBIT.Benchmark);
        List<Resource> systems = RdfHelper.getSubjectResources(p.systemModel, RDF.type, HOBBIT.SystemInstance);
        Stream<Resource> resources = benchmarks != null
                ? (systems != null ? Stream.concat(benchmarks.stream(), systems.stream()) : benchmarks.stream())
                : (systems != null ? systems.stream() : Stream.empty());
        return resources.map(r -> Pair.of(r.getURI(), p));
    }

    @Override
    public void stopFetchingProjects() {
        timer.cancel();
        timer.purge();
    }

    @Override
    public List<Project> getAllProjects() {
        if (projects == null) {
            // The projects don't have been fetched -> we should wait until this
            // has been done but with a maximum of 10 seconds.
            synchronized (this) {
                try {
                    this.wait(10000);
                } catch (InterruptedException e) {
                }
            }
        }
        return new ArrayList<>(projects.values());
    }

    @Override
    public Project gitlabToProject(GitlabProject project) {
        String name = project.getNameWithNamespace();
        Date lastActivityAt = project.getLastActivityAt();
        if (mostRecentLastActivityAt == null || lastActivityAt.after(mostRecentLastActivityAt)) {
            mostRecentLastActivityAt = lastActivityAt;
        }
        // get default branch
        GitlabBranch b;
        try {
            b = api.getBranch(project, project.getDefaultBranch());
        } catch (Exception e) {
            // there is no default graph -> the project is empty
            // we can return null and don't have to log this error
            return null;
        }
        String commitId;
        try {
            commitId = b.getCommit().getId();
            if (commitId == null) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        if (projects != null) {
            Project oldProject = projects.get(name);
            if (oldProject != null) {
                if (oldProject.getCommitId().equals(commitId)) {
                    return oldProject;
                }
            }
        }
        // read system config
        Model systemModel = null;
        try {
            byte[] systemCfgBytes = api.getRawFileContent(project, commitId, SYSTEM_CONFIG_FILENAME);
            systemModel = getCheckedModel(systemCfgBytes, "system", project.getWebUrl());
        } catch (Exception e) {
            LOGGER.debug("system.ttl configuration file NOT FOUND in {}", project.getWebUrl());
        }
        // read benchmark config
        Model benchmarkModel = null;
        try {
            byte[] benchmarkCfgBytes = api.getRawFileContent(project, commitId, BENCHMARK_CONFIG_FILENAME);
            benchmarkModel = getCheckedModel(benchmarkCfgBytes, "benchmark", project.getWebUrl());
        } catch (Exception e) {
            LOGGER.debug("benchmark.ttl configuration file NOT FOUND in {}", project.getWebUrl());
        }
        if ((benchmarkModel != null) || (systemModel != null)) {
            // get user
            String user = null;
            GitlabUser owner = project.getOwner();
            if (owner != null) {
                user = owner.getEmail();
            } else {
                String warning = "The project " + name + " has no owner.";
                handleErrorMsg(warning, null, false);
            }
            Project p = new Project(benchmarkModel, systemModel, user, name,
                    project.getCreatedAt(), project.getVisibility() == GITLAB_VISIBILITY_PRIVATE, commitId);
            return p;
        } else {
            // There is no data which is interesting for us. We can ignore this project.
            return null;
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        GitlabControllerImpl c = new GitlabControllerImpl(System.getenv(GITLAB_TOKEN), true, true);
        Thread.sleep(40000);
        List<Project> projects = c.getAllProjects();
        c.stopFetchingProjects();
        for (Project p : projects) {
            System.out.println(p.getName());
        }
//        LOGGER.info("Request Systems for user gerbil@informatik.uni-leipzig.de");
//        Set<String> userProjects = c.getProjectsOfUser("gerbil@informatik.uni-leipzig.de");
//        LOGGER.info("Found {} projects", userProjects.size());
//        for (String p : userProjects) {
//            System.out.println(p);
//        }
    }

    @Override
    public Model getCheckedModel(byte modelData[], String modelType, String projectName) {
        try {
            Model model = ModelFactory.createDefaultModel();
            model.read(new ByteArrayInputStream(modelData), null, "TTL");
            return model;
        } catch (Exception e) {
            String parsingError = "Couldn't parse " + modelType + " model from " + projectName
                    + ". It won't be available. " + e.getMessage();
            handleErrorMsg(parsingError, e, true);
        }
        return null;
    }

    /**
     * Method for handling errors occurring when crawling gitlab (mainly parsing
     * errors). Since the class is crawling Gitlab regularly, logging errors every
     * time is not necessary.
     *
     * @param message
     *            the error message that should be logged
     * @param e
     *            the exception that should be logged (only if the error hasn't been
     *            logged before). Can be {@code null}.
     * @param logMsgIfAlreadyKnown
     *            if {@code true} the message will be logged even if it has been
     *            logged before. If the flag is set to {@code false} redundant
     *            messages will be ignored.
     */
    protected void handleErrorMsg(String message, Exception e, boolean logMsgIfAlreadyKnown) {
        if (parsingErrors.contains(message)) {
            if (logMsgIfAlreadyKnown) {
                LOGGER.info(message + " (Error already reported before)");
            }
        } else {
            if(e != null) {
                LOGGER.info(message, e);
            } else {
                LOGGER.info(message);
            }
            sortedParsingErrors.addLast(message);
            parsingErrors.add(message);
            // If the cached errors become to long, remove the oldest
            if (sortedParsingErrors.size() > MAX_PARSING_ERRORS) {
                parsingErrors.remove(sortedParsingErrors.pop());
            }
        }
    }

    /**
     * Workaround an exception thrown by api.getUser().isAdmin().
     *
     * @return whether the platform's gitlab user is admin
     * @throws IOException
     *             If the Gitlab API throws an IOException
     */
    protected boolean isAdmin() throws IOException {
        GitlabUser user = api.getUser();
        if (user == null) {
            return false;
        }
        try {
            return user.isAdmin();
        } catch (NullPointerException e) {
            // that happens when no gitlab account is configured for platform
            return false;
        }
    }

    /**
     * If the Gitlab API is used with an admin account, this method returns the
     * names of the projects that are visible for this user. Otherwise, it returns
     * all project names that are known.
     *
     * @param mail
     *            the e-mail address of the user for which the project names should
     *            be retrieved
     * @return a set of project names (including the namespace)
     * @throws IOException
     *             If the Gitlab API throws an IOException
     */
    protected Set<String> getProjectsOfUser(String mail) throws IOException {
        // If we have admin access
        if (isAdmin()) {
            GitlabUser user = getUserByMail(mail);
            if (user == null) {
                LOGGER.warn("Couldn't find user with mail \"{}\". returning empty list of projects.", mail);
                return Collections.EMPTY_SET;
            }
            // List<GitlabProject> gitProjects = api.getProjectsViaSudo(user);
            List<GitlabProject> gitProjects = getProjectsVisibleForUser(user);
            Set<String> projectNames = new HashSet<String>();
            for (GitlabProject p : gitProjects) {
                projectNames.add(p.getNameWithNamespace());
            }
            return projectNames;
        } else {
            // We can not check the access of a single user. Simply return all known
            // projects.
            return (projects != null) ? projects.keySet() : Collections.EMPTY_SET;
        }
    }

    /**
     * Tries to find the GitlabUser with the given mail address.
     *
     * @param mail
     *            the mail address of the user
     * @return the GitlabUser object for the user or {@code null} if it could not be
     *         found.
     * @throws IOException
     *             If the Gitlab API throws an IOException
     */
    protected GitlabUser getUserByMail(String mail) throws IOException {
        if (mail == null) {
            return null;
        }
        List<GitlabUser> users = api.getUsers();
        for (GitlabUser user : users) {
            String email = user.getEmail();
            if (email != null && email.equals(mail)) {
                return user;
            }
        }
        return null;
    }

    public List<Project> getProjectsVisibleForUser(String mail) {
        Set<String> projectNames;
        try {
            if (visibleProjectsCache != null) {
                projectNames = visibleProjectsCache.get(mail);
            } else {
                projectNames = getProjectsOfUser(mail);
            }
        } catch (IOException e) {
            LOGGER.error("Exception while trying to retrieve projects of the user with the mail \"" + mail
                    + "\". Returning null.", e);
            return null;
        } catch (ExecutionException e) {
            LOGGER.error("Exception while trying to retrieve projects of the user with the mail \"" + mail
                    + "\". Returning null.", e);
            return null;
        }
        List<Project> userProjects = projects.values().parallelStream()
                .filter(p -> ((!p.isPrivate) || projectNames.contains(p.name))).collect(Collectors.toList());
        return userProjects;
    }

    protected List<GitlabProject> getProjectsVisibleForUser(GitlabUser user) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(GitlabUser.URL);
        builder.append('/');
        builder.append(user.getId());
        builder.append("/projects");
        builder.append(new Pagination().withPerPage(Pagination.MAX_ITEMS_PER_PAGE).toString());
        builder.append("&simple=true");
        return api.retrieve().getAll(builder.toString(), GitlabProject[].class);
    }

}
