package org.hobbit.controller.docker;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Timofey Ermilov on 01/09/16.
 */
public class ContainerTerminationCallbackImplTest {
    @Test
    public void notifyTermination() throws Exception {
        ContainerTerminationCallbackImpl c = new ContainerTerminationCallbackImpl();
        c.notifyTermination("a", 0);
        assertEquals(c.containerId, "a");
        assertEquals(c.exitCode, 0);
    }
}
