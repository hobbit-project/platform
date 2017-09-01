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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Semaphore;

import org.hobbit.core.Constants;

/**
 * Created by Timofey Ermilov on 01/09/16.
 */
public class ContainerStateObserverImplTest extends ContainerManagerBasedTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerStateObserverImplTest.class);
    
    private ContainerStateObserverImpl observer;
    private Throwable throwable = null;
    private Semaphore termination = new Semaphore(0);

    @Before
    public void initObserver() {
        // poll every 500ms
        observer = new ContainerStateObserverImpl(this.manager, 500); 
        observer.startObserving();
    }

    @Test
    public void run() throws Exception {
        // create two containers
        LOGGER.info("Creating container one...");
        String containerOneId = manager.startContainer("busybox:latest", Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        LOGGER.info("Creating container two...");
        String containerTwoId = manager.startContainer("busybox:latest", Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);

        // add containers to observer
        observer.addObservedContainer(containerOneId);
        observer.addObservedContainer(containerTwoId);

        // step two
        ContainerTerminationCallbackImpl cb2 = new ContainerTerminationCallbackImpl() {
            @Override
            public void notifyTermination(String containerId, int exitCode) {
                try {
                    // check that correct values were set
                    assertEquals(containerId, containerTwoId);
                    assertEquals(0, exitCode);
    
                    // cleanup
                    LOGGER.info("Removing stopped container two...");
                    observer.removedObservedContainer(containerTwoId);
                    manager.removeContainer(containerTwoId);
                } catch (Throwable t) {
                    throwable = t;
                }
                termination.release();
            }
        };

        // step one
        ContainerTerminationCallbackImpl cb1 = new ContainerTerminationCallbackImpl() {
            @Override
            public void notifyTermination(String containerId, int exitCode) {
                try {
                    // check that correct values were set
                    assertEquals(containerOneId, containerId);
                    assertEquals(137, exitCode);
                    observer.removeTerminationCallback(this);
    
                    // cleanup
                    LOGGER.info("Removing stopped container one...");
                    observer.removedObservedContainer(containerOneId);
                    manager.removeContainer(containerOneId);
                    
                    observer.addTerminationCallback(cb2);
                    LOGGER.info("Waiting for container two to terminate...");
                } catch (Throwable t) {
                    throwable = t;
                    termination.release();
                }
            }
        };
        observer.addTerminationCallback(cb1);

        // stop container one
        LOGGER.info("Stopping container one...");
        manager.stopContainer(containerOneId);
        
        // wait for the check to end
        LOGGER.info("Waiting for the check to end...");
        termination.acquire();
        observer.stopObserving();
        if(throwable != null){
            throwable.printStackTrace();
            Assert.fail(throwable.toString());
        }
    }
}
