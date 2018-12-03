package org.hobbit.cloud;

import com.spotify.docker.client.DockerClient;
import org.hobbit.controller.cloud.DockerClientProvider;
import org.hobbit.controller.cloud.aws.swarm.SwarmClusterManager;
import org.junit.Before;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */
public class DockerClientTest {

    DockerClient dockerClient;

    @Before
    public void init() throws Exception {
        dockerClient = DockerClientProvider.getDockerClient();
    }
}
