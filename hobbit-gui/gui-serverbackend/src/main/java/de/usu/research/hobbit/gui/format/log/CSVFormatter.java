package de.usu.research.hobbit.gui.format.log;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Formatter for printing logs. Based on code from
 * {@link https://www.mkyong.com/java/how-to-export-data-to-csv-file-java/}.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class CSVFormatter implements LogFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVFormatter.class);

    private static final char DEFAULT_SEPARATOR = ',';
    private static final char DEFAULT_QUOTE = '"';

    protected String fields[];
    protected char separator;
    protected char quote;

    public CSVFormatter(String[] fields) {
        this(fields, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
    }

    public CSVFormatter(String[] fields, char separator) {
        this(fields, separator, DEFAULT_QUOTE);
    }

    public CSVFormatter(String[] fields, char separator, char quote) {
        this.fields = fields;
        this.separator = separator;
        this.quote = quote;
    }

    @Override
    public String format(JSONArray logs) {
        String result = null;
        try (StringWriter writer = new StringWriter()) {
            String values[] = new String[fields.length];
            writeLine(writer, fields);
            JSONObject obj;
            for (int i = 0; i < logs.length(); i++) {
                obj = (JSONObject) logs.get(i);
                if (!obj.isNull("_source")) {
                    obj = (JSONObject) obj.get("_source");
                    for (int j = 0; j < fields.length; j++) {
                        if (obj.isNull(fields[j])) {
                            values[j] = "";
                        } else {
                            values[j] = obj.getString(fields[j]);
                        }
                    }
                }
                writeLine(writer, values);
            }
            result = writer.toString();
        } catch (JSONException e) {
            LOGGER.error("Got an exception while parsing the logs. Returning null.", e);
        } catch (IOException e) {
            LOGGER.error("Got an exception while formatting logs. Returning null.", e);
        }
        return result;
    }

    // https://tools.ietf.org/html/rfc4180
    protected static void followCVSformat(StringBuilder builder, String value) throws IOException {
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '"') {
                builder.append('"');
            }
            builder.append(chars[i]);
        }
        // String result = value;
        // if (result.contains("\"")) {
        // result = result.replace("\"", "\"\"");
        // }
        // return result;
    }

    protected void writeLine(Writer w, String[] values) throws IOException {

        boolean first = true;

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(separator);
            }
            if (quote == ' ') {
                sb.append(value);
                // followCVSformat(sb, value);
            } else {
                sb.append(quote);
                followCVSformat(sb, value);
                sb.append(quote);
            }

            first = false;
        }
        sb.append('\n');
        w.append(sb.toString());
    }
}
