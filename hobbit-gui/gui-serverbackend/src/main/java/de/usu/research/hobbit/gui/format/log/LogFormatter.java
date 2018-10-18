package de.usu.research.hobbit.gui.format.log;

import org.json.JSONArray;

/**
 * A simple interface which takes a {@link JSONArray} containing logs and
 * transforms them into a String.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public interface LogFormatter {

    /**
     * Transforms the given log lines into a String in the format defined by this
     * formatter.
     * 
     * @param logs
     *            {@link JSONArray} containing log messages
     * @return a String following the format of this formatter
     */
    public String format(JSONArray logs);
}
