package org.hobbit.controller.gitlab;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by Timofey Ermilov on 17/10/2016.
 */
public class GitlabControllerImplTest {
    GitlabControllerImpl controller;

    @Before
    public void init() throws InterruptedException {
        controller = new GitlabControllerImpl();
        // wait for controller to fetch projects
        Thread.sleep(2000);
    }

    @Test
    public void getAllProjects() {
        List<Project> projects = controller.getAllProjects();
        assertNotNull(projects);
    }
}
