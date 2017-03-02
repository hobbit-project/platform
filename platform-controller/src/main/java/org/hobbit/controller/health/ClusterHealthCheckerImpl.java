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
