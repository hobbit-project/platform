package org.hobbit.controller.docker;

import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Info;

/**
 * This interface is implemented by classes that can be used to manage and
 * collect info about Docker Cluster.
 *
 * @author Ivan Ermilov (iermilov@informatik.uni-leipzig.de)
 *
 */
public interface ClusterManager {
    /**
     * Get number of nodes in the cluster
     *
     * @return number of nodes
     */
    public Info getClusterInfo() throws DockerException, InterruptedException;

    /**
     * Get number of nodes in the cluster
     *
     * @return number of nodes
     */
    public Integer getNumberOfNodes() throws DockerException, InterruptedException;

    /**
     * Get the health status of the cluster
     *
     * @return boolean (is cluster healthy?)
     */
    public boolean isClusterHealthy() throws DockerException, InterruptedException;
}
