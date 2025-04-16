package org.hobbit.controller.containers.kubernetes;

import io.kubernetes.client.openapi.models.V1Pod;
import org.hobbit.controller.containers.ContainerStateObserver;
import org.hobbit.controller.containers.ContainerTerminationCallback;
import org.hobbit.controller.containers.KubExtendedContainerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A concrete implementation of {@link ContainerStateObserver} that monitors Kubernetes pods and
 * notifies registered callbacks when a pod terminates.
 *  @author Farshad Afshari farshad.afshari@uni-paderborn.de
 */

public class ContainerStateObserverImpl implements ContainerStateObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerStateObserverImpl.class);

    private KubExtendedContainerManager manager;
    private final List<String> monitoredContainers;
    private final List<ContainerTerminationCallback> terminationCallbacks;
    private final Timer timer;
    private final int repeatInterval;

    public ContainerStateObserverImpl(KubExtendedContainerManager manager, int repeatInterval) {
        this.manager = manager;
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
                List<String> containerConvertedIPs;
                synchronized (monitoredContainers) {
                    containerConvertedIPs = new ArrayList<>(monitoredContainers);
                }

                for (String containerConvertedIP : containerConvertedIPs) {
                    try {
                        V1Pod pod = manager.getPod(containerConvertedIP);
                        if (pod != null && isPodTerminated(pod)) {
                            int exitCode = getPodExitCode(pod);

                            for (ContainerTerminationCallback callback : terminationCallbacks) {
                                try {
                                    callback.notifyTermination(containerConvertedIP, exitCode);
                                } catch (Exception e) {
                                    LOGGER.error("Error while calling container termination callback.", e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Couldn't get the status of container " + containerConvertedIP + ". It will be ignored during this run but will be checked again during the next run.", e);
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
    public void addObservedContainer(String containerConvertedIP) {
        synchronized (monitoredContainers) {
            if (!monitoredContainers.contains(containerConvertedIP)) {
                monitoredContainers.add(containerConvertedIP);
            }
        }
    }

    @Override
    public void removedObservedContainer(String containerConvertedIP) {
        synchronized (monitoredContainers) {
            monitoredContainers.remove(containerConvertedIP);
        }
    }

    @Override
    public List<String> getObservedContainers() {
        synchronized (monitoredContainers) {
            return new ArrayList<>(monitoredContainers);
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
