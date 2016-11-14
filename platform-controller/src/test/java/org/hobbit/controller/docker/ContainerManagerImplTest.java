package org.hobbit.controller.docker;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by yamalight on 31/08/16.
 */
public class ContainerManagerImplTest extends ContainerManagerBasedTest {
    @Test
    public void created() throws Exception {
        assertNotNull(manager);
    }

    @Test
    public void startContainer() throws Exception {
        String containerId = manager.startContainer(busyboxImageName, ContainerManagerImpl.TYPE_SYSTEM, "0",
                sleepCommand);
        assertNotNull(containerId);
        containers.add(containerId);

        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
        assertEquals(containerInfo.getId(), containerId);
        assertTrue(containerInfo.getState().getRunning());
        assertEquals(containerInfo.getConfig().getLabels().get(ContainerManagerImpl.LABEL_TYPE),
                ContainerManagerImpl.TYPE_SYSTEM);
        assertEquals(containerInfo.getConfig().getLabels().get(ContainerManagerImpl.LABEL_PARENT), "0");
        assertTrue(Arrays.equals(containerInfo.getConfig().getCmd(), sleepCommand));
    }

    @Test
    public void startContainerWithoutCommand() throws Exception {
        String containerId = manager.startContainer(busyboxImageName, ContainerManagerImpl.TYPE_SYSTEM, "0");
        assertNotNull(containerId);
        containers.add(containerId);
        // make sure it was executed with default sleepCommand
        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
        String[] defaultCommand = { "sh" };
        assertTrue(Arrays.equals(containerInfo.getConfig().getCmd(), defaultCommand));
    }

    @Test
    public void stopContainer() throws Exception {
        // start new test container
        String containerId = manager.startContainer(busyboxImageName, sleepCommand);
        assertNotNull(containerId);
        containers.add(containerId);
        // check that it's actually running
        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
        assertTrue(containerInfo.getState().getRunning());
        // stop it immediately
        manager.stopContainer(containerId);
        // check that it's actually stopped
        containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
        assertFalse(containerInfo.getState().getRunning());
    }

    @Test
    public void removeContainer() throws Exception {
        // start new test container
        String testContainer = manager.startContainer(busyboxImageName, sleepCommand);
        assertNotNull(testContainer);
        containers.add(testContainer);
        // stop it immediately
        manager.stopContainer(testContainer);
        // remove it
        manager.removeContainer(testContainer);
        try {
            // try to get info on removed container
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(testContainer).exec();
            containerInfo.getState().getRunning();
            // we expected an exception
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void stopAndRemoveParentAndChildren() throws Exception {
        // start new test containers
        // topParent:
        // - child1
        // - subParent:
        // - subchild
        String topParent = manager.startContainer(busyboxImageName, ContainerManagerImpl.TYPE_SYSTEM, "0",
                sleepCommand);
        assertNotNull(topParent);
        containers.add(topParent);
        String child1 = manager.startContainer(busyboxImageName, ContainerManagerImpl.TYPE_SYSTEM, topParent,
                sleepCommand);
        assertNotNull(child1);
        containers.add(child1);
        String subParent = manager.startContainer(busyboxImageName, ContainerManagerImpl.TYPE_SYSTEM, topParent,
                sleepCommand);
        assertNotNull(subParent);
        containers.add(subParent);
        String subchild = manager.startContainer(busyboxImageName, ContainerManagerImpl.TYPE_SYSTEM, subParent,
                sleepCommand);
        assertNotNull(subchild);
        containers.add(subchild);

        // make sure they are running
        assertTrue(dockerClient.inspectContainerCmd(topParent).exec().getState().getRunning());
        assertTrue(dockerClient.inspectContainerCmd(child1).exec().getState().getRunning());
        assertTrue(dockerClient.inspectContainerCmd(subParent).exec().getState().getRunning());
        assertTrue(dockerClient.inspectContainerCmd(subchild).exec().getState().getRunning());

        // trigger stop parent function
        manager.stopParentAndChildren(topParent);

        // make sure all the containers are stopped
        assertFalse(dockerClient.inspectContainerCmd(topParent).exec().getState().getRunning());
        assertFalse(dockerClient.inspectContainerCmd(child1).exec().getState().getRunning());
        assertFalse(dockerClient.inspectContainerCmd(subParent).exec().getState().getRunning());
        assertFalse(dockerClient.inspectContainerCmd(subchild).exec().getState().getRunning());

        // trigger removal
        manager.removeParentAndChildren(topParent);

        // make sure they are removed
        assertNotNull(getContainer(topParent));
        assertNotNull(getContainer(child1));
        assertNotNull(getContainer(subParent));
        assertNotNull(getContainer(subchild));
    }

    private Exception getContainer(String id) {
        try {
            // try to get info on removed container
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(id).exec();
            containerInfo.getState().getRunning();
            // we expected an exception
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    @Test
    public void getContainerInfo() throws Exception {
        // start new test container
        String testContainer = manager.startContainer(busyboxImageName, sleepCommand);
        assertNotNull(testContainer);
        containers.add(testContainer);
        // stop it immediately
        manager.stopContainer(testContainer);

        // compare info
        InspectContainerResponse infoFromMananger = manager.getContainerInfo(testContainer);
        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(testContainer).exec();
        assertEquals(infoFromMananger.getId(), containerInfo.getId());
        assertEquals(infoFromMananger.getState().getExitCode(), containerInfo.getState().getExitCode());
    }

    @Test
    public void getContainerIdAndName() throws Exception {
        // start new test container
        String containerId = manager.startContainer(busyboxImageName, sleepCommand);
        assertNotNull(containerId);
        containers.add(containerId);

        // compare containerId and retrieved id
        String containerName = manager.getContainerName(containerId);
        assertEquals(containerId, manager.getContainerId(containerName));
    }
}
