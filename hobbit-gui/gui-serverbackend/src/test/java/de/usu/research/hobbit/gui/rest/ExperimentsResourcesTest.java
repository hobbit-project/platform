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
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ExperimentsResourcesTest {
    @Test
    public void query() throws Exception {
        String experimentId = "1515598150763";
    }
}