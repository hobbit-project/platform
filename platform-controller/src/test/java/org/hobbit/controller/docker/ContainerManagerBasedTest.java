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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.docker.client.exceptions.ServiceNotFoundException;

/**
 * Created by Timofey Ermilov on 01/09/16.
 */
public class ContainerManagerBasedTest extends DockerBasedTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerBasedTest.class);

    protected ContainerManagerImpl manager;
    protected List<String> containers = new ArrayList<String>();

    @Before
    public void initManager() throws Exception {
        manager = new ContainerManagerImpl();
    }

    @After
    public void cleanUp() {
        for (String taskId : containers) {
            try {
                String serviceId = dockerClient.inspectTask(taskId).serviceId();
                String containerId = dockerClient.inspectTask(taskId).status().containerStatus().containerId();
                try {
                    dockerClient.removeService(serviceId);
                } catch (Exception e) {
                    LOGGER.info(
                            "Cleaning up service of task {} was not successful ({}). This does not have to be a problem.",
                            taskId, e.getMessage());
                }
                try {
                    dockerClient.stopContainer(containerId, 10);
                    dockerClient.removeContainer(containerId);
                } catch (Exception e) {
                    LOGGER.info("Cleaning up container {} was not successful ({}). This does not have to be a problem.",
                            containerId, e.getMessage());
                }
                // wait for the service to disappear
                waitFor(() -> {
                    try {
                        dockerClient.inspectService(serviceId);
                        return false;
                    } catch (ServiceNotFoundException e) {
                        return true;
                    }
                }, 100);
            } catch (Exception e) {
                LOGGER.info(
                        "Cleaning up service and containers of task {} were not successful ({}). This does not have to be a problem.",
                        taskId, e.getMessage());
            }
        }
    }

    @FunctionalInterface
    private interface ExceptionBooleanSupplier {
        boolean getAsBoolean() throws Exception;
    }
    
    private static void waitFor(ExceptionBooleanSupplier checkSupplier, long interval) throws Exception {
        while (!checkSupplier.getAsBoolean()) {
            // TODO: can use some kind of inverval adjustion
            Thread.sleep(interval);
        }
    }
}
