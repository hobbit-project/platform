package org.hobbit.controller.containers.kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.hobbit.controller.containers.ContainerManager;
import org.hobbit.controller.containers.ContainerStateObserver;
import org.hobbit.controller.containers.ContainerTerminationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ContainerStateObserverImpl implements ContainerStateObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerStateObserverImpl.class);

    ContainerManager manager;
    private String nameSpace = "default";
    private final CoreV1Api coreV1Api;
    private final List<String> monitoredContainers;
    private final List<ContainerTerminationCallback> terminationCallbacks;
    private final Timer timer;
    private final int repeatInterval;

    public ContainerStateObserverImpl(ContainerManager manager, int repeatInterval,ApiClient client) {
        this.manager = manager;
        this.coreV1Api = new CoreV1Api(client);
        this.monitoredContainers = new ArrayList<>();
        this.terminationCallbacks = new ArrayList<>();
        this.timer = new Timer();
        this.repeatInterval = repeatInterval;
    }

    @Override
    public void startObserving() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<String> containerNames;
                synchronized (monitoredContainers) {
                    containerNames = new ArrayList<>(monitoredContainers);
                }

                for (String containerName : containerNames) {
                    try {
                        V1Pod pod = getPodByContainerName(containerName);
                        if (pod != null && isPodTerminated(pod)) {
                            int exitCode = getPodExitCode(pod);

                            for (ContainerTerminationCallback callback : terminationCallbacks) {
                                try {
                                    callback.notifyTermination(containerName, exitCode);
                                } catch (Exception e) {
                                    LOGGER.error("Error while calling container termination callback.", e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Couldn't get the status of container " + containerName + ". It will be ignored during this run but will be checked again during the next run.", e);
                    }
                }
            }
        }, repeatInterval, repeatInterval);
    }

    @Override
    public void stopObserving() {
        timer.cancel();
        timer.purge();
    }

    @Override
    public void addTerminationCallback(ContainerTerminationCallback callback) {
        terminationCallbacks.add(callback);
    }

    @Override
    public void removeTerminationCallback(ContainerTerminationCallback callback) {
        terminationCallbacks.remove(callback);
    }

    @Override
    public void addObservedContainer(String containerName) {
        synchronized (monitoredContainers) {
            if (!monitoredContainers.contains(containerName)) {
                monitoredContainers.add(containerName);
            }
        }
    }

    @Override
    public void removedObservedContainer(String containerName) {
        synchronized (monitoredContainers) {
            monitoredContainers.remove(containerName);
        }
    }

    @Override
    public List<String> getObservedContainers() {
        synchronized (monitoredContainers) {
            return new ArrayList<>(monitoredContainers);
        }
    }

    private V1Pod getPodByContainerName(String containerName) {
        //LOGGER.info("Attempting to find pod by container name: {} in namespace {}", containerName, nameSpace);

        try {
            V1PodList podList = coreV1Api.listNamespacedPod(nameSpace,null, null, null, "", null, null, null, null, null, false);
            //LOGGER.info("Found pods: {}", podList.getItems().size());
            // Search for pod by container name
            for (V1Pod pod : podList.getItems()) {
                if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
                    //LOGGER.info("Checking pod: {}", pod.getMetadata().getName());

                    if (pod.getStatus().getContainerStatuses().stream().anyMatch(status -> containerName.equals(status.getName()))) {
                        //LOGGER.info("Found pod: {} matching container name: {}", pod.getMetadata().getName(), containerName);
                        return pod;
                    }
                }
            }

            //LOGGER.warn("No pod found by container name: {}. Attempting to search by UID.", containerName);

            // If no pod was found by container name, search by UID
            for (V1Pod pod : podList.getItems()) {
                if (pod.getMetadata() != null && containerName.equals(pod.getMetadata().getUid())) {
                    //LOGGER.info("Found pod: {} matching UID: {}", pod.getMetadata().getName(), containerName);
                    return pod;
                }
            }

            LOGGER.warn("No pod found with container name or UID: {}", containerName);
            return null;

        } catch (ApiException e) {
            LOGGER.error("ApiException occurred while trying to list pods. Error message: {}. Stack trace: {}", e.getMessage(), e);
            // You can rethrow the exception if needed or return null as fallback
            return null;
        } catch (Exception e) {
            LOGGER.error("Unexpected error occurred while trying to list pods. Error message: {}. Stack trace: {}", e.getMessage(), e);
            return null;
        }
    }


    private boolean isPodTerminated(V1Pod pod) {
        return pod.getStatus() != null && "Succeeded".equals(pod.getStatus().getPhase()) || "Failed".equals(pod.getStatus().getPhase());
    }

    private int getPodExitCode(V1Pod pod) {
        if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
            return pod.getStatus().getContainerStatuses().stream()
                .filter(status -> status.getState() != null && status.getState().getTerminated() != null)
                .findFirst()
                .map(status -> status.getState().getTerminated().getExitCode().intValue())
                .orElse(0);
        }
        return 0;
    }
}
