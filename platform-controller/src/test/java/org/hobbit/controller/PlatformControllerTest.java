package org.hobbit.controller;

import static org.junit.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hobbit.controller.docker.ContainerManagerImpl;
import org.hobbit.core.Commands;
import org.junit.Before;
import org.junit.Test;

import com.github.dockerjava.api.model.Container;

/**
 * Created by Timofey Ermilov on 02/09/16.
 */
public class PlatformControllerTest extends DockerBasedTest {
    private PlatformController controller;

    @Before
    public void init() throws Exception {
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
        byte[] data = "{\"image\": \"busybox\", \"type\": \"test\", \"parent\": \"test\"}".getBytes(StandardCharsets.UTF_8);

        // execute
        controller.receiveCommand(command, data, "1", "");

        // get running containers
        String containerId = null;
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for(Container c : containers) {
            String image = c.getImage();
            String type = c.getLabels().get(ContainerManagerImpl.LABEL_TYPE);
            String parent = c.getLabels().get(ContainerManagerImpl.LABEL_PARENT);
            if (image.contains("busybox") && type.equals("test") && parent.equals("test")) {
                containerId = c.getId();
            }
        }

        // check that container exists
        assertNotNull(containerId);

        // cleanup
        try {
            dockerClient.stopContainerCmd(containerId).exec();
        } catch (Exception e) {}
        try {
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (Exception e) {}
    }
}
