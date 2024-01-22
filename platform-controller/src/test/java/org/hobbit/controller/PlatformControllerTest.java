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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.io.IOUtils;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.data.ExperimentStatus;
import org.hobbit.controller.data.ExperimentStatus.States;
import org.hobbit.controller.docker.ContainerManagerBasedTest;
import org.hobbit.controller.docker.ContainerManagerImpl;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.utils.config.HobbitConfiguration;
import org.hobbit.utils.docker.DockerHelper;
import org.hobbit.vocab.HobbitExperiments;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.Task;

/**
 * Created by Timofey Ermilov on 02/09/16.
 */
public class PlatformControllerTest extends ContainerManagerBasedTest {

    private static final String RABBIT_HOST_NAME = DockerHelper.getHost();
    private static final String SESSION_ID = "test-session";

    private PlatformController controller;

    private void assertDockerImageEquals(String message, String expected, String got) throws Exception {
        final Matcher matcher = Pattern.compile("^(.*?)(?:@.*)?$").matcher(got);
        assertTrue("Image name matches pattern", matcher.find());
        assertEquals(message, expected, matcher.group(1));
    }

    @Before
    public void init() throws Exception {
        Map<String, String> envVariables = new HashMap<>();
        envVariables.put(Constants.RABBIT_MQ_HOST_NAME_KEY, RABBIT_HOST_NAME);
        envVariables.put(Constants.GENERATOR_ID_KEY, "0");
        envVariables.put(Constants.GENERATOR_COUNT_KEY, "1");
        envVariables.put(Constants.HOBBIT_SESSION_ID_KEY, "0");

        HobbitConfiguration configuration = new HobbitConfiguration();
        configuration.addConfiguration(new MapConfiguration(envVariables));

        controller = new PlatformController(new LocalExperimentManager(null, configuration, SESSION_ID));
        try {
            controller.init();
        } catch (Exception e) {
            throw e;
        }
    }

    public void close() throws Exception {
        IOUtils.closeQuietly(controller);
        super.close();
    }

    /**
     * This task simply tests the receiving and handling of a create container
     * command. First, a parent container is created which will be used by the test
     * commands. After that, a valid command is received and the result of the
     * container creation is checked by the test. The second command has an invalid
     * (because unknown) session id and the test checks whether the second command
     * creates an additional container.
     * 
     * @throws Exception
     */
    @Test
    public void receiveCreateContainerCommand() throws Exception {
        byte command = Commands.DOCKER_CONTAINER_START;

        // create and execute parent container
        final String parentId = manager.startContainer("busybox", Constants.CONTAINER_TYPE_SYSTEM, null,
                new String[] { "sh", "-c", "while :; do sleep 1; done" });
        final String parentName = manager.getContainerName(parentId);
        services.add(parentId);

        // create and execute test container
        final String image = "busybox:latest";
        final String type = Constants.CONTAINER_TYPE_SYSTEM;
        byte[] data = ("{\"image\": \"" + image + "\", \"type\": \"" + type + "\", \"parent\": \"" + parentName + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        controller.receiveCommand(command, data, SESSION_ID, null);

        // get running containers
        Service serviceInfo = null;
        Task taskInfo = null;
        String taskId = null;
        List<Task> taskList = dockerClient
                .listTasks(Task.Criteria.builder().label(ContainerManagerImpl.LABEL_PARENT + "=" + parentId).build());

        if (!taskList.isEmpty()) {
            taskInfo = taskList.get(0);
            serviceInfo = dockerClient.inspectService(taskInfo.serviceId());
            taskId = taskInfo.id();
        }

        // check that container exists
        assertNotNull(taskId);
        assertEquals("Amount of child containers of the test parent container", 1, taskList.size());
        assertEquals("Type of created container", Constants.CONTAINER_TYPE_SYSTEM,
                serviceInfo.spec().labels().get(ContainerManagerImpl.LABEL_TYPE));
        assertDockerImageEquals("Image of created container", image, taskInfo.spec().containerSpec().image());

        // create and execute a second test container from a different session
        // (shouldn't be created)
        controller.receiveCommand(command, data, "wrong-" + SESSION_ID, null);
        taskList = dockerClient
                .listTasks(Task.Criteria.builder().label(ContainerManagerImpl.LABEL_PARENT + "=" + parentId).build());

        Assert.assertTrue(!taskList.isEmpty());
        Assert.assertEquals("Still only one child of the parent task", taskId, taskList.get(0).id());
        Assert.assertEquals("Amount of child containers of the test parent container", 1, taskList.size());
    }

    protected static class LocalExperimentManager extends ExperimentManager {

        public LocalExperimentManager(PlatformController controller, HobbitConfiguration config, String session) {
            super(controller, config);
            experimentStatus = new ExperimentStatus(
                    new ExperimentConfiguration(session, "TestBenchmark", "", "TestSytem"),
                    HobbitExperiments.getExperimentURI(session));
            experimentStatus.setState(States.STARTED);
        }

    }
}
