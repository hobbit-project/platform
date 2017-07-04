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

import com.spotify.docker.client.messages.ContainerInfo;

import org.hobbit.core.Constants;
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
        String containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, "0",
                sleepCommand);
        assertNotNull(containerId);
        containers.add(containerId);

        ContainerInfo containerInfo = dockerClient.inspectContainer(containerId);
        assertEquals(containerInfo.id(), containerId);
        assertTrue(containerInfo.state().running());
        assertEquals(containerInfo.config().labels().get(ContainerManagerImpl.LABEL_TYPE),
                Constants.CONTAINER_TYPE_SYSTEM);
        assertEquals(containerInfo.config().labels().get(ContainerManagerImpl.LABEL_PARENT), "0");
        assertTrue(Arrays.equals(containerInfo.config().cmd().toArray(), sleepCommand));
    }

    @Test
    public void startContainerWithoutCommand() throws Exception {
        String containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, "0");
        assertNotNull(containerId);
        containers.add(containerId);
        // make sure it was executed with default sleepCommand
        ContainerInfo containerInfo = dockerClient.inspectContainer(containerId);
        String[] defaultCommand = { "sh" };
        assertTrue(Arrays.equals(containerInfo.config().cmd().toArray(), defaultCommand));
    }

    @Test
    public void stopContainer() throws Exception {
        // start new test container
        String containerId = manager.startContainer(busyboxImageName, sleepCommand);
        assertNotNull(containerId);
        containers.add(containerId);
        // check that it's actually running
        ContainerInfo containerInfo = dockerClient.inspectContainer(containerId);
        assertTrue(containerInfo.state().running());
        // stop it immediately
        manager.stopContainer(containerId);
        // check that it's actually stopped
        containerInfo = dockerClient.inspectContainer(containerId);
        assertFalse(containerInfo.state().running());
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
            ContainerInfo containerInfo = dockerClient.inspectContainer(testContainer);
            containerInfo.state().running();
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
        String topParent = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, "0", sleepCommand);
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

        // make sure they are running
        assertTrue(dockerClient.inspectContainer(topParent).state().running());
        assertTrue(dockerClient.inspectContainer(child1).state().running());
        assertTrue(dockerClient.inspectContainer(subParent).state().running());
        assertTrue(dockerClient.inspectContainer(subchild).state().running());

        // trigger stop parent function
        manager.stopParentAndChildren(topParent);

        // make sure all the containers are stopped
        assertFalse(dockerClient.inspectContainer(topParent).state().running());
        assertFalse(dockerClient.inspectContainer(child1).state().running());
        assertFalse(dockerClient.inspectContainer(subParent).state().running());
        assertFalse(dockerClient.inspectContainer(subchild).state().running());

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
        String testContainer = manager.startContainer(busyboxImageName, sleepCommand);
        assertNotNull(testContainer);
        containers.add(testContainer);
        // stop it immediately
        manager.stopContainer(testContainer);

        // compare info
        ContainerInfo infoFromMananger = manager.getContainerInfo(testContainer);
        ContainerInfo containerInfo = dockerClient.inspectContainer(testContainer);
        assertEquals(infoFromMananger.id(), containerInfo.id());
        assertEquals(infoFromMananger.state().exitCode(), containerInfo.state().exitCode());
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
