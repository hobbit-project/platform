package org.hobbit.controller.gitlab;

import java.io.FileNotFoundException;
import java.util.*;

import org.apache.commons.io.Charsets;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Timofey Ermilov on 17/10/2016.
 */
public class GitlabControllerImpl implements GitlabController {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabControllerImpl.class);

    // Gitlab URL and access token
    private static final String GITLAB_URL = System.getProperty("GITLAB_URL", "https://git.project-hobbit.eu/");
    private static final String GITLAB_TOKEN = System.getProperty("GITLAB_TOKEN", "QHQnDKujW3GbsVPZMoNv");

    // Config filenames
    private static final String SYSTEM_CONFIG_FILENAME = "system.ttl";
    private static final String BENCHMARK_CONFIG_FILENAME = "benchmark.ttl";

    // gitlab api
    private GitlabAPI api;
    // projects refresh timer
    private Timer timer;
    private int repeatInterval = 60 * 1000; // every 1 min
    // projects array
    private List<Project> projects;

    public GitlabControllerImpl() {
        api = GitlabAPI.connect(GITLAB_URL, GITLAB_TOKEN);
        timer = new Timer();
        projects = new ArrayList<>();

        // start fetching projects
        startFetchingProjects();
    }

    private void startFetchingProjects() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<Project> newProjects = new ArrayList<>();

                try {
                    List<GitlabProject> gitProjects = api.getAllProjects();
                    for (GitlabProject project : gitProjects) {
                        try {
                            Project p = gitlabToProject(project);
                            newProjects.add(p);
                        } catch (FileNotFoundException e) {
                            LOGGER.debug("No config files found in", project.getWebUrl());
                        } catch (Exception e) {
                            LOGGER.error("Error getting project config files", e);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Couldn't get all gitlab projects.", e);
                }

                // update cached version
                projects = newProjects;
            }
        }, 0, repeatInterval);
    }

    @Override
    public void stopFetchingProjects() {
        timer.cancel();
        timer.purge();
    }

    @Override
    public List<Project> getAllProjects() {
        return projects;
    }

    private Project gitlabToProject(GitlabProject project) throws Exception {
        // get default branch
        GitlabBranch b = api.getBranch(project, project.getDefaultBranch());
        // read system config
        byte[] systemCfgBytes = api.getRawFileContent(project, b.getCommit().getId(), SYSTEM_CONFIG_FILENAME);
        String systemCfgContent = new String(systemCfgBytes, Charsets.UTF_8);
        // read benchmark config
        byte[] benchmarkCfgBytes = api.getRawFileContent(project, b.getCommit().getId(), BENCHMARK_CONFIG_FILENAME);
        String benchmarkCfgContent = new String(benchmarkCfgBytes, Charsets.UTF_8);
        // get user
        String user = project.getOwner().getUsername();
        Project p = new Project(benchmarkCfgContent, systemCfgContent, user);
        return p;
    }
}
