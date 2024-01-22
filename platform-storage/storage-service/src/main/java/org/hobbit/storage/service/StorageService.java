/**
 * This file is part of storage-service.
 *
 * storage-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * storage-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with storage-service.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hobbit.storage.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.sparql.modify.UpdateProcessRemote;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractComponent;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.QueueingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Delivery;

/**
 *
 * @author Milos Jovanovik (mjovanovik@openlinksw.com)
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 */
public class StorageService extends AbstractComponent implements CredentialsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageService.class);

    private static final int MAX_NUMBER_PARALLEL_REQUESTS = 10;
    /**
     * The time the main receiver thread will sleep if it didn't receive any message
     * (in seconds).
     */
    private static final int WAITING_TIME_BEFORE_CHECKING_STATUS = 60;

    /**
     * Maximum result size used for pagination.
     */
    private static final int MAX_RESULT_SIZE = 1000;

    /**
     * The queue name for communication between the Platform components and the
     * Storage component.
     */
    private static final String QUEUE_NAME = Constants.STORAGE_QUEUE_NAME;

    /**
     * The environment variable containing the URL to the SPARQL Endpoint used as a
     * Storage component for the Platform.
     */
    private static final String SPARQL_ENDPOINT_URL_KEY = "SPARQL_ENDPOINT_URL";

    /**
     * The environment variable containing the username for authenticating with the
     * SPARQL Endpoint.
     */
    private static final String SPARQL_ENDPOINT_USERNAME_KEY = "SPARQL_ENDPOINT_USERNAME";

    /**
     * The environment variable containing the password for authenticating with the
     * SPARQL Endpoint.
     */
    private static final String SPARQL_ENDPOINT_PASSWORD_KEY = "SPARQL_ENDPOINT_PASSWORD";

    /**
     * The queue on which this service is listening for requests.
     */
    private RabbitQueue queue = null;

    /**
     * The consumer used to consume requests.
     */
    private QueueingConsumer consumer = null;

    /**
     * The URL of the SPARQL endpoint.
     */
    private String sparqlEndpointUrl = null;

    /**
     * The Query factory used to query the SPARQL endpoint.
     */
    private QueryExecutionFactory queryExecFactory = null;

    /**
     * The provided credentials.
     */
    private Credentials credentials = null;

    /**
     * The HTTP client used for communication with the SPARQL endpoint.
     */
    private CloseableHttpClient client = null;

    /**
     * Calls the SPARQL Endpoint denoted by the URL, to execute the queryString.
     *
     * @param queryString The query to be executed
     * @return Returns the queryString results serialized in JSON
     * @throws Exception If endpoint not reachable, exception while executing query,
     *                   etc.
     */
    public String callSparqlEndpoint(String queryString) throws Exception {
        String response = null;
        QueryExecution qexec = null;

        LOGGER.info("Received a request to call the SPARQL Endpoint at {} and execute the following query: {}",
                sparqlEndpointUrl, queryString.replace("\n", " "));

        // TODO: Fix this with something better
        String queryKeywords = reduceQueryToKeyWords(queryString);
        if (queryKeywords.contains("INSERT") || queryKeywords.contains("UPDATE") || queryKeywords.contains("DELETE")) {
            // It's probably a SPARQL UPDATE query
            try {
                // The UPDATE query will go to the protected SPARQL endpoint, at
                // /sparql-auth
                UpdateProcessRemote update = (UpdateProcessRemote) UpdateExecutionFactory
                        .createRemote(UpdateFactory.create(queryString), sparqlEndpointUrl, client);
                update.execute(); // There's no response from an UPDATE query
                System.out.println("[Storage Service] Done with the SPARQL UPDATE.");
                response = "Successfully executed the SPARQL UPDATE.";
            } catch (Exception e) {
                System.out.println("Error: " + e.toString());
                throw e;
            }
        } else {
            try {
                // Prepare the query execution
                Query query = QueryFactory.create(queryString);
                qexec = queryExecFactory.createQueryExecution(query);
                ResultSet results = null;
                Model resultsModel = null;
                Boolean resultsBoolean = null;

                // Execute the query and get the results
                // Detect query type (SELECT, CONSTRUCT, DESCRIBE, ASK)
                if (query.isSelectType())
                    results = qexec.execSelect();
                else if (query.isConstructType())
                    resultsModel = qexec.execConstruct();
                else if (query.isDescribeType())
                    resultsModel = qexec.execDescribe();
                else if (query.isAskType())
                    resultsBoolean = qexec.execAsk();

                // Process the response
                if (query.isSelectType()) {
                    // Transform the ResultSet into a JSON serialization
                    ByteArrayOutputStream jsonOutputStream = new ByteArrayOutputStream();
                    ResultSetFormatter.outputAsJSON(jsonOutputStream, results);
                    String jsonResults = new String(jsonOutputStream.toByteArray(), "UTF-8");
                    LOGGER.debug("[Storage Service] Results serialized in JSON: \n{}", jsonResults);
                    response = jsonResults;
                } else if (query.isConstructType() || query.isDescribeType()) {
                    // Transform the Model into a JSON serialization
                    ByteArrayOutputStream jsonOutputStream = new ByteArrayOutputStream();
                    resultsModel.write(jsonOutputStream, "JSON-LD");
                    String jsonResults = new String(jsonOutputStream.toByteArray(), "UTF-8");
                    LOGGER.debug("[Storage Service] Results serialized in JSON: \n{}", jsonResults);
                    response = jsonResults;
                } else if (query.isAskType()) {
                    LOGGER.debug("[Storage Service] Result is: {}", resultsBoolean.toString());
                    response = resultsBoolean.toString();
                }
            } catch (Exception e) {
                LOGGER.error("Exception while executing query.", e);
                throw e;
            } finally {
                try {
                    qexec.close();
                } catch (Exception e) {
                }
            }
        }
        return response;
    }

    @Override
    public void init() throws Exception {
        super.init();

        sparqlEndpointUrl = getEnvValue(SPARQL_ENDPOINT_URL_KEY, true) + "-auth";
        String username = getEnvValue(SPARQL_ENDPOINT_USERNAME_KEY, true);
        String password = getEnvValue(SPARQL_ENDPOINT_PASSWORD_KEY, true);
        credentials = new UsernamePasswordCredentials(username, password);

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultCredentialsProvider(this);
        client = clientBuilder.build();

        queryExecFactory = new QueryExecutionFactoryHttp(sparqlEndpointUrl, new DatasetDescription(), client);
        queryExecFactory = new QueryExecutionFactoryPaginated(queryExecFactory, MAX_RESULT_SIZE);

        queue = incomingDataQueueFactory.createDefaultRabbitQueue(QUEUE_NAME);
        queue.channel.basicQos(MAX_NUMBER_PARALLEL_REQUESTS);

        consumer = new QueueingConsumer(queue.channel);
        queue.channel.basicConsume(QUEUE_NAME, false, consumer);
    }

    @Override
    public void run() throws Exception {
        LOGGER.info("[Storage Service] Awaiting Storage Service requests");
        ExecutorService executor = Executors.newFixedThreadPool(MAX_NUMBER_PARALLEL_REQUESTS);
        Delivery delivery;
        while (true) {
            delivery = null;
            // Let's wait for a delivery for 60 seconds
            try {
                delivery = consumer.getDeliveryQueue().poll(WAITING_TIME_BEFORE_CHECKING_STATUS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // interrupted; just continue
            }
            if (delivery != null) {
                executor.execute(new DeliveryProcessing(this, delivery, queue));
            } else {
                // This would be the place at which we could react to signals, e.g., terminate
                // the service if needed.
            }
        }
    }

    /**
     * A simple method that reduces the given query to the parts that are located
     * outside of brackets, i.e.,parts that match {{@code ...}} and {@code <...>}
     * are removed. It can be used to make sure that only keywords are processed.
     *
     * @param query the SPARQL query that should be reduced
     * @return the reduced SPARQL query
     */
    protected static String reduceQueryToKeyWords(String query) {
        StringBuilder reduced = new StringBuilder(query);
        int startPos = reduced.lastIndexOf("{");
        int endPos;
        /*
         * First, delete all {...} constructs. We start with the last inner bracket,
         * delete it and search for the next last inner bracket.
         */
        while (startPos >= 0) {
            endPos = reduced.indexOf("}", startPos);
            reduced.delete(startPos, endPos + 1);
            startPos = reduced.lastIndexOf("{");
        }
        startPos = reduced.lastIndexOf("<");
        while (startPos >= 0) {
            endPos = reduced.indexOf(">", startPos);
            reduced.delete(startPos, endPos + 1);
            startPos = reduced.lastIndexOf("<");
        }
        return reduced.toString();
    }

    /**
     * Retrieves the value of the environmental variable with the given key if such
     * a variable can be found. Otherwise, if the given essential flag is
     * <code>true</code> an {@link IllegalStateException} is thrown. If the flag is
     * <code>false</code>, <code>null</code> is returned.
     *
     * @param key       the name of the environmental variable
     * @param essential a flag indicating whether the value must be retrievable
     * @return the value of the environmental variable or <code>null</code> if the
     *         variable couldn't be found and the essential flag is
     *         <code>false</code>.
     * @throws IllegalStateException if the variable couldn't be found and the
     *                               essential flag is <code>true</code>.
     */
    protected String getEnvValue(String key, boolean essential) {
        String value = null;
        if (System.getenv().containsKey(key)) {
            value = System.getenv().get(key);
        }
        if (essential && (value == null)) {
            throw new IllegalStateException("Couldn't get " + key + " from the environment. Aborting.");
        }
        return value;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(queue);
        try {
            queryExecFactory.close();
        } catch (Exception e) {
        }
        IOUtils.closeQuietly(client);
        super.close();
    }

    @Override
    public void clear() {
    }

    @Override
    public Credentials getCredentials(AuthScope scope) {
        return credentials;
    }

    @Override
    public void setCredentials(AuthScope arg0, Credentials arg1) {
        LOGGER.error("I am a read-only credential provider but got a call to set credentials.");
    }

    /**
     * Main method for debugging purposes.
     * 
     * @param args
     */
    public static void main(String[] args) {
        StorageService storage = new StorageService();
        try {
            storage.init();
            System.out.println(storage.callSparqlEndpoint(
                    "select distinct ?g ?p ?o where { graph ?g { <http://w3id.org/hobbit/experiments#1520529269933> ?p ?o }} LIMIT 100"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(storage);
        }
    }
}
