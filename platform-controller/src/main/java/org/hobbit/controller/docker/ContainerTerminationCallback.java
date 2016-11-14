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
