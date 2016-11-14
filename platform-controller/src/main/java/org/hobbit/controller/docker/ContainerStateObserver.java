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
