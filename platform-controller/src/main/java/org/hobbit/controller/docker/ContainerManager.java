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

import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.swarm.Task;

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
     * Label that denotes container type
     */
    public static final String LABEL_TYPE = "org.hobbit.type";
    /**
     * Label that denotes container parent
     */
    public static final String LABEL_PARENT = "org.hobbit.parent";

    /**
     * Start container with container type Benchmark and no parent
     *
     * @param imageName
     *            Name of the image to start
     *
     * @return container id
     * @deprecated because the method tries to create a container with type=null
     *             and parent="" which does not work without a predefined
     *             default type for all containers that are created in that way.
     *             Use {@link #startContainer(String, String, String)} instead.
     */
    @Deprecated
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
     * @deprecated because the method tries to create a container with type=null
     *             and parent="" which does not work without a predefined
     *             default type for all containers that are created in that way.
     *             Use {@link #startContainer(String, String, String, String[])}
     *             instead.
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
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
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
     * @param command
     *            commands that should be executed
     * @param experimentId
     *            experimentId to add to GELF tag
     *
     * @return container Id or null if an error occurred.
     */
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] command, String experimentId);

    /**
     * Stops the container with the given container Id.
     *
     * @param containerId
     *            id of the container that should be stopped
     */
    @Deprecated
    public void stopContainer(String containerId);

    /**
     * Removes the container with the given container Id.
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
    public Task getContainerInfo(String containerId) throws InterruptedException, DockerException;

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

    /**
     * Pulls the image with the given name.
     *
     * @param imageName
     *            the name of the image that should be pulled
     */
    public void pullImage(String imageName);
}
