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
package org.hobbit;

import static org.hobbit.controller.ExperimentManager.MAX_EXECUTION_TIME_KEY;
import static org.hobbit.controller.PlatformController.*;
import static org.hobbit.controller.cloud.ClusterManagerProvider.CLOUD_VPC_CLUSTER_NAME_KEY;
import static org.hobbit.controller.docker.CloudClusterManager.CLOUD_EXPIRE_TIMEOUT_MIN_KEY;
import static org.hobbit.controller.docker.CloudClusterManager.CLOUD_SSH_KEY_FILE_PATH_KEY;
import static org.hobbit.controller.docker.CloudClusterManager.CLOUD_SSH_KEY_NAME_KEY;
import static org.hobbit.controller.docker.ContainerManagerImpl.ENABLE_VOLUMES_FOR_SYSTEM_CONTAINERS_KEY;
import static org.hobbit.controller.docker.FileBasedImageManager.FILE_BASED_IMAGE_MANAGER_FOLDER_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.hobbit.controller.PlatformController;
import org.hobbit.controller.docker.ContainerManagerBasedTest;
import org.hobbit.controller.docker.ContainerManagerImpl;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.utils.docker.DockerHelper;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.Task;

/**
 * Created by Timofey Ermilov on 02/09/16.
 */
public class PlatformControllerTest extends ContainerManagerBasedTest {

    private static final String RABBIT_HOST_NAME = DockerHelper.getHost();

    @Rule
    public static final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private PlatformController controller;

    private void assertDockerImageEquals(String message, String expected, String got) throws Exception {
        final Matcher matcher = Pattern.compile("^(.*?)(?:@.*)?$").matcher(got);
        assertTrue("Image name matches pattern", matcher.find());
        assertEquals(message, expected, matcher.group(1));
    }

    @BeforeClass
    public static void setEnvVars(){

        // Cloud extension parameters
        environmentVariables.set(USE_CLOUD_KEY, "true");
        environmentVariables.set(CLOUD_VPC_CLUSTER_NAME_KEY, "hobbit");
        environmentVariables.set(CLOUD_EXPIRE_TIMEOUT_MIN_KEY, "-1");
        environmentVariables.set(CLOUD_SSH_KEY_NAME_KEY, "hobbit_2");
        environmentVariables.set(CLOUD_SSH_KEY_FILE_PATH_KEY, "ssh/hobbit_2.pem");


        //environmentVariables.set("DOCKER_HOST", "tcp://localhost:2376"); - might be needed for testing

        // Enabling file-based image manager for local platforms
        environmentVariables.set(FILE_BASED_IMAGE_MANAGER_KEY, "true");
        environmentVariables.set(FILE_BASED_IMAGE_MANAGER_FOLDER_KEY, "/mnt/share/platform-controller/metadata");
        
        // Enabling file-based image manager for local platforms
        environmentVariables.set(ENABLE_VOLUMES_FOR_SYSTEM_CONTAINERS_KEY, "1");
        environmentVariables.set(ALLOW_ASYNC_CONTAINER_COMMANDS_KEY, "1");

        // Enabling containet logs output to console (does not require ELK)
        environmentVariables.set(SERVICE_LOGS_READER_KEY, "1");
        environmentVariables.set(MAX_EXECUTION_TIME_KEY, "3600000");

        environmentVariables.set("HOBBIT_RABBIT_HOST", "rabbit");
        environmentVariables.set("DEPLOY_ENV", "testing");
        //environmentVariables.set("LOGGING_GELF_ADDRESS", "udp://localhost:12201");

        environmentVariables.set("GITLAB_USER", System.getenv("GITLAB_USER"));
        environmentVariables.set("GITLAB_EMAIL", System.getenv("GITLAB_EMAIL"));
        environmentVariables.set("GITLAB_TOKEN", System.getenv("GITLAB_TOKEN"));

        environmentVariables.set("HOBBIT_REDIS_HOST", "redis");
        environmentVariables.set("CONTAINER_PARENT_CHECK", "0");

        environmentVariables.set(Constants.GENERATOR_ID_KEY, "0");
        environmentVariables.set(Constants.GENERATOR_COUNT_KEY, "1");
        environmentVariables.set(Constants.HOBBIT_SESSION_ID_KEY, "0");
    }

    @Before
    public void init() throws Exception {


        controller = new PlatformController();
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

    //#mvn -Dtest=PlatformControllerTest#checkRuntimeWork test
    //java -cp platform-controller.jar org.hobbit.core.run.ComponentStarter org.hobbit.controller.PlatformController
    @Test
    public void checkRuntimeWork() throws Exception{
        controller.run();
    }

    @Ignore
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
        tasks.add(parentId);

        // create and execute test container
        final String image = "busybox:latest";
        final String type = Constants.CONTAINER_TYPE_SYSTEM;
        byte[] data = ("{\"image\": \"" + image + "\", \"type\": \"" + type + "\", \"parent\": \"" + parentName + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        controller.receiveCommand(command, data, "1", "");

        // get running containers
        Service serviceInfo = null;
        Task taskInfo = null;
        String taskId = null;
        String containerId = null;
        final List<Task> taskList = dockerClient.listTasks(Task.Criteria.builder()
                .label(ContainerManagerImpl.LABEL_PARENT + "=" + parentId)
                .build());

        if (!taskList.isEmpty()) {
            taskInfo = taskList.get(0);
            serviceInfo = dockerClient.inspectService(taskInfo.serviceId());
            taskId = taskInfo.id();
        }

        // check that container exists
        assertNotNull(taskId);
        assertEquals("Amount of child containers of the test parent container", 1, taskList.size());
        assertEquals("Type of created container",
                Constants.CONTAINER_TYPE_SYSTEM, serviceInfo.spec().labels().get(ContainerManagerImpl.LABEL_TYPE));
        assertDockerImageEquals("Image of created container", image, taskInfo.spec().containerSpec().image());
    }
}
