package org.hobbit.controller.docker;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Info;
import org.hobbit.controller.ExperimentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClusterManager implementation
 *
 * @author Ivan Ermilov (iermilov@informatik.uni-leipzig.de)
 *
 */
public class ClusterManagerImpl implements ClusterManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);

    /**
     * Docker client instance
     */
    private DockerClient dockerClient;
    private Integer expectedNumberOfNodes = 0;

    public ClusterManagerImpl() throws DockerCertificateException {
        dockerClient = DockerUtility.getDockerClient();
        expectedNumberOfNodes = Integer.parseInt(System.getenv("SWARM_NODE_NUMBER"));
        if(expectedNumberOfNodes == null) {
            expectedNumberOfNodes = 1;
        }
    }

    public Info getClusterInfo() throws DockerException, InterruptedException {
        return dockerClient.info();
    }

    public Integer getNumberOfNodes() throws DockerException, InterruptedException {
        final Info info = getClusterInfo();
        return info.swarm().nodes();
    }

    public boolean isClusterHealthy() throws DockerException, InterruptedException {
        Integer numberOfNodes = getNumberOfNodes();
        if(numberOfNodes >= expectedNumberOfNodes) {
            return true;
        }
        return false;
    }

    public Integer getExpectedNumberOfNodes() {
        return expectedNumberOfNodes;
    }
}
