package org.hobbit.controller.docker;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Info;

/**
 * ClusterManager implementation
 *
 * @author Ivan Ermilov (iermilov@informatik.uni-leipzig.de)
 *
 */
public class ClusterManagerImpl implements ClusterManager {
    /**
     * Docker client instance
     */
    private DockerClient dockerClient;
    private Integer expectedNumberOfNodes = 0;

    public ClusterManagerImpl() throws DockerCertificateException {
        dockerClient = DockerUtility.getDockerClient();
        expectedNumberOfNodes = Integer.getInteger(System.getenv("SWARM_NODE_NUMBER"));
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
