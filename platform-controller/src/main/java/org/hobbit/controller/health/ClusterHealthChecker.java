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
