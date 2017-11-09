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

import static org.junit.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import org.hobbit.controller.docker.ContainerManagerImpl;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

/**
 * Created by Timofey Ermilov on 02/09/16.
 */
public class PlatformControllerTest extends DockerBasedTest {

    private static final String RABBIT_HOST_NAME = "192.168.99.100";

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
        Map<String,String> labels = new HashMap<String, String>();
        labels.put(ContainerManagerImpl.LABEL_TYPE, Constants.CONTAINER_TYPE_SYSTEM);
        final String parentId = dockerClient.createContainer(ContainerConfig.builder()
                .image("busybox")
                .cmd("sh", "-c", "while :; do sleep 1; done")
                .labels(labels)
                .build()).id();
        dockerClient.startContainer(parentId);
        final String parentName = dockerClient.inspectContainer(parentId).name();

        // create and execute test container
        final String image = "busybox:latest";
        final String type = Constants.CONTAINER_TYPE_SYSTEM;
        byte[] data = ("{\"image\": \"" + image + "\", \"type\": \"" + type + "\", \"parent\": \"" + parentName + "\"}").getBytes(StandardCharsets.UTF_8);
        controller.receiveCommand(command, data, "1", "");

        // get running containers
        String containerId = null;
        List<Container> containers = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        for(Container c : containers) {
            String gotImage = c.image();
            String gotType = c.labels().get(ContainerManagerImpl.LABEL_TYPE);
            String gotParent = c.labels().get(ContainerManagerImpl.LABEL_PARENT);
            if (gotImage != null && gotImage.equals(image)
                    && gotType != null && gotType.equals(type)
                    && gotParent != null && gotParent.equals(parentId)) {
                containerId = c.id();
                break;
            }
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
    }
}
