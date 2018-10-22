/**
 * This file is part of gui-serverbackend.
 *
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rest;

import static de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper.getTolerantDateTimeValue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.elasticsearch.client.RestClient;
import org.hobbit.core.Constants;
import org.hobbit.storage.queries.SparqlQueries;
import org.hobbit.vocab.HOBBIT;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.format.log.CSVFormatter;
import de.usu.research.hobbit.gui.format.log.JSONFormatter;
import de.usu.research.hobbit.gui.format.log.LogFormatter;
import de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.ExperimentBean;

@Path("logs")
public class LogsResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogsResources.class);

    private static final String UNKNOWN_EXP_ERROR_MSG = "Could not find results for this experiments. "
            + "Either the experiment has not been finished or it does not exist.";
    private static final String benchmarkQueryBase = "" + "{" + "%s" + // placeholder for _source or other extensions
            "\"query\" : {" + "\"constant_score\": {" + "\"filter\": {" + "\"bool\": {" + "\"must\": {"
            + "\"range\": { \"@timestamp\": { \"gte\": \"%s\", \"lte\": \"%s||+1d/d\" }}" + "}," + "\"should\": ["
            + "{\"wildcard\": {\"tag\":\"benchmark_sep_%s_sep_*\"}},"
            + "{\"wildcard\": {\"tag\":\"data_sep_%s_sep_*\"}}" + "]" + "}" + "}" + "}" + "}" + "}";

    private static final String systemQueryBase = "" + "{" + "%s" + // placeholder for _source or other extensions
            "\"query\" : {" + "\"constant_score\": {" + "\"filter\": {" + "\"bool\": {" + "\"must\": {"
            + "\"range\": { \"@timestamp\": { \"gte\": \"%s\", \"lte\": \"%s||+1d/d\" }}" + "}," + "\"should\": ["
            + "{\"wildcard\": {\"tag\":\"system_sep_%s_sep_*\"}}" + "]" + "}" + "}" + "}" + "}" + "}";

    private final int MAX_RESULT_SIZE = 100000;

    private LogFormatter defaultFormatter;
    private Map<String, LogFormatter> formatters;

    public LogsResources() {
        defaultFormatter = new JSONFormatter();
        formatters = new HashMap<String, LogFormatter>();
        formatters.put("JSON", defaultFormatter);
        String logFields[] = new String[] { "@timestamp", "image_name", "container_name", "message" };
        formatters.put("CSV", new CSVFormatter(logFields));
        formatters.put("TXT", new CSVFormatter(logFields, ' ', ' '));
    }

    @GET
    @RolesAllowed("system-provider") // Guests can not access this method
    @Path("benchmark/query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response benchmarkQuery(@QueryParam("id") String id, @Context SecurityContext sc, @QueryParam("format") @DefaultValue("JSON") String format) throws Exception {
        String logs = query(id, "benchmark", format);
        if (logs == null) {
            Response.ok(UNKNOWN_EXP_ERROR_MSG).build();
        }

        return Response.ok(logs).build();
    }

    @GET
    @RolesAllowed("system-provider") // Guests can not access this method
    @Path("system/query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response systemQuery(@QueryParam("id") String id, @Context SecurityContext sc, @QueryParam("format") @DefaultValue("JSON") String format) throws Exception {
        Model model = getExperimentModel(id);
        Response isAllowed = checkAccessAllowed(model, id, sc);
        if (isAllowed != null) {
            return isAllowed;
        }

        String logs = query(id, "system", format);
        if (logs == null) {
            // The experiment is not known
            Response.ok(UNKNOWN_EXP_ERROR_MSG).build();
        }

        return Response.ok(logs).build();
    }

    @GET
    @RolesAllowed("system-provider") // Guests can not access this method
    @Path("query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(@QueryParam("id") String id, @Context SecurityContext sc,
            @QueryParam("format") @DefaultValue("JSON") String format) throws Exception {
        String logs = query(id, "all", format);
        if (logs == null) {
            Response.ok(UNKNOWN_EXP_ERROR_MSG).build();
        }

        return Response.ok(logs).build();
    }

    private Model getExperimentModel(String experimentId) {
        String experimentUri = Constants.EXPERIMENT_URI_NS + experimentId;
        String query = SparqlQueries.getExperimentGraphQuery(experimentUri, null);
        Model model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
        return model;
    }

    private Response checkAccessAllowed(Model experimentModel, String experimentId, SecurityContext sc) {
        String experimentUri = Constants.EXPERIMENT_URI_NS + experimentId;
        // get the date info from model and restrict query by time
        if (experimentModel != null && experimentModel.size() > 0) {
            ExperimentBean experiment = RdfModelHelper.createExperimentBean(experimentModel,
                    experimentModel.getResource(experimentUri));
            // Check whether the user is the owner of the system
            String systemURI = experiment.getSystem().getId();
            Set<String> userOwnedSystemIds = InternalResources.getUserSystemIds(sc);
            if (!userOwnedSystemIds.contains(systemURI)) {
                // The user is not allowed to see the systems log
                return Response.status(Status.FORBIDDEN).build();
            }
        } else {
            return Response.status(Status.NO_CONTENT).build();
        }
        return null;
    }

    private String getExperimentDate(String experimentId) {
        Model experimentModel = getExperimentModel(experimentId);
        String experimentUri = Constants.EXPERIMENT_URI_NS + experimentId;
        Resource experimentResource = experimentModel.getResource(experimentUri);
        Calendar cal = getTolerantDateTimeValue(experimentModel, experimentResource, HOBBIT.startTime);
        SimpleDateFormat esDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (cal != null) {
            return esDateFormat.format(cal.getTime()); // or whatever you need
        } else {
            LOGGER.error("Experiment with id {} does not have startTime property in the database.", experimentId);
            LOGGER.error("I set experiment date to 1970-01-01, the log result will be empty.");
            return "1970-01-01";
        }
    }

    public String query(String experimentId, String type, String format) throws Exception {
        RestClient restClient = null;
        String esHost = System.getenv("ELASTICSEARCH_HOST");
        String esPort = System.getenv("ELASTICSEARCH_HTTP_PORT");
        if (esHost != null && esPort != null) {
            restClient = RestClient.builder(new HttpHost(esHost, Integer.parseInt(esPort), "http")).build();
        } else {
            throw new Exception("ELASTICSEARCH_HOST and ELASTICSEARCH_HTTP_PORT env are not set.");
        }
        String logs = null;
        switch (type) {
        case "all":
            logs = getLogs(experimentId, restClient, format);
            break;
        case "system":
            logs = getSystemLogs(experimentId, restClient, format);
            break;
        case "benchmark":
            logs = getBenchmarkLogs(experimentId, restClient, format);
            break;
        }
        return logs;
    }

    private String getBenchmarkLogs(String experimentId, RestClient restClient, String format) throws Exception {
        return transformFormat(getLogsByType(experimentId, "benchmark", restClient), format);
    }

    private String getSystemLogs(String experimentId, RestClient restClient, String format) throws Exception {
        return transformFormat(getLogsByType(experimentId, "system", restClient), format);
    }

    private String getLogs(String experimentId, RestClient restClient, String format) throws Exception {
        JSONArray benchmarkLogs = getLogsByType(experimentId, "benchmark", restClient);
        JSONArray systemLogs = getLogsByType(experimentId, "system", restClient);
        return transformFormat(mergeJSONArrays(benchmarkLogs, systemLogs), format);
    }

    private JSONArray getLogsByType(String experimentId, String type, RestClient restClient) throws Exception {
        String experimentDate = getExperimentDate(experimentId);
        String countQuery = createCountQuery(experimentId, type);
        String countJsonString = fireQuery(countQuery, experimentDate, "count", restClient);
        JSONObject jsonObject = new JSONObject(countJsonString);
        Integer count = Integer.parseInt(jsonObject.get("count").toString());

        JSONArray results = new JSONArray();
        if (count > MAX_RESULT_SIZE) {
            LOGGER.info("Log size {} of experiment id {} is bigger than {}. Experiment should log less messages.",
                    count, experimentId, MAX_RESULT_SIZE);
            JSONObject error = new JSONObject();
            String errorMessage = String.format(
                    "Log size %s of experiment id %s is bigger than %s. Please descrease your experiment logging to retrieve the log messages.",
                    count, experimentId, MAX_RESULT_SIZE);
            error.put("error", errorMessage);
            results.put(error);
            return results;
        }

        // pagination
        Integer offset = 0;
        Integer size = 10000;

        String lastSortValue = null;
        String sortValue;
        float status = 0;
        while (offset < MAX_RESULT_SIZE && offset < count) {
            String searchQuery;
            if (lastSortValue == null) {
                searchQuery = createSearchQuery(size, experimentId, type);
            } else {
                searchQuery = createSearchQuery(lastSortValue, size, experimentId, type);
            }
            String queryResults = fireQuery(searchQuery, experimentDate, "search", restClient);
            JSONObject queryResultsJson = new JSONObject(queryResults);
            JSONArray hits = queryResultsJson.getJSONObject("hits").getJSONArray("hits");
            if (hits.length() == 0)
                break;
            JSONObject lastHit = hits.getJSONObject(hits.length() - 1);
            sortValue = lastHit.optJSONArray("sort").toString();
            lastSortValue = sortValue.substring(1, sortValue.length() - 1);
            results = mergeJSONArrays(results, hits);
            offset += size;
            if (offset != 0) {
                status = ((float) offset / count) * 100;
            }
            if (offset > count) {
                status = 100;
            }
            LOGGER.info("Retrieving logs for experiment id: {}, {}%", experimentId, status);
        }
        status = 100;
        LOGGER.info("Retrieving logs for experiment id: {}, {}%", experimentId, status);
        return results;
    }

    private JSONArray mergeJSONArrays(JSONArray array_1, JSONArray array_2) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < array_1.length(); i++) {
            result.put(array_1.getJSONObject(i));
        }
        for (int i = 0; i < array_2.length(); i++) {
            result.put(array_2.getJSONObject(i));
        }
        return result;
    }

    private String transformFormat(JSONArray logs, String format) {
        if (formatters.containsKey(format)) {
            return formatters.get(format).format(logs);
        } else {
            LOGGER.error("Unkown log format {}. Using default.", format);
            return defaultFormatter.format(logs);
        }
    }

    private String createQuery(String extension, String experimentId, String type) throws Exception {
        // date
        String experimentDate = getExperimentDate(experimentId);

        if (type.equals("benchmark")) {
            return String.format(benchmarkQueryBase, extension, experimentDate, experimentDate, experimentId,
                    experimentId);
        } else if (type.equals("system")) {
            return String.format(systemQueryBase, extension, experimentDate, experimentDate, experimentId);
        }
        throw new Exception("Can not generate query of type " + type);
    }

    private String createCountQuery(String experimentId, String type) throws Exception {
        return createQuery("", experimentId, type);
    }

    private String createSearchQuery(String lastSortValue, Integer size, String experimentId, String type)
            throws Exception {
        String extension = "\"_source\": [\"@timestamp\", \"image_name\", \"container_name\", \"container_id\", \"message\"],"
                + "\"size\":" + size.toString() + "," + "\"search_after\": [" + lastSortValue + "],"
                + "\"sort\": [{ \"@timestamp\" : \"desc\"}],";
        return createQuery(extension, experimentId, type);
    }

    private String createSearchQuery(Integer size, String experimentId, String type) throws Exception {
        String extension = "\"_source\": [\"@timestamp\", \"image_name\", \"container_name\", \"container_id\", \"message\"],"
                + "\"size\":" + size.toString() + "," + "\"sort\": [{ \"@timestamp\" : \"desc\"}],";
        return createQuery(extension, experimentId, type);
    }

    private String fireQuery(String query, String experimentDate, String type, RestClient restClient) throws IOException {
        HttpEntity entity = new NStringEntity(query, ContentType.APPLICATION_JSON);
        SimpleDateFormat esDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat logstashDateFormat = new SimpleDateFormat("yyyy.MM.dd");
        String experimentDate1 = "";
        String experimentDate2 = "";
        try {
            Date experimentDateParsed = esDateFormat.parse(experimentDate);
            experimentDate1 = logstashDateFormat.format(experimentDateParsed);
            experimentDateParsed.setDate(experimentDateParsed.getDate() + 1);
            experimentDate2 = logstashDateFormat.format(experimentDateParsed);
        } catch (ParseException e) {
            // Leave dates empty.
            // The following request will try to query all indexes,
            // which will fail if there are too many.
        }

        org.elasticsearch.client.Response response = restClient.performRequest(
                "GET",
                "/logstash-"+experimentDate1+"*,logstash-"+experimentDate2+"*/_"+type,
                Collections.singletonMap("pretty", "true"),
                entity
        );
        return EntityUtils.toString(response.getEntity());
    }
}
