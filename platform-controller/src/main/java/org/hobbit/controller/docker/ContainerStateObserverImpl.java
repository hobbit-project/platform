package org.hobbit.controller.docker;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;

/**
 * Created by Timofey Ermilov on 01/09/16.
 */
public class ContainerStateObserverImpl implements ContainerStateObserver {

    // private static final Logger LOGGER =
    // LoggerFactory.getLogger(ContainerStateObserverImpl.class);

    private List<String> monitoredContainers;
    private List<ContainerTerminationCallback> terminationCallbacks;
    private ContainerManager manager;
    private int repeatInterval;
    private Timer timer;

    public ContainerStateObserverImpl(ContainerManager manager, int repeatInterval) {
        this.manager = manager;
        this.repeatInterval = repeatInterval;
        monitoredContainers = new ArrayList<>();
        terminationCallbacks = new ArrayList<>();
        timer = new Timer();
    }

    @Override
    public void startObserving() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<Container> containers = manager.getContainers();
                for (Container c : containers) {
                    if (c.getStatus().contains("Exit") && monitoredContainers.contains(c.getId())) {
                        // get exit code
                        InspectContainerResponse containerInfo = manager.getContainerInfo(c.getId());
                        int exitStatus = containerInfo.getState().getExitCode();
                        // notify all callbacks
                        for (ContainerTerminationCallback cb : terminationCallbacks) {
                            cb.notifyTermination(c.getId(), exitStatus);
                        }
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
    public void addObservedContainer(String containerId) {
        // check if it's already added
        if (monitoredContainers.contains(containerId)) {
            return;
        }
        // if not - add
        monitoredContainers.add(containerId);
    }

    @Override
    public void removedObservedContainer(String containerId) {
        monitoredContainers.remove(containerId);
    }
}
