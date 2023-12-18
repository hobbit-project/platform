package org.hobbit.controller.mocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.hobbit.controller.docker.ContainerManager;
import org.hobbit.controller.docker.ContainerStateObserver;
import org.hobbit.controller.docker.ContainerTerminationCallback;

import com.spotify.docker.client.messages.ContainerStats;
import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.Service.Criteria;

public class DummyContainerManager implements ContainerManager {

    private Semaphore benchmarkControllerTerminated;
    private ContainerTerminationCallback terminationCallback;

    public DummyContainerManager(Semaphore benchmarkControllerTerminated,
            ContainerTerminationCallback terminationCallback) {
        this.benchmarkControllerTerminated = benchmarkControllerTerminated;
        this.terminationCallback = terminationCallback;
    }

    private String containerName(String imageName) {
        return imageName.replaceFirst(":.*$", "");
    }

    @Override
    public String startContainer(String imageName) {
        return containerName(imageName);
    }

    @Override
    public String startContainer(String imageName, String[] command) {
        return containerName(imageName);
    }

    @Override
    public String startContainer(String imageName, String type, String parent) {
        return containerName(imageName);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] command) {
        return containerName(imageName);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
            String[] command) {
        return containerName(imageName);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
            String[] command, boolean pullImage) {
        return containerName(imageName);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
                                 String[] netAliases, String[] command) {
        return containerName(imageName);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
            String[] netAliases, String[] command, boolean pullImage, Map<String, Object> constraints) {
        return containerName(imageName);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
            String[] netAliases, String[] command, String experimentId, Map<String, Object> constraints) {
        return containerName(imageName);
    }

    @Override
    public void stopContainer(String serviceName) {
        // Check whether the benchmark controller has been terminated
        if (serviceName.equals(DummyImageManager.BENCHMARK_NAME)) {
            // Release the mutex for the main method
            benchmarkControllerTerminated.release();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                terminationCallback.notifyTermination(serviceName, 137);
            }
        }).start();
    }

    @Override
    public void removeContainer(String serviceName) {
    }

    @Override
    public void stopParentAndChildren(String parent) {
        stopContainer(parent);
    }

    @Override
    public void removeParentAndChildren(String parent) {
        stopContainer(parent);
    }

    @Override
    public Service getContainerInfo(String serviceName) {
        return null;
    }

    @Override
    public List<Service> getContainers(Criteria criteria) {
        return new ArrayList<>(0);
    }

    @Override
    public Long getContainerExitCode(String serviceName) {
        return null;
    }

    @Override
    public String getContainerId(String name) {
        return name;
    }

    @Override
    public String getContainerName(String containerId) {
        return containerId;
    }

    @Override
    public void addContainerObserver(ContainerStateObserver containerObserver) {
    }

    @Override
    public void pullImage(String imageName) {
        System.out.print("Pulling Image (fake) ");
        System.out.print(imageName);
        System.out.println("...");
    }

    @Override
    public ContainerStats getStats(String containerId) {
        return null;
    }

    @Override
    public String getContainerType(String containerId) {
        return null;
    }

}
