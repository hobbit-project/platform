package org.hobbit.controller.orchestration;

import org.hobbit.controller.docker.ContainerStateObserver;

import java.util.List;

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
    public String startContainer(String imageName);

    String startContainer(String imageName, String[] command);

    String startContainer(String imageName, String type, String parent);

    String startContainer(String imageName, String containerType, String parentId, String[] command);

    String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] command);

    String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] netAliases, String[] command);

    String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] command, boolean pullImage);

    String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] netAliases, String[] command, boolean pullImage);

    String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] netAliases, String[] command, String experimentId);

    @Deprecated
    public void stopContainer(String containerId);

    void removeContainer(String serviceName);

    @Deprecated
    void stopParentAndChildren(String parentId);

    void removeParentAndChildren(String parent);

    Integer getContainerExitCode(String serviceName);

    ReplicationController getContainerInfo(String serviceName);

    List<ReplicationController> getContainers(String parent);

    @Deprecated
    String getContainerId(String name);

    @Deprecated
    String getContainerName(String containerId);

    void addContainerObserver(ContainerStateObserver containerObserver);

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
