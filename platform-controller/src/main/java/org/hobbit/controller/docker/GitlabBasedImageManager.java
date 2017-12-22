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
package org.hobbit.controller.docker;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hobbit.controller.gitlab.GitlabController;
import org.hobbit.controller.gitlab.GitlabControllerImpl;
import org.hobbit.controller.gitlab.Project;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ImageManager} implementation relying on the usage of a
 * {@link GitlabController}.
 * 
 * Created by Timofey Ermilov on 22/09/16.
 * 
 */
public class GitlabBasedImageManager extends AbstactImageManager implements ImageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabBasedImageManager.class);

    // gitlab access controller
    private GitlabControllerImpl gitlab;

    public GitlabBasedImageManager() {
        // instantiate gitlab
        gitlab = new GitlabControllerImpl();
    }

    public void runWhenGitlabIsReady(Runnable r) {
        gitlab.runAfterFirstFetch(r);
    }

    @Override
    protected List<BenchmarkMetaData> getUncheckedBenchmarks() {
        return gitlab.getAllProjects().parallelStream().filter(p -> p.benchmarkModel != null)
                .flatMap(p -> MetaDataFactory.modelToBenchmarkMetaData(p.benchmarkModel, p.name, p.createdAt).stream())
                .collect(Collectors.toList());
    }

    @Override
    protected List<SystemMetaData> getUncheckedSystems() {
        return gitlab.getAllProjects().parallelStream().filter(p -> p.systemModel != null)
                .flatMap(p -> MetaDataFactory.modelToSystemMetaData(p.systemModel, p.name, p.createdAt).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemMetaData> getSystemsOfUser(String userName) {
        Set<String> visibleProjects = gitlab.getProjectsVisibleForUser(userName).stream()
                // get all projects which have system information
                .filter(p -> p.systemModel != null)
                // get the project names
                .map(Project::getName)
                // get
                .collect(Collectors.toSet());
        return getFilteredSystems(s -> visibleProjects.contains(s.source));
    }
}
