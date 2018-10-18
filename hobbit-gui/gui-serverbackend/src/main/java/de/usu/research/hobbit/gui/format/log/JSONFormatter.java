package de.usu.research.hobbit.gui.format.log;

import org.json.JSONArray;

/**
 * Simple formatter that just transforms the given JSON array into a String representation of the JSON.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class JSONFormatter implements LogFormatter {

    @Override
    public String format(JSONArray logs) {
        return logs.toString();
    }
}
