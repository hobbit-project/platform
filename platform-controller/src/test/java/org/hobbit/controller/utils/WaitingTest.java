package org.hobbit.controller.utils;

import org.junit.Test;

import junit.framework.Assert;

public class WaitingTest {

    @Test(timeout = 10000)
    public void testWaiting() throws Exception {
        long checkInterval = 100;
        final long startTime = System.currentTimeMillis();
        // wait for 1 second
        final long waitingTime = 1000;

        Waiting.waitFor(() -> (System.currentTimeMillis() - startTime) >= waitingTime, checkInterval);
        // Make sure that the method was really waiting
        Assert.assertTrue((System.currentTimeMillis() - startTime) >= waitingTime);

        // Define a max waiting time and update the start time
        final long startTime2 = System.currentTimeMillis();
        long maxWaitingTime = 1000000;
        // maxWaitingTime > timeout, i.e., if the method is waiting too long, the test
        // will fail.

        Waiting.waitFor(() -> (System.currentTimeMillis() - startTime2) >= waitingTime, checkInterval, maxWaitingTime);
        // Make sure that the method was really waiting
        Assert.assertTrue((System.currentTimeMillis() - startTime2) >= waitingTime);
    }

    @Test(timeout = 10000)
    public void testMaxWaiting() throws Exception {
        long checkInterval = 100;
        long startTime = System.currentTimeMillis();
        // Define a waiting time which is too long for this test. If the maximum waiting
        // time is not working, this test will fail because of its timeout.
        long waitingTime = 100000;
        long maxWaitingTime = 1000;

        try {
            Waiting.waitFor(() -> (System.currentTimeMillis() - startTime) >= waitingTime, checkInterval,
                    maxWaitingTime);
            Assert.fail();
        } catch (InterruptedException e) {
            // Every other exception will let the test fail
        }
    }
}
