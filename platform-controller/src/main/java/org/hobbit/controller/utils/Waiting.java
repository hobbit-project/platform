package org.hobbit.controller.utils;

/**
 * A simple class easing the waiting for a given check function to return
 * {@code true}.
 * 
 * @author Denis Kuchelev
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class Waiting {

    /**
     * Waits until the given function returns {@code true} while executing
     * repeatedly after the given amount of time.
     * 
     * @param checkSupplier
     *            the function for which this method is waiting for to return
     *            {@code true}.
     * @param interval
     *            the time interval in which the given function will be executed,
     *            again (in ms).
     * @throws Exception
     *             every exception that might be thrown by the given function.
     */
    public static void waitFor(ExceptionBooleanSupplier checkSupplier, long interval) throws Exception {
        waitFor(checkSupplier, interval, Long.MAX_VALUE);
    }

    /**
     * Waits until the given function returns {@code true} while executing
     * repeatedly after the given amount of time or terminates with throwing an
     * {@link InterruptedException} when the given maximum waiting time has been
     * reached.
     * 
     * @param checkSupplier
     *            the function for which this method is waiting for to return
     *            {@code true}.
     * @param interval
     *            the time interval in which the given function will be executed,
     *            again (in ms).
     * @throws InterruptedException
     *             if the given maximum waiting time has been reached.
     * @throws Exception
     *             every exception that might be thrown by the given function.
     */
    public static void waitFor(ExceptionBooleanSupplier checkSupplier, long interval, long maxWaitingTime)
            throws Exception {
        long startTime = System.currentTimeMillis();
        while (!checkSupplier.getAsBoolean()) {
            if ((System.currentTimeMillis() - startTime) > maxWaitingTime) {
                throw new InterruptedException("Interrupting waiting after reaching the maximum time to wait.");
            }
            Thread.sleep(interval);
        }
    }
}
