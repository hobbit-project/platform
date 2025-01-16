package org.hobbit.controller.docker;

import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerStats;
import com.spotify.docker.client.messages.swarm.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Farshad Afshari on 13/01/2025
 *
 * @author Farshad afshari (farshad.afshari@uni-paderborn.de)
 */

public class ContainerManagerKubernetesImpl implements ContainerManager {

    private String experimentId = null;


    @Deprecated
    public String startContainer(String imageName) {
        return "";
    }

    @Deprecated
    public String startContainer(String imageName, String[] command) {
        return "";
    }

    @Override
    public String startContainer(String imageName, String type, String parent) {
        return startContainer(imageName, type, parent, null);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] command) {
        return startContainer(imageName, containerType, parentId, null, command);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] command) {
        return startContainer(imageName, containerType, parentId, env, null, command);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] netAliases, String[] command) {
        return startContainer(imageName, containerType, parentId, env, netAliases, command, true,
            Collections.emptyMap());
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] command, boolean pullImage) {
        return startContainer(imageName, containerType, parentId, env, null, command, true, Collections.emptyMap());
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] netAliases, String[] command, boolean pullImage, Map<String, Object> constraints) {
//todo
        return null;
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] netAliases, String[] command, String experimentId, Map<String, Object> constraints) {
        this.experimentId = experimentId;
        return startContainer(imageName, containerType, parentId, env, netAliases, command, true, constraints);
    }

    @Override
    public void stopContainer(String containerId) {

    }

    @Override
    public void removeContainer(String serviceName) {

    }

    @Override
    public void stopParentAndChildren(String parentId) {

    }

    @Override
    public void removeParentAndChildren(String parent) {

    }

    @Override
    public Long getContainerExitCode(String serviceName) throws DockerException, InterruptedException {
        return 0L;
    }

    @Override
    public Service getContainerInfo(String serviceName) throws InterruptedException, DockerException {
        return null;
    }

    @Override
    public List<Service> getContainers(Service.Criteria criteria) {
        return null;
    }

    @Override
    public String getContainerId(String name) {
        return "";
    }

    @Override
    public String getContainerName(String containerId) {
        return "";
    }

    @Override
    public void addContainerObserver(ContainerStateObserver containerObserver) {

    }

    @Override
    public void pullImage(String imageName) {

    }

    @Override
    public ContainerStats getStats(String containerId) {
        return null;
    }

    @Override
    public String getContainerType(String containerId) {
        return "";
    }
}
