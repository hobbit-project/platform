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
