package org.hobbit.controller.utils;

/**
 * A simple functional interface can be used to implement a function that
 * returns a boolean but is allowed to throw an {@link Exception}.
 * 
 * @author Denis Kuchelev
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
@FunctionalInterface
public interface ExceptionBooleanSupplier {
    /**
     * Method that can be used to implement a function that returns a boolean but is
     * allowed to throw an {@link Exception}.
     * 
     * @return a boolean value.
     * @throws Exception
     *             if the internal check causes an error that can not be handled by
     *             the method itself.
     */
    boolean getAsBoolean() throws Exception;
}
