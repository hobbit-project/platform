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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Timofey Ermilov on 17/10/2016.
 */
public class GitlabControllerImplTest {
    GitlabControllerImpl controller;
    GitlabProject gitlabProject;
    GitlabBranch gitlabBranch;
    GitlabAPI api;

    @Before
    public void init() throws InterruptedException {
        gitlabProject = new GitlabProject();
        gitlabProject.setId(526);
        gitlabProject.setName("testing-benchmark");
        gitlabProject.setNameWithNamespace("gitadmin / testing-benchmark");
        gitlabProject.setDescription("A benchmark for platform-controller tests.");
        gitlabProject.setDefaultBranch("master");

        GitlabUser owner = new GitlabUser();
        owner.setUsername("gitadmin");
        owner.setName("gitadmin");
        owner.setState("active");
        owner.setAvatarUrl("https://secure.gravatar.com/avatar/73ee2127ad0f938995a7b5fb9b3417d8?s=46&d=identicon");

        gitlabProject.setOwner(owner);
        gitlabProject.setPath("testing-benchmark");
        gitlabProject.setVisibility("public");
        gitlabProject.setPathWithNamespace("gitadmin/testing-benchmark");
        gitlabProject.setSshUrl("git@git.project-hobbit.eu:gitadmin/testing-benchmark.git");
        gitlabProject.setWebUrl("https://git.project-hobbit.eu/gitadmin/testing-benchmark");
        gitlabProject.setHttpUrl("https://git.project-hobbit.eu/gitadmin/testing-benchmark.git");

        GitlabNamespace gitlabNamespace = new GitlabNamespace();
        gitlabNamespace.setName("gitadmin");
        gitlabNamespace.setPath("gitadmin");

        gitlabProject.setNamespace(gitlabNamespace);
        gitlabProject.setCreatorId(90);
        gitlabProject.setStarCount(0);
        gitlabProject.setForksCount(0);
        gitlabProject.setTagList(new ArrayList<>());
        gitlabProject.setSharedWithGroups(new ArrayList<>());

        gitlabBranch = new GitlabBranch();
        gitlabBranch.setName("master");
        GitlabBranchCommit gitlabCommit = new GitlabBranchCommit();
        gitlabCommit.setId("2192a05e8ecda70511b2aa112e6618d0f3de3220");
        gitlabCommit.setMessage("Fix syntax errors");
        gitlabBranch.setCommit(gitlabCommit);

        api = GitlabAPI.connect("https://git.project-hobbit.eu/", "fykySfxWaUyCS1xxTSVy");


        controller = new GitlabControllerImpl(false, false);
        // wait for controller to fetch projects
    }

    //@Test
    //public void getAllProjects() throws InterruptedException {
    //    controller.startFetchingProjects();
    //    Thread.sleep(20000);
    //    List<Project> projects = controller.getAllProjects();
    //    System.out.println(projects);
    //    assert(!projects.isEmpty());
    //    assert(projects.size() > 10);
    //}

    @Test
    public void gitlabToProject() {
        Project project = controller.gitlabToProject(gitlabProject);
        assertNotNull("Project shouldn't be null", project);
        assertEquals("Project name",
                "gitadmin / testing-benchmark", project.getName());
    }

    @Test
    public void getCheckedModel() throws IOException {
        byte[] benchmarkCfgBytes = api.getRawFileContent(gitlabProject.getId(), gitlabBranch.getCommit().getId(), "benchmark.ttl");
        Model checkedModel = controller.getCheckedModel(benchmarkCfgBytes, "benchmark", gitlabProject.getWebUrl());
        Resource resource = new ResourceImpl("http://w3id.org/hobbit/platform-benchmark/vocab#seed");
        Property property = new PropertyImpl("http://www.w3.org/2000/01/rdf-schema#label");
        assert checkedModel.contains(resource, property) :
                "Benchmark model should contain " + resource + " " + property;
    }
}
