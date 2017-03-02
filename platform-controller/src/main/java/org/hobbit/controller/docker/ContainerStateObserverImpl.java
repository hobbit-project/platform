package org.hobbit.controller.docker;

import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
                    if (c.status().contains("Exit") && monitoredContainers.contains(c.id())) {
                        // get exit code
                        ContainerInfo containerInfo = manager.getContainerInfo(c.id());
                        int exitStatus = containerInfo.state().exitCode();
                        // notify all callbacks
                        for (ContainerTerminationCallback cb : terminationCallbacks) {
                            cb.notifyTermination(c.id(), exitStatus);
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
