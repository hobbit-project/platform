package org.hobbit.controller.docker;

import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;

import java.util.List;

/**
 * This interface is implemented by classes that can be used to manage Docker
 * containers.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface ContainerManager {

    /**
     * Start container with container type Benchmark and no parent
     *
     * @param imageName
     *            Name of the image to start
     *
     * @return container id
     */
    public String startContainer(String imageName);

    /**
     * Start container with container type Benchmark and no parent
     *
     * @param imageName
     *            name of the image to start
     * @param command
     *            command to be executed
     *
     * @return container id
     */
    public String startContainer(String imageName, String[] command);

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
    public String startContainer(String imageName, String type, String parent);

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
    public String startContainer(String imageName, String containerType, String parentId, String[] command);

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
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] command);

    /**
     * Stops the container with the given container Id.
     * 
     * @param containerId
     *            id of the container that should be stopped
     */
    public void stopContainer(String containerId);

    /**
     * Removes the already terminated container with the given container Id.
     * 
     * @param containerId
     *            id of the container that should be removed
     */
    public void removeContainer(String containerId);

    /**
     * Stops the parent container and all its children given the parent id
     *
     * @param parentId
     *            id of the parent container
     */
    public void stopParentAndChildren(String parentId);

    /**
     * Removes the parent container and all its children given the parent id
     *
     * @param parentId
     *            id of the parent container
     */
    public void removeParentAndChildren(String parentId);

    /**
     * Returns container info
     * 
     * @param containerId
     */
    public ContainerInfo getContainerInfo(String containerId);

    /**
     * Get a list of containers
     */
    public List<Container> getContainers();

    /**
     * Retrieves the container Id for the container with the given name or null
     * if no such container could be found.
     */
    public String getContainerId(String name);

    public String getContainerName(String containerId);

    public void addContainerObserver(ContainerStateObserver containerObserver);
}
