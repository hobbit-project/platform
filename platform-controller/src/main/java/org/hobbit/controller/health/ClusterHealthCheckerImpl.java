package org.hobbit.controller.health;

/**
 * Simple implementation of the {@link ClusterHealthChecker} interface.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ClusterHealthCheckerImpl implements ClusterHealthChecker {

	@Override
	public boolean isClusterHealthy(String[] clusterNodes) {
		// TODO get the list of available nodes from the Docker Swarm service
		// TODO compare the lists and make sure that all given nodes are
		// available in the cluster
		return false;
	}

}
