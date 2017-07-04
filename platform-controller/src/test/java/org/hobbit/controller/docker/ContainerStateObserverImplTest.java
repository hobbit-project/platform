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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Timofey Ermilov on 01/09/16.
 */
public class ContainerStateObserverImplTest extends ContainerManagerBasedTest {
    private ContainerStateObserverImpl observer;

    @Before
    public void initObserver() {
        observer = new ContainerStateObserverImpl(this.manager, 500); // poll every 500ms
        observer.startObserving();
    }

    @Test
    public void run() throws Exception {
        // create two containers
        String containerOneId = manager.startContainer("busybox", sleepCommand);
        String containerTwoId = manager.startContainer("busybox", sleepCommand);

        // add containers to observer
        observer.addObservedContainer(containerOneId);
        observer.addObservedContainer(containerTwoId);

        // step two
        ContainerTerminationCallbackImpl cb2 = new ContainerTerminationCallbackImpl() {
            @Override
            public void notifyTermination(String containerId, int exitCode) {
                // check that correct values were set
                assertEquals(containerId, containerTwoId);
                assertEquals(exitCode, 0);

                // cleanup
                manager.removeContainer(containerOneId);
                manager.removeContainer(containerTwoId);
            }
        };

        // step one
        ContainerTerminationCallbackImpl cb1 = new ContainerTerminationCallbackImpl() {
            @Override
            public void notifyTermination(String containerId, int exitCode) {
                // check that correct values were set
                assertEquals(containerId, containerOneId);
                assertEquals(exitCode, 137);
                observer.removeTerminationCallback(this);
                observer.addTerminationCallback(cb2);

                manager.stopContainer(containerTwoId);
            }
        };
        observer.addTerminationCallback(cb1);

        // stop container one
        manager.stopContainer(containerOneId);
    }
}
