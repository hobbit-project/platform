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
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface ContainerStateObserver {
    /**
     * Start observing the state of containers
     */
    public void startObserving();

    /**
     * Stop observing the state of containers
     */
    public void stopObserving();

    /**
     * Adds the callback that will be notified using
     * {@link ContainerTerminationCallback#notifyTermination(String, int)}
     *
     * @param callback
     *            the class that should be called if a container terminates
     */
    public void addTerminationCallback(ContainerTerminationCallback callback);

    /**
     * Removes the callback that will be notified using
     * {@link ContainerTerminationCallback#notifyTermination(String, int)}
     *
     * @param callback
     *            the class that should be called if a container terminates
     */
    public void removeTerminationCallback(ContainerTerminationCallback callback);

    /**
     * Adds the container with the given container Id to the list of observed
     * containers.
     *
     * @param containerId
     *            the Id of the container that should be observed
     */
    public void addObservedContainer(String containerId);

    /**
     * Removes the container with the given container Id from the list of
     * observed containers.
     *
     * @param containerId
     *            the Id of the container that should be removed
     */
    public void removedObservedContainer(String containerId);

}
