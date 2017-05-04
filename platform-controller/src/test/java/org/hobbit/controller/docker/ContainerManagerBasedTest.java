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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hobbit.controller.DockerBasedTest;
import org.junit.After;
import org.junit.Before;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.AttachedNetwork;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;

/**
 * Created by Timofey Ermilov on 01/09/16.
 */
public class ContainerManagerBasedTest extends DockerBasedTest {
    protected ContainerManagerImpl manager;
    protected List<String> containers = new ArrayList<String>();

    @Before
    public void initManager() throws Exception {
        manager = new ContainerManagerImpl();
    }

    @After
    public void cleanUp() {
        for (String containerId : containers) {
            try {
                dockerClient.stopContainer(containerId, 5);
            } catch (Exception e) {
            }
            try {
                dockerClient.removeContainer(containerId);
            } catch (Exception e) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DockerClient dockerClient = DefaultDockerClient.fromEnv().build();

        String containerName = "riak-test4";
        ContainerConfig.Builder cfgBuilder = ContainerConfig.builder();
        cfgBuilder.image("basho/riak-kv");
        cfgBuilder.hostname(containerName);

        cfgBuilder.env(Arrays.asList("HOBBIT_CONTAINER_NAME=riak-kv-eb1cb01e6b1348bc9dff895d29f7fa29",
                "CLUSTER_NAME=riakkv", "HOBBIT_RABBIT_HOST=rabbit", "HOBBIT_SESSION_ID=1481122628848",
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "OS_FAMILY=ubuntu",
                "OS_VERSION=14.04", "OS_FLAVOR=trusty", "DEBIAN_FRONTEND=noninteractive",
                "DEBCONF_NONINTERACTIVE_SEEN=true", "RIAK_VERSION=2.1.4", "RIAK_FLAVOR=KV", "RIAK_HOME=/usr/lib/riak"));

        // trigger creation
        ContainerConfig cfg = cfgBuilder.build();
        try {
            ContainerCreation resp = dockerClient.createContainer(cfg, containerName);
            String containerId = resp.id();
            // disconnect the container from every network it might be connected
            // to
            ContainerInfo info = dockerClient.inspectContainer(containerId);
            Map<String, AttachedNetwork> networks = info.networkSettings().networks();
            for (String networkName : networks.keySet()) {
                dockerClient.disconnectFromNetwork(containerId, networkName);
            }
            // connect to hobbit network
            dockerClient.connectToNetwork(resp.id(), "hobbit");
            // return new container id
            dockerClient.startContainer(containerId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        dockerClient.close();
    }
}
