package org.hobbit.controller.orchestration;

import org.hobbit.controller.docker.ContainerStateObserver;

import java.util.List;

/**
 * This interface is implemented by classes that can be used to manage Docker
 * containers.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface ContainerManager<ReplicationController, Metrics> {

    /**
     * Exit code of containers
     * where process was terminated with SIGKILL (number 9).
     */
    public static int DOCKER_EXITCODE_SIGKILL = 128 + 9;

    /**
     * Label that denotes container type
     */
    public static final String LABEL_TYPE = "org.hobbit.type";
    /**
     * Label that denotes container parent
     */
    public static final String LABEL_PARENT = "org.hobbit.parent";

    @Deprecated
    String startContainer(String imageName);

    /**
     * Start container with container type Benchmark and no parent
     *
     * @param imageName
     *            name of the image to start
     * @param command
     *            command to be executed
     *
     * @return container id
     * @deprecated because the method tries to create a container with type=null and
     *             parent="" which does not work without a predefined default type
     *             for all containers that are created in that way. Use
     *             {@link #startContainer(String, String, String, String[])}
     *             instead.
     */
    String startContainer(String imageName, String[] command);

    /**
     * Start container with given image, type and parent
     *
     * @param imageName
     *            name of the image to start
     * @param type
     *            container type
     * @param parent
     *            parent id
     *
     *
     * @return container id
     */
    String startContainer(String imageName, String type, String parent);

    /**
     * Starts the container with the given image name.
     *
     * @param imageName
     *            name of the image to be started
     * @param containerType
     *            type to be assigned to container
     * @param parentId
     *            id of the parent container
     * @param command
     *            commands that should be executed
     *
     * @return container Id or null if an error occurred.
     */
    String startContainer(String imageName, String containerType, String parentId, String[] command);

    /**
     * Starts the container with the given image name.
     *
     * @param imageName
     *            name of the image to be started
     * @param containerType
     *            type to be assigned to container
     * @param parentId
     *            id of the parent container
     * @param env
     *            environment variables of the schema "key=value"
     * @param command
     *            commands that should be executed
     *
     * @return container Id or null if an error occurred.
     */
    String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] command);
    /**
     * Starts the container with the given image name.
     *
     * @param imageName
     *            name of the image to be started
     * @param containerType
     *            type to be assigned to container
     * @param parentId
     *            id of the parent container
     * @param env
     *            environment variables of the schema "key=value"
     * @param netAliases
     *            network aliases for this container
     * @param command
     *            commands that should be executed
     *
     * @return container Id or null if an error occurred.
     */
    String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] netAliases, String[] command);
    /**
     * Starts the container with the given image name.
     *
     * @param imageName
     *            name of the image to be started
     * @param containerType
     *            type to be assigned to container
     * @param parentId
     *            id of the parent container
     * @param env
     *            environment variables of the schema "key=value"
     * @param command
     *            commands that should be executed
     * @param pullImage
     *            whether the image needs to be prefetched
     *
     * @return container Id or null if an error occurred.
     */
    String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] command, boolean pullImage);

    /**
     * Starts the container with the given image name.
     *
     * @param imageName
     *            name of the image to be started
     * @param containerType
     *            type to be assigned to container
     * @param parentId
     *            id of the parent container
     * @param env
     *            environment variables of the schema "key=value"
     * @param netAliases
     *            network aliases for this container
     * @param command
     *            commands that should be executed
     * @param pullImage
     *            whether the image needs to be prefetched
     *
     * @return container Id or null if an error occurred.
     */
    String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] netAliases, String[] command, boolean pullImage);

    /**
     * Starts the container with the given image name.
     *
     * @param imageName
     *            name of the image to be started
     * @param containerType
     *            type to be assigned to container
     * @param parentId
     *            id of the parent container
     * @param env
     *            environment variables of the schema "key=value"
     * @param command
     *            commands that should be executed
     * @param experimentId
     *            experimentId to add to GELF tag
     *
     * @return container Id or null if an error occurred.
     */
    String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] netAliases, String[] command, String experimentId);

    /**
     * Stops the container with the given container Id.
     *
     * @param containerId
     *            id of the container that should be stopped
     * @deprecated use {@link #removeContainer(String)} instead.
     */
    @Deprecated
     void stopContainer(String containerId);

    /**
     * Removes the container with the given container Id.
     *
     * @param containerId
     *            id of the container that should be removed
     */
    void removeContainer(String serviceName);

    /**
     * Stops the parent container and all its children given the parent id
     *
     * @param parentId
     *            id of the parent container
     * @deprecated use {@link #removeParentAndChildren(String)} instead.
     */
    @Deprecated
    void stopParentAndChildren(String parentId);

    /**
     * Removes the parent container and all its children given the parent id
     *
     * @param parent
     *            id of the parent container
     */
    void removeParentAndChildren(String parent);

    /**
     * Returns container's exit code or null if container is still running.
     *
     * @param container
     */
    Integer getContainerExitCode(String serviceName);

    /**
     * Returns container info
     *
     * @param containerId
     */
    ReplicationController getContainerInfo(String serviceName);

    /**
     * Get a list of services
     */
    List<ReplicationController> getContainers(String label, String value);

    /**
     * Get a list of services which fulfill the given filter criteria.
     *
     * @Service.Criteria criteria
     *            service criteria for filtering the list of services
     */
    @Deprecated
    String getContainerId(String name);

    /**
     * @deprecated Platform uses names as IDs.
     * Returns the name of the container with the given Id or {@code null} if such a
     * container can not be found
     *
     * @param containerId
     *            the Id of the container for which the name should be retrieved
     * @return the name of the container with the given Id or {@code null} if such a
     *         container can not be found
     */
    @Deprecated
    String getContainerName(String containerId);

    /**
     * Adds the given observer to the list of internal observers.
     *
     * @param containerObserver
     *            the observer that should be added to the internal list
     */
    void addContainerObserver(ContainerStateObserver containerObserver);

    /**
     * Pulls the image with the given name.
     *
     * @param imageName
     *            the name of the image that should be pulled
     */
    void pullImage(String imageName);

    /**
     * Returns statistics of the container with the given Id or {@code null} if the
     * container can not be found or an error occurs.
     *
     * @param containerId
     *            the Id of the container for which statistics should be requested
     * @return statistics of the container with the given Id or {@code null} if the
     *         container can not be found or an error occurs.
     */
    Metrics getStats(String containerId);
}
