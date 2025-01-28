package org.hobbit.controller.containers.kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
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

    private final ApiClient apiClient;
    private final CoreV1Api coreV1Api;
    private final List<String> monitoredContainers;
    private final List<ContainerTerminationCallback> terminationCallbacks;
    private final Timer timer;
    private final int repeatInterval;

    public ContainerStateObserverImpl(ApiClient apiClient, int repeatInterval) {
        this.apiClient = apiClient;
        this.coreV1Api = new CoreV1Api(apiClient);
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
                    } catch (ApiException e) {
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

    private V1Pod getPodByContainerName(String containerName) throws ApiException {
        V1PodList podList = coreV1Api.listPodForAllNamespaces(null, null, null, "", null, null, null, null, null, false);
        for (V1Pod pod : podList.getItems()) {
            if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
                if (pod.getStatus().getContainerStatuses().stream().anyMatch(status -> containerName.equals(status.getName()))) {
                    return pod;
                }
            }
        }
        return null;
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
