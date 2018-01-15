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

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

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
import org.elasticsearch.client.RestClient;
import org.hobbit.core.Constants;
import org.hobbit.storage.queries.SparqlQueries;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.ExperimentBean;


@Path("logs")
public class LogsResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentsResources.class);
    private static final String UNKNOWN_EXP_ERROR_MSG = "Could not find results for this experiments. Either the experiment has not been finished or it does not exist.";
    private static final String benchmarkQueryBase = "" +
        "{" +
        "%s" + // placeholder for _source or other extensions
        "\"query\" : {" +
            "\"constant_score\": {" +
                    "\"filter\": {" +
                        "\"bool\": {" +
                            "\"should\": [" +
                                "{\"wildcard\": {\"tag\":\"benchmark_sep_%s_sep_*\"}}," +
                                "{\"wildcard\": {\"tag\":\"data_sep_%s_sep_*\"}}" +
                            "]" +
                        "}" +
                    "}" +
                "}" +
            "}" +
        "}";

    private static final String systemQueryBase = "{" +
        "%s" + // placeholder for _source or other extensions
        "\"query\" :" +
            "{\"wildcard\": {\"tag\":\"system_sep_%s_sep_*\"}}" +
        "}";

    @GET
    @Path("benchmark/query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response benchmarkQuery(@QueryParam("id") String id, @Context SecurityContext sc) throws Exception {
        String logs = query(id, "benchmark");
        if(logs == null) {
            Response.ok(UNKNOWN_EXP_ERROR_MSG).build();
        }

        return Response.ok(logs).build();
    }

    @GET
    @Path("system/query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response systemQuery(@QueryParam("id") String id, @Context SecurityContext sc) throws Exception {
        // create experiment URI
        String experimentUri = Constants.EXPERIMENT_URI_NS + id;
        // construct query to gather experiment data
        String query = SparqlQueries.getExperimentGraphQuery(experimentUri, null);
        Model model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
        if (model != null && model.size() > 0) {
            ExperimentBean experiment = RdfModelHelper.createExperimentBean(model, model.getResource(experimentUri));
            // Check whether the user is the owner of the system
            String systemURI = experiment.getSystem().getId();
            Set<String> userOwnedSystemIds = InternalResources.getUserSystemIds(sc);
            if(!userOwnedSystemIds.contains(systemURI)) {
                // The user is not allowed to see the systems log
                return Response.status(Status.FORBIDDEN).build();
            }
        } else {
            return Response.status(Status.NO_CONTENT).build();
        }
        String logs = query(id, "system");
        if(logs == null) {
            // The experiment is not known
            Response.ok(UNKNOWN_EXP_ERROR_MSG).build();
        }

        return Response.ok(logs).build();
    }

    @GET
    @Path("query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(@QueryParam("id") String id, @Context SecurityContext sc) throws Exception {
        String logs = query(id, "all");
        if(logs == null) {
            Response.ok(UNKNOWN_EXP_ERROR_MSG).build();
        }

        return Response.ok(logs).build();
    }

    public String query(String experimentId, String type) throws Exception {
        RestClient restClient = null;
        String esHost = System.getenv("ELASTICSEARCH_HOST");
        String esPort = System.getenv("ELASTICSEARCH_HTTP_PORT");
        if(esHost != null && esPort != null) {
            restClient = RestClient.builder(
                    new HttpHost(esHost, Integer.parseInt(esPort), "http")
            ).build();
        } else {
            throw new Exception("ELASTICSEARCH_HOST and ELASTICSEARCH_HTTP_PORT env are not set.");
        }
        String logs = null;
        if(type.equals("all")) {
            logs = getLogs(experimentId, restClient);
        } else if (type.equals("system")) {
            logs = getSystemLogs(experimentId, restClient);
        } else if (type.equals("benchmark")) {
            logs = getBenchmarkLogs(experimentId, restClient);
        }
        return logs;
    }

    public String getBenchmarkLogs(String experimentId, RestClient restClient) throws Exception {
        return getLogsByType(experimentId, "benchmark", restClient).toString();
    }

    public String getSystemLogs(String experimentId, RestClient restClient) throws Exception {
        return getLogsByType(experimentId, "system", restClient).toString();
    }

    public String getLogs(String experimentId, RestClient restClient) throws Exception {
        JSONArray benchmarkLogs = getLogsByType(experimentId, "benchmark", restClient);
        JSONArray systemLogs = getLogsByType(experimentId, "system", restClient);
        return mergeJSONArrays(benchmarkLogs, systemLogs).toString();
    }

    private JSONArray getLogsByType(String experimentId, String type, RestClient restClient) throws Exception {
        String countQuery = createCountQuery(experimentId, type);
        String countJsonString = fireQuery(countQuery, "count", restClient);
        JSONObject jsonObject = new JSONObject(countJsonString);
        Integer count = Integer.parseInt(jsonObject.get("count").toString());

        //pagination
        Integer offset = 0;
        Integer size = 1000;
        JSONArray results = new JSONArray();
        while(offset < count) {
            String searchQuery = createSearchQuery(offset, size, experimentId, type);
            String queryResults = fireQuery(searchQuery, "search", restClient);
            JSONObject queryResultsJson = new JSONObject(queryResults);
            JSONArray hits = queryResultsJson.getJSONObject("hits").getJSONArray("hits");
            results = mergeJSONArrays(results, hits);
            offset += size;
        }
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

    private String createQuery(String extension, String experimentId, String type) throws Exception {
        if(type.equals("benchmark")) {
            return String.format(benchmarkQueryBase, extension, experimentId, experimentId);
        } else if(type.equals("system")) {
            return String.format(systemQueryBase, extension, experimentId);
        }
        throw new Exception("Can not generate query of type " + type);
    }

    private String createCountQuery(String experimentId, String type) throws Exception {
        return createQuery("", experimentId, type);
    }

    private String createSearchQuery(Integer from, Integer size, String experimentId, String type) throws Exception {
        String extension = "\"_source\": [\"@timestamp\", \"image_name\", \"container_name\", \"container_id\", \"message\"]," +
                "\"from\":"+from.toString()+",\"size\":"+size.toString()+",";
        return createQuery(extension, experimentId, type);
    }

    private String fireQuery(String query, String type, RestClient restClient) throws IOException {
        HttpEntity entity = new NStringEntity(query, ContentType.APPLICATION_JSON);
        org.elasticsearch.client.Response response = restClient.performRequest(
                "GET",
                "/_"+type,
                Collections.singletonMap("pretty", "true"),
                entity
        );
        return EntityUtils.toString(response.getEntity());
    }
}
