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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hobbit.core.Constants;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ServiceNotFoundException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.TaskStatus;

/**
 * Created by yamalight on 31/08/16.
 */
public class ContainerManagerImplTest extends ContainerManagerBasedTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImplTest.class);

    private void assertContainerIsRunning(String message, String containerId) throws Exception {
        try {
            List<Task> tasks = dockerClient.listTasks(Task.Criteria.builder().serviceName(containerId).build());
            assertEquals("Amount of tasks of service", 1, tasks.size());
            for (Task task : tasks) {
                // FIXME: "starting container failed: Address already in use"
                // skip test if this happens
                if (task.status().state().equals(TaskStatus.TASK_STATE_FAILED)) {
                    Assert.assertFalse("BUG: Address already in use",
                            task.status().err().equals("starting container failed: Address already in use"));
                }

                assertEquals(message + " is running (error: " + task.status().err() + ")",
                        TaskStatus.TASK_STATE_RUNNING, task.status().state());
            }
        } catch (ServiceNotFoundException e) {
            fail(message + "is running got: swarm service not found");
        }
    }

    private void assertContainerIsNotRunning(String message, String containerId) throws Exception {
        try {
            Service serviceInfo = dockerClient.inspectService(containerId);

            fail(message
                    + " expected an ServiceNotFoundException to be thrown, got a service info: "
                    + serviceInfo);
        } catch (ServiceNotFoundException e) {
            assertNotNull(message + " expected ServiceNotFoundException", e);
        }
    }

    @Test
    public void created() throws Exception {
        assertNotNull(manager);
    }

    @Test
    public void startContainer() throws Exception {
        String parentId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null,
                sleepCommand);
        assertNotNull(parentId);
        services.add(parentId);

        String containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, parentId,
                sleepCommand);
        assertNotNull(containerId);
        services.add(containerId);

        final Service serviceInfo = dockerClient.inspectService(containerId);
        assertNotNull("Service inspection response from docker", serviceInfo);

        assertThat("Container ID as seen by the platform (names are used in place of IDs)", containerId, not(equalTo(serviceInfo.id())));
        assertEquals("Type label of created swarm service",
                serviceInfo.spec().labels().get(ContainerManagerImpl.LABEL_TYPE),
                Constants.CONTAINER_TYPE_SYSTEM);
        assertEquals("Parent label of created swarm service",
                parentId, serviceInfo.spec().labels().get(ContainerManagerImpl.LABEL_PARENT));

        List<Task> tasks = dockerClient.listTasks(Task.Criteria.builder().serviceName(containerId).build());
        assertEquals("Amount of tasks of created swarm service", 1, tasks.size());
        for (Task taskInfo : tasks) {
            assertEquals("Task state of created swarm service",
                    TaskStatus.TASK_STATE_RUNNING, taskInfo.status().state());
            assertTrue("Command of created container spec is sleepCommand",
                    Arrays.equals(sleepCommand, taskInfo.spec().containerSpec().command().toArray()));
        }
    }

    @Test
    public void startContainerWithoutCommand() throws Exception {
        String containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null);
        assertNotNull(containerId);
        services.add(containerId);

        // make sure it was executed with default sleepCommand
        List<Task> tasks = dockerClient.listTasks(Task.Criteria.builder().serviceName(containerId).build());
        assertEquals("Amount of tasks of created swarm service", 1, tasks.size());
        for (Task taskInfo : tasks) {
            assertNull("Command of created container spec",
                    taskInfo.spec().containerSpec().command());
        }
    }

    @Test
    public void removeContainer() throws Exception {
        // start new test container
        String testContainer = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull(testContainer);
        services.add(testContainer);
        // remove it
        manager.removeContainer(testContainer);
        // check that it's actually removed
        assertContainerIsNotRunning("Removed container", testContainer);
        services.remove(testContainer);
    }

    @Test
    public void removeParentAndChildren() throws Exception {
        // start new test containers
        // topParent:
        // - child1
        // - subParent:
        //   - subchild
        // unrelatedParent:
        // - unrelatedChild
        String topParent = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull(topParent);
        services.add(topParent);
        String child1 = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, topParent,
                sleepCommand);
        assertNotNull(child1);
        services.add(child1);
        String subParent = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, topParent,
                sleepCommand);
        assertNotNull(subParent);
        services.add(subParent);
        String subchild = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, subParent,
                sleepCommand);
        assertNotNull(subchild);
        services.add(subchild);
        String unrelatedParent = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull(unrelatedParent);
        services.add(unrelatedParent);
        String unrelatedChild = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, unrelatedParent,
                sleepCommand);
        assertNotNull(unrelatedChild);
        services.add(unrelatedChild);

        // make sure they are running
        assertContainerIsRunning("Top parent container", topParent);
        assertContainerIsRunning("Child 1 container", child1);
        assertContainerIsRunning("Sub parent container", subParent);
        assertContainerIsRunning("Sub child container", subchild);
        assertContainerIsRunning("Unrelated parent container", unrelatedParent);
        assertContainerIsRunning("Unrelated child container", unrelatedChild);

        // trigger removal
        manager.removeParentAndChildren(topParent);

        // make sure they are removed
        assertContainerIsNotRunning("Top parent container", topParent);
        services.remove(topParent);
        assertContainerIsNotRunning("Child 1 container", child1);
        services.remove(child1);
        assertContainerIsNotRunning("Sub parent container", subParent);
        services.remove(subParent);
        assertContainerIsNotRunning("Sub child container", subchild);
        services.remove(subchild);

        // make sure unrelated containers are running
        assertContainerIsRunning("Unrelated parent container", unrelatedParent);
        assertContainerIsRunning("Unrelated child container", unrelatedChild);
    }

    @Test
    public void getContainerInfo() throws Exception {
        // start new test container
        String testContainer = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull(testContainer);
        services.add(testContainer);
        // get info
        Service infoFromMananger = manager.getContainerInfo(testContainer);
        Service containerInfo = dockerClient.inspectService(testContainer);
        // stop it immediately
        manager.removeContainer(testContainer);
        services.remove(testContainer);

        // compare info
        assertEquals(infoFromMananger.id(), containerInfo.id());
    }

    @Test
    public void getContainerIdAndName() throws Exception {
        // start new test container
        String containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull(containerId);
        services.add(containerId);

        // compare containerId and retrieved id
        String containerName = manager.getContainerName(containerId);
        assertEquals(containerId, manager.getContainerId(containerName));
    }

    private void removeImage(String imageName) throws Exception {
        // remove image (FIXME: from all nodes)
        List<Image> images = dockerClient.listImages(DockerClient.ListImagesParam.filter("reference", imageName));
        LOGGER.info("Removing image: {} ({})", imageName, images.stream().map(i -> i.id()).collect(Collectors.joining(", ")));
        if (!images.isEmpty()) {
            for (Service s : dockerClient.listServices()) {
                if (s.spec().taskTemplate().containerSpec().image().equals(imageName)) {
                    dockerClient.removeService(s.id());
                }
            }
            for (Container c : dockerClient.listContainers(DockerClient.ListContainersParam.allContainers())) {
                if (c.image().equals(imageName)) {
                    dockerClient.removeContainer(c.id());
                }
            }
            for (Image image : images) {
                LOGGER.info("Removing image: {}", image.id());
                dockerClient.removeImage(image.id(), true, false);
            }
        }
    }

    private boolean imageExists(String name) {
        // check if image exists (FIXME: on all nodes)
        try {
            return !dockerClient.listImages(DockerClient.ListImagesParam.filter("reference", name)).isEmpty();
        } catch (Exception e) {
            fail("Couldn't list images with name " + name);
            return false;
        }
    }

    @Test
    public void pullPublicImage() throws Exception {
        final String testImage = "hello-world";
        // FIXME: all checks should be performed on all nodes in the swarm! Currently it only looks at local node

        removeImage(testImage);
        assertTrue("No test image should exist before pulling", !imageExists(testImage));

        manager.pullImage(testImage);
        assertTrue("Test image should exist after pulling", imageExists(testImage));
    }

    @Test
    public void pullPrivateImage() throws Exception {
        Assume.assumeNotNull(System.getenv("GITLAB_USER"),
                             System.getenv("GITLAB_EMAIL"),
                             System.getenv("GITLAB_TOKEN"));

        final String testImage = "git.project-hobbit.eu:4567/gitadmin/docker-test";
        // FIXME: all checks should be performed on all nodes in the swarm! Currently it only looks at local node

        removeImage(testImage);
        assertTrue("No test image should exist before pulling", !imageExists(testImage));

        manager.pullImage(testImage);
        assertTrue("Test image should exist after pulling", imageExists(testImage));
    }

    @Test(timeout=60000)
    public void pullUpdatedImage() throws Exception {
        /*
        To test with multinode swarm,
        set registryHost as master node's hostname or IP,
        add the following to the /etc/docker/daemon.json on the system worker node:
        {
          "insecure-registries" : ["YOUR-REGISTRY-HOST-HERE:5000"]
        }
        and restart docker on that node.
        */
        final String registryHost = "localhost";
        final String registryHostPort = "5000";
        final String registryImage = "registry:2";
        final String testImage = registryHost + ":" + registryHostPort + "/test-image-version";

        // remove image from local cache
        removeImage(testImage);
        // start local docker registry
        dockerClient.pull(registryImage);
        ContainerConfig.Builder cfgBuilder = ContainerConfig.builder();
        cfgBuilder.image(registryImage);
        cfgBuilder.exposedPorts("5000");
        HostConfig.Builder hostCfgBuilder = HostConfig.builder();
        hostCfgBuilder.portBindings(ImmutableMap.of("5000",
                new ArrayList<PortBinding>(Arrays.asList(PortBinding.of("0.0.0.0", registryHostPort)))));
        cfgBuilder.hostConfig(hostCfgBuilder.build());
        final String registryContainer = dockerClient.createContainer(cfgBuilder.build()).id();
        dockerClient.startContainer(registryContainer);
        containers.add(registryContainer);
        // build first version of image
        dockerClient.build(Paths.get("docker/test-image-version-1"), testImage + ":latest");
        // push it to the registry
        dockerClient.push(testImage + ":latest");
        removeImage(testImage);
        // start a service using the image via the manager
        String testTask = manager.startContainer(testImage, Constants.CONTAINER_TYPE_SYSTEM, null);
        services.add(testTask);
        // check if the started service uses the first version of image
        Long exitCode = null;
        while (exitCode == null) {
            Thread.sleep(500);
            exitCode = manager.getContainerExitCode(testTask);
        }
        assertEquals("Service is using first image version",
                Long.valueOf(1), exitCode);
        manager.removeContainer(testTask);
        services.remove(testTask);
        // build second version of image
        dockerClient.build(Paths.get("docker/test-image-version-2"), testImage + ":latest");
        // push it to the registry
        dockerClient.push(testImage + ":latest");
        removeImage(testImage);
        // restore (rebuild) first version of image locally
        dockerClient.build(Paths.get("docker/test-image-version-1"), testImage + ":latest");
        // start a service using the image via the manager
        testTask = manager.startContainer(testImage, Constants.CONTAINER_TYPE_SYSTEM, null);
        services.add(testTask);
        // check if the started service uses the second version of image
        exitCode = null;
        while (exitCode == null) {
            Thread.sleep(500);
            exitCode = manager.getContainerExitCode(testTask);
        }
        assertEquals("Service is using second image version",
                Long.valueOf(2), exitCode);
    }

    private Long getContainerEnvValue(String envVariable) throws DockerException, InterruptedException {
        String id = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null,
                new String[]{"sh", "-c", "exit $" + envVariable});
        assertNotNull(id);
        services.add(id);

        Long exitCode = null;
        while (exitCode == null) {
            Thread.sleep(500);

            List<Task> tasks = dockerClient.listTasks(Task.Criteria.builder().serviceName(id).build());
            for (Task taskInfo : tasks) {
                exitCode = taskInfo.status().containerStatus().exitCode();

                if (taskInfo.status().state().equals(TaskStatus.TASK_STATE_COMPLETE) && exitCode == null) {
                    // assume exit code 0
                    exitCode = 0l;
                }
            }
        }

        assertTrue("Exit code should be < 125 (special `docker run` and `chroot` exit codes)", exitCode < 125);
        return exitCode;
    }

    @Test(timeout=60000)
    public void environmentNodesInformation() throws Exception {
        Long nodes = getContainerEnvValue(Constants.HARDWARE_NUMBER_OF_NODES_KEY);
        Long systemNodes = getContainerEnvValue(Constants.HARDWARE_NUMBER_OF_SYSTEM_NODES_KEY);
        Long benchmarkNodes = getContainerEnvValue(Constants.HARDWARE_NUMBER_OF_BENCHMARK_NODES_KEY);

        assertTrue("Total nodes should be > 0 (got " + nodes + ")",
            nodes > 0);

        assertTrue("System nodes should be <= total nodes (got " + systemNodes + " <= " + nodes + ")",
            systemNodes <= nodes);

        assertTrue("Benchmark nodes should be <= total nodes (got " + benchmarkNodes + " <= " + nodes + ")",
            benchmarkNodes <= nodes);

        if (nodes > 1) {
            assertTrue("System nodes should be > 0 (got " + systemNodes + ")",
                systemNodes > 0);

            assertTrue("Benchmark nodes should be > 0 (got " + benchmarkNodes + ")",
                benchmarkNodes > 0);
        }
    }

    @Test
    public void testNetworkAliases() throws Exception {
        final String alias = "alias-1";

        // start new test container
        String testContainer = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_BENCHMARK,
                null, null, new String[]{alias}, sleepCommand);
        assertNotNull(testContainer);
        services.add(testContainer);

        String pingContainer = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_BENCHMARK,
                null, null, null, new String[]{"ping", "-c", "1", "-W", "2", alias});
        assertNotNull(pingContainer);
        services.add(pingContainer);
        Thread.sleep(10000);
        assertEquals("Result of pinging the container's network alias", Long.valueOf(0), manager.getContainerExitCode(pingContainer));

        pingContainer = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_BENCHMARK,
                null, null, null, new String[]{"ping", "-c", "1", "-W", "2", "nonexistant"});
        assertNotNull(pingContainer);
        services.add(pingContainer);
        Thread.sleep(10000);
        assertEquals("Result of pinging the nonexisting host", Long.valueOf(1), manager.getContainerExitCode(pingContainer));
    }
}
