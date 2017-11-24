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
package org.hobbit.controller;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.Task;
import org.hobbit.controller.docker.ContainerManagerImpl;
import org.hobbit.controller.docker.ContainerManagerBasedTest;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.utils.docker.DockerHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

/**
 * Created by Timofey Ermilov on 02/09/16.
 */
public class PlatformControllerTest extends ContainerManagerBasedTest {

    private static final String RABBIT_HOST_NAME = DockerHelper.getHost();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private PlatformController controller;

    @Before
    public void init() throws Exception {
        environmentVariables.set(Constants.RABBIT_MQ_HOST_NAME_KEY, RABBIT_HOST_NAME);
        environmentVariables.set(Constants.GENERATOR_ID_KEY, "0");
        environmentVariables.set(Constants.GENERATOR_COUNT_KEY, "1");
        environmentVariables.set(Constants.HOBBIT_SESSION_ID_KEY, "0");

        controller = new PlatformController();
        try {
            controller.init();
        } catch (Exception e) {
            throw e;
        }
    }

    @Test
    public void receiveCommand() throws Exception {
        byte command = Commands.DOCKER_CONTAINER_START;

        // create and execute parent container
        final String parentId = manager.startContainer(
                "busybox",
                Constants.CONTAINER_TYPE_SYSTEM,
                null,
                new String[] { "sh", "-c", "while :; do sleep 1; done" });
        final String parentName = manager.getContainerName(parentId);

        // create and execute test container
        final String image = "busybox:latest";
        final String type = Constants.CONTAINER_TYPE_SYSTEM;
        byte[] data = ("{\"image\": \"" + image + "\", \"type\": \"" + type + "\", \"parent\": \"" + parentName + "\"}").getBytes(StandardCharsets.UTF_8);
        controller.receiveCommand(command, data, "1", "");

        // get running containers
        Service serviceInfo = null;
        Task taskInfo = null;
        String containerId = null;
        final List<Task> containers = dockerClient.listTasks(Task.Criteria.builder()
                .label(ContainerManagerImpl.LABEL_PARENT + "=" + parentId)
                .build());

        if (!containers.isEmpty()) {
            taskInfo = containers.get(0);
            serviceInfo = dockerClient.inspectService(taskInfo.serviceId());
            containerId = taskInfo.id();
        }

        // cleanup
        try {
            dockerClient.stopContainer(containerId, 5);
        } catch (Exception e) {}
        try {
            dockerClient.removeContainer(containerId);
        } catch (Exception e) {}
        try {
            dockerClient.stopContainer(parentId, 5);
        } catch (Exception e) {}
        try {
            dockerClient.removeContainer(parentId);
        } catch (Exception e) {}

        // check that container exists
        assertNotNull(containerId);
        assertEquals("Amount of child containers of the test parent container", 1, containers.size());
        assertEquals("Type of created container",
                Constants.CONTAINER_TYPE_SYSTEM, serviceInfo.spec().labels().get(ContainerManagerImpl.LABEL_TYPE));
        assertEquals("Image of created container", taskInfo.spec().containerSpec().image(), image);
    }
}
