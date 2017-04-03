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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;

/**
 * This class implements the {@link ContainerStateObserver} interface by
 * starting a scheduled job that retrieves a list of containers and their status
 * using the given {@link ContainerManager}. If a container has the status
 * "exited" and can be found in the internal list of monitored containers, the
 * {@link ContainerTerminationCallback#notifyTermination(String, int)} methods
 * of all registered callbacks are called.
 * 
 * Created by Timofey Ermilov on 01/09/16.
 */
public class ContainerStateObserverImpl implements ContainerStateObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerStateObserverImpl.class);

    /**
     * Internal list of monitored Docker containers.
     */
    private List<String> monitoredContainers;
    /**
     * List of termination callbacks that are called if one of the monitored
     * containers was terminated.
     */
    private List<ContainerTerminationCallback> terminationCallbacks;
    /**
     * The {@link ContainerManager} class that is used to retrieve information
     * about containers.
     */
    private ContainerManager manager;
    /**
     * The time interval in which the checking of containers is performed.
     */
    private int repeatInterval;
    /**
     * The {@link Timer} used to regularly check the state of container.
     */
    private Timer timer;

    /**
     * Constructor.
     * 
     * @param manager
     *            The {@link ContainerManager} class that is used to retrieve
     *            information about containers.
     * @param repeatInterval
     *            The time interval in which the checking of containers is
     *            performed.
     */
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
                List<Container> containers = null;
                try {
                    containers = manager.getContainers();
                } catch (Throwable e) {
                    LOGGER.error(
                            "Error while retrieving list of containers. Aborting this try to get status updates for the observed containers.",
                            e);
                    return;
                }
                for (Container c : containers) {
                    try {
                        if (monitoredContainers.contains(c.id()) && c.status().contains("Exit")) {
                            // get exit code
                            ContainerInfo containerInfo = manager.getContainerInfo(c.id());
                            int exitStatus = containerInfo.state().exitCode();
                            // notify all callbacks
                            for (ContainerTerminationCallback cb : terminationCallbacks) {
                                try {
                                    cb.notifyTermination(c.id(), exitStatus);
                                } catch (Throwable e) {
                                    LOGGER.error("Error while calling container termination callback.", e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        LOGGER.error(
                                "Error while checking status of container " + c.id()
                                        + "{}. It will be ignored during this run but will be checked again during the next run.",
                                e);
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
