package org.hobbit.controller.orchestration;

import org.hobbit.controller.orchestration.objects.ClusterInfo;

/**
 * This interface is implemented by classes that can be used to manage and
 * collect info about Docker Cluster.
 *
 * @author Ivan Ermilov (iermilov@informatik.uni-leipzig.de)
 *
 */
public interface ClusterManager {
    /**
     * Get cluster info
     *
     * @return org.hobbit.controller.orchestration.objects.ClusterInfo
     */
    ClusterInfo getClusterInfo();

    /**
     * Get number of nodes in the cluster
     *
     * @return number of nodes
     */
    long getNumberOfNodes();

    /**
     * Get number of nodes in the cluster
     *
     * @param label
     *            the label to filter nodes
     * @return number of nodes with the specified label
     */
    long getNumberOfNodes(String label);

    /**
     * Get the health status of the cluster
     *
     * @return boolean (is cluster healthy?)
     */
    boolean isClusterHealthy();

    /**
     * Get expected number of nodes in the cluster
     * Set externally by SWARM_NODE_NUMBER env variable
     *
     * @return expected number of nodes
     */
    long getExpectedNumberOfNodes();

    /**
     * Set task history limit for the swarm/kubernetes cluster
     * For production history limit should be set to 0
     * Then the cluster will not keep containers after the services are removed
     */
    void setTaskHistoryLimit(Integer taskHistoryLimit);

    /**
     * Get task history limit for the swarm/kubernetes cluster
     *
     * @return int (task history limit)
     */
    int getTaskHistoryLimit();
}
