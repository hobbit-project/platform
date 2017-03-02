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
