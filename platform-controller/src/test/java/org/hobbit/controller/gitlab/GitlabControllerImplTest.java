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
        gitlabProject.setId(404);
        gitlabProject.setName("qabenchmarkcontrollerfortesting");
        gitlabProject.setNameWithNamespace("Mohammed Abdelgadir / qabenchmarkcontrollerfortesting");
        gitlabProject.setDescription("(test) QA benchmark-controller (ttl) and image");
        gitlabProject.setDefaultBranch("master");

        GitlabUser owner = new GitlabUser();
        owner.setUsername("sjdff1");
        owner.setName("sjdff1");
        owner.setState("active");
        owner.setAvatarUrl("https://secure.gravatar.com/avatar/248dc7b4d869ff34dd97a1f07c00dd63?s=80&d=identicon");

        gitlabProject.setOwner(owner);
        gitlabProject.setPath("qabenchmarkcontrollerfortesting");
        gitlabProject.setVisibilityLevel(20);
        gitlabProject.setPathWithNamespace("weekmo/qabenchmarkcontrollerfortesting");
        gitlabProject.setSshUrl("git@git.project-hobbit.eu:weekmo/qabenchmarkcontrollerfortesting.git");
        gitlabProject.setWebUrl("https://git.project-hobbit.eu/weekmo/qabenchmarkcontrollerfortesting");
        gitlabProject.setHttpUrl("https://git.project-hobbit.eu/weekmo/qabenchmarkcontrollerfortesting.git");

        GitlabNamespace gitlabNamespace = new GitlabNamespace();
        gitlabNamespace.setName("weekmo");
        gitlabNamespace.setPath("weekmo");

        gitlabProject.setNamespace(gitlabNamespace);
        gitlabProject.setCreatorId(90);
        gitlabProject.setStarCount(0);
        gitlabProject.setForksCount(0);
        gitlabProject.setTagList(new ArrayList<>());
        gitlabProject.setSharedWithGroups(new ArrayList<>());

        gitlabBranch = new GitlabBranch();
        gitlabBranch.setName("master");
        GitlabBranchCommit gitlabCommit = new GitlabBranchCommit();
        gitlabCommit.setId("2a36977cb8307a2d51cc2bfdb426457d99d3a51d");
        gitlabCommit.setMessage("change the name");
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
        assert(project.getName().equals("Mohammed Abdelgadir / qabenchmarkcontrollerfortesting"));
    }

    @Test
    public void getCheckedModel() throws IOException {
        byte[] benchmarkCfgBytes = api.getRawFileContent(gitlabProject.getId(), gitlabBranch.getCommit().getId(), "benchmark.ttl");
        Model checkedModel = controller.getCheckedModel(benchmarkCfgBytes, "benchmark", gitlabProject.getWebUrl());
        Resource resource = new ResourceImpl("http://w3id.org/gerbil/qa/hobbit/vocab#hasQuestionLanguage");
        Property property = new PropertyImpl("http://www.w3.org/2000/01/rdf-schema#label");
        assert(checkedModel.contains(resource, property));
    }
}
