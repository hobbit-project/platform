package org.hobbit.controller;

import static org.junit.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Container;
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
        byte[] data = "{\"image\": \"busybox\", \"type\": \"test\", \"parent\": \"test\"}".getBytes(StandardCharsets.UTF_8);

        // execute
        controller.receiveCommand(command, data, "1", "");

        // get running containers
        String containerId = null;
        List<Container> containers = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        for(Container c : containers) {
            String image = c.image();
            String type = c.labels().get(ContainerManagerImpl.LABEL_TYPE);
            String parent = c.labels().get(ContainerManagerImpl.LABEL_PARENT);
            if (image.contains("busybox") && type.equals("test") && parent.equals("test")) {
                containerId = c.id();
            }
        }

        // check that container exists
        assertNotNull(containerId);

        // cleanup
        try {
            dockerClient.stopContainer(containerId, 5);
        } catch (Exception e) {}
        try {
            dockerClient.removeContainer(containerId);
        } catch (Exception e) {}
    }
}
