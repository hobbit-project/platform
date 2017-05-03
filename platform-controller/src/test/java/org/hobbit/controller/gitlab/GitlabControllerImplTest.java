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
        Thread.sleep(10000);
    }

    @Test
    public void getAllProjects() {
        List<Project> projects = controller.getAllProjects();
        assertNotNull(projects);
        for(Project project : projects) {
            System.out.println(project.name.toString());
        }
    }
}
