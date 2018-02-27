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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.hobbit.core.Constants;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.ServiceNotFoundException;
import com.spotify.docker.client.exceptions.TaskNotFoundException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.Image;
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
            Task task = dockerClient.inspectTask(containerId);
            ObjectMapper objectMapper = new ObjectMapper();
            System.out.println("Checking status of " + objectMapper.writeValueAsString(task).replace('\n', ' '));
            Thread.sleep(100);
            TaskStatus status = task.status();
            Assert.assertNotNull(status);
            
            // FIXME: "starting container failed: Address already in use"
            // skip test if this happens
            if (status.state().equals(TaskStatus.TASK_STATE_FAILED)) {
                Assert.assertFalse("BUG: Address already in use",
                        task.status().err().equals("starting container failed: Address already in use"));
            }

            assertEquals(message + " is running (error: " + task.status().err() + ")",
                    TaskStatus.TASK_STATE_RUNNING, task.status().state());
        } catch (TaskNotFoundException e) {
            fail(message + "is running got: swarm task not found");
        }
    }

    private void assertContainerIsNotRunning(String message, String containerId) throws Exception {
        try {
            Task taskInfo = dockerClient.inspectTask(containerId);
            @SuppressWarnings("unused")
            Service serviceInfo = dockerClient.inspectService(taskInfo.serviceId());

            fail(message
                    + " expected an TaskNotFoundException to be thrown, got a task with state="
                    + taskInfo.status().state());
        } catch (TaskNotFoundException | ServiceNotFoundException e) {
            assertNotNull(message + " expected TaskNotFoundException | ServiceNotFoundException", e);
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
        containers.add(parentId);

        String containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, parentId,
                sleepCommand);
        assertNotNull(containerId);
        containers.add(containerId);

        final Task containerInfo = dockerClient.inspectTask(containerId);
        assertNotNull("Task inspection response from docker", containerInfo);

        final Service serviceInfo = dockerClient.inspectService(containerInfo.serviceId());
        assertNotNull("Service inspection response from docker", serviceInfo);

        assertEquals(containerInfo.id(), containerId);
        assertEquals("Task state of created swarm service",
                TaskStatus.TASK_STATE_RUNNING, containerInfo.status().state());
        assertEquals("Type label of created swarm service",
                serviceInfo.spec().labels().get(ContainerManagerImpl.LABEL_TYPE),
                Constants.CONTAINER_TYPE_SYSTEM);
        assertEquals("Parent label of created swarm service",
                parentId, serviceInfo.spec().labels().get(ContainerManagerImpl.LABEL_PARENT));
        assertTrue("Command of created container spec is sleepCommand",
                Arrays.equals(sleepCommand, containerInfo.spec().containerSpec().command().toArray()));
    }

    @Test
    public void startContainerWithoutCommand() throws Exception {
        String containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null);
        assertNotNull(containerId);
        containers.add(containerId);
        // make sure it was executed with default sleepCommand
        final Task taskInfo = dockerClient.inspectTask(containerId);
        assertNotNull("Task inspection result from docker", taskInfo);
        assertNull("Command of created container spec", taskInfo.spec().containerSpec().command());
    }

    @Test
    public void removeContainer() throws Exception {
        // start new test container
        String testContainer = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull(testContainer);
        containers.add(testContainer);
        // remove it
        manager.removeContainer(testContainer);
        // check that it's actually removed
        assertContainerIsNotRunning("Removed container", testContainer);
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
        containers.add(topParent);
        String child1 = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, topParent,
                sleepCommand);
        assertNotNull(child1);
        containers.add(child1);
        String subParent = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, topParent,
                sleepCommand);
        assertNotNull(subParent);
        containers.add(subParent);
        String subchild = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, subParent,
                sleepCommand);
        assertNotNull(subchild);
        containers.add(subchild);
        String unrelatedParent = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull(unrelatedParent);
        containers.add(unrelatedParent);
        String unrelatedChild = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, unrelatedParent,
                sleepCommand);
        assertNotNull(unrelatedChild);
        containers.add(unrelatedChild);

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
        assertContainerIsNotRunning("Child 1 container", child1);
        assertContainerIsNotRunning("Sub parent container", subParent);
        assertContainerIsNotRunning("Sub child container", subchild);

        // make sure unrelated containers are running
        assertContainerIsRunning("Unrelated parent container", unrelatedParent);
        assertContainerIsRunning("Unrelated child container", unrelatedChild);

        // cleanup
        manager.removeParentAndChildren(unrelatedParent);
    }

    private Exception getContainer(String id) {
        try {
            // try to get info on removed container
            ContainerInfo containerInfo = dockerClient.inspectContainer(id);
            containerInfo.state().running();
            // we expected an exception
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    @Test
    public void getContainerInfo() throws Exception {
        // start new test container
        String testContainer = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull(testContainer);
        containers.add(testContainer);
        // get info
        Task infoFromMananger = manager.getContainerInfo(testContainer);
        Task containerInfo = dockerClient.inspectTask(testContainer);
        // stop it immediately
        manager.removeContainer(testContainer);

        // compare info
        assertEquals(infoFromMananger.id(), containerInfo.id());
        assertEquals(infoFromMananger.status().containerStatus().exitCode(), containerInfo.status().containerStatus().exitCode());
    }

    @Test
    public void getContainerIdAndName() throws Exception {
        // start new test container
        String containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull(containerId);
        containers.add(containerId);

        // compare containerId and retrieved id
        String containerName = manager.getContainerName(containerId);
        assertEquals(containerId, manager.getContainerId(containerName));
    }

    private void removeImage(String imageName) throws Exception {
        // remove image (FIXME: from all nodes)
        List<Image> images = dockerClient.listImages(DockerClient.ListImagesParam.byName(imageName));
        if (!images.isEmpty()) {
            for (Container c : dockerClient.listContainers(DockerClient.ListContainersParam.allContainers())) {
                if (c.image().equals(imageName)) {
                    dockerClient.removeContainer(c.id());
                }
            }
            for (Image image : images) {
                dockerClient.removeImage(image.id(), true, false);
            }
        }
    }

    private boolean imageExists(String name) {
        // check if image exists (FIXME: on all nodes)
        try {
            return !dockerClient.listImages(DockerClient.ListImagesParam.byName(name)).isEmpty();
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
}
