package de.usu.research.hobbit.gui.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.atlas.json.JSON;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import javax.json.Json;
import javax.json.JsonReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ExperimentsResourcesTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void query() throws Exception {
        environmentVariables.set("ELASTICSEARCH_HOST", "localhost");
        environmentVariables.set("ELASTICSEARCH_HTTP_PORT", "9200");

        String experimentId = "1521118896241";
        LogsResources logsResource = new LogsResources();
        String logs = logsResource.query(experimentId, "benchmark");
        System.out.println(logs);
    }
}