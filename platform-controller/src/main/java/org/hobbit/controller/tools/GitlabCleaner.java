package org.hobbit.controller.tools;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabRepositoryFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitlabCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabCleaner.class);

    public GitlabCleaner() {
        super();
    }

    public void run(String url, String token) {
        final GitlabAPI api = GitlabAPI.connect(url, token);

        // https://docs.gitlab.com/ee/api/projects.html#list-all-projects
        // Get a list of all visible projects across GitLab for the authenticated user.
        // When accessed without authentication, only public projects with simple fields
        // are returned.
        LOGGER.info("Requesting projects...");
        List<GitlabProject> gitProjects = api.getProjects();
        int projectsCount = gitProjects.size();

        List<GitlabProject> spamProjects = gitProjects.stream()
                // Filter all projects that couldn't be converted
                .filter(p -> isSpamProject(p, api))
                // Put remaining projects in a map
                .collect(Collectors.toList());

        for (GitlabProject project : spamProjects) {
            LOGGER.info("Deleting {}...", project.getNameWithNamespace());
            // api.deleteProject(project.getId());
        }
        LOGGER.info("Deleted {}/{} projects", spamProjects.size(), projectsCount);
    }

    protected boolean isSpamProject(GitlabProject project, GitlabAPI api) {
        return hasNoFile(project, api) && hasNoImage(project, api);
    }

    protected boolean hasNoFile(GitlabProject project, GitlabAPI api) {
        String defaultBranch = project.getDefaultBranch();
        // If there is no default branch
        if ((defaultBranch == null) || ("".equals(defaultBranch))) {
            return true;
        }

        GitlabBranch branch;
        try {
            branch = api.getBranch(project, project.getDefaultBranch());
        } catch (Exception e) {
            // there is no default graph -> the project is empty
            return true;
        }
        return !hasFile("system.ttl", project, branch, api) && !hasFile("benchmark.ttl", project, branch, api);
    }

    protected boolean hasFile(String fileName, GitlabProject project, GitlabBranch branch, GitlabAPI api) {
        GitlabRepositoryFile file = null;
        try {
            file = api.getRepositoryFile(project, fileName, branch.getName());
        } catch (Exception e) {
            // nothing to do
        }
        return ((file != null) && (file.getFileName().contains(fileName)));
    }

    protected boolean hasNoImage(GitlabProject project, GitlabAPI api) {
        return !project.isContainerRegistryEnabled();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(
                    "Error: wrong usage. The following parameters are necessary:\n <gitlab-url> <security-token>");
            return;
        }
        String url = args[0];
        String token = args[1];

        GitlabCleaner cleaner = new GitlabCleaner();
        cleaner.run(url, token);
    }
}
