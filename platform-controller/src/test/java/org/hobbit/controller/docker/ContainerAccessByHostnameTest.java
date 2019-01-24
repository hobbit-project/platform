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

import com.spotify.docker.client.messages.ContainerConfig;
import org.hobbit.core.Constants;
import org.junit.Test;

public class ContainerAccessByHostnameTest extends ContainerManagerBasedTest {

    @Test
    public void test() throws Exception {
        String serverContainer = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull("ID of server container is not null", serverContainer);
        services.add(serverContainer);

        ContainerConfig.Builder cfgBuilder = ContainerConfig.builder();
        cfgBuilder.image(busyboxImageName);
        cfgBuilder.cmd("ping", "-q", "-c", "1", serverContainer);
        String clientContainer = dockerClient.createContainer(cfgBuilder.build()).id();
        assertNotNull("ID of client container is not null", clientContainer);
        containers.add(clientContainer);

        dockerClient.connectToNetwork(clientContainer, ContainerManagerImpl.HOBBIT_DOCKER_NETWORK);
        dockerClient.startContainer(clientContainer);
        int clientStatusCode = dockerClient.waitContainer(clientContainer).statusCode();
        assertEquals("Ping exit code", 0, clientStatusCode);
    }

}
