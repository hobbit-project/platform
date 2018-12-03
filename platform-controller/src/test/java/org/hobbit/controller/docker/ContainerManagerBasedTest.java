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

import java.util.ArrayList;
import java.util.List;

import org.hobbit.controller.DockerBasedTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Timofey Ermilov on 01/09/16.
 */
public class ContainerManagerBasedTest extends DockerBasedTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerBasedTest.class);

    protected ContainerManagerImpl manager;
    protected List<String> containers = new ArrayList<String>();
    protected List<String> tasks = new ArrayList<String>();


    @Before
    public void initManager() throws Exception {
        //manager = new ContainerManagerImpl();
    }

    @Test
    public void pullImage(){
        manager.pullImage("git.project-hobbit.eu:4567/smirnp/sml-benchmark-v2/benchmark-controller");
    }

    @After
    public void cleanUp() {
        for (String taskId : tasks) {
            try {
                String serviceId = dockerClient.inspectTask(taskId).serviceId();
                dockerClient.removeService(serviceId);
            } catch (Exception e) {
                LOGGER.warn("Couldn't cleanup service with task {}", taskId, e);
            }
        }
        for (String containerId : containers) {
            try {
                dockerClient.stopContainer(containerId, 5);
                dockerClient.removeContainer(containerId);
            } catch (Exception e) {
                LOGGER.warn("Couldn't cleanup container {}", containerId, e);
            }
        }
    }
}
