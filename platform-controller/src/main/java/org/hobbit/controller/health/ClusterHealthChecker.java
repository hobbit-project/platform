package org.hobbit.controller.health;

/**
 * A class implementing this interface is able to check whether all given nodes
 * are part of the Docker Swarm cluster.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface ClusterHealthChecker {

	/**
	 * Checks whether all nodes are available. Returns true if the Docker Swarm
	 * service lists these nodes. If one of the nodes is missing or the list can
	 * not be acquired, false is returned.
	 * 
	 * @return
	 */
	public boolean isClusterHealthy(String clusterNodes[]);
}
