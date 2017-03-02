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

/**
 * These methods have to be implemented by a class that should be called if a
 * {@link ContainerStateObserver} determines the termination of a container.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface ContainerTerminationCallback {

	/**
	 * This method is called if the container with the given container Id
	 * terminated.
	 * 
	 * @param containerId
	 *            the id of the container that terminated
	 * @param exitCode
	 *            the exit code of the container
	 */
	public void notifyTermination(String containerId, int exitCode);
}
