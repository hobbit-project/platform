/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hobbit.storage.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.modify.UpdateProcessRemote;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractComponent;
import org.hobbit.core.data.RabbitQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.QueueingConsumer;

/**
 *
 * @author Milos Jovanovik (mjovanovik@openlinksw.com)
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 */
public class StorageService extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageService.class);

    /**
     * The queue name for communication between the Platform components and the
     * Storage component.
     */
    private static final String QUEUE_NAME = Constants.STORAGE_QUEUE_NAME;

    /**
     * The environment variable containing the URL to the SPARQL Endpoint
     * used as a Storage component for the Platform.
     */
    private static final String SPARQL_ENDPOINT_URL_KEY = "SPARQL_ENDPOINT_URL";
    
    /**
     * The environment variable containing the username for authenticating
     * with the SPARQL Endpoint.
     */
    private static final String SPARQL_ENDPOINT_USERNAME = "HobbitPlatform";
    
    /**
     * The environment variable containing the password for authenticating
     * with the SPARQL Endpoint.
     */
    private static final String SPARQL_ENDPOINT_PASSWORD = "HobbitPlatformStorage";

    private RabbitQueue queue = null;
    private String sparqlEndpointUrl = null;
    private QueueingConsumer consumer = null;

    /**
     * Calls the SPARQL Endpoint denoted by the URL, to execute the queryString.
     * 
     * @param endpointURL
     *            The URL of the SPARQL Endpoint
     * @param queryString
     *            The query to be executed
     * @return Returns the queryString results serialized in JSON
     */
    private static String callSparqlEndpoint(String endpointURL, String queryString) throws Exception {
        String response = null;
        QueryExecution qexec = null;

        System.out.println("[Storage Service] Received a request to call the SPARQL Endpoint at " + endpointURL
                + " and execute the following query: " + queryString);

        // TODO: Fix this with something better
        if (queryString.contains("INSERT") || queryString.contains("UPDATE")) { // It's probably a SPARQL UPDATE query
            try {
                // The UPDATE query will go to the protected SPARQL endpoint, at /sparql-auth
                UpdateProcessRemote update = (UpdateProcessRemote) UpdateExecutionFactory.createRemote(UpdateFactory.create(queryString), endpointURL + "-auth");                
                update.setAuthentication(SPARQL_ENDPOINT_USERNAME, SPARQL_ENDPOINT_PASSWORD.toCharArray());
                update.execute(); // There's no response from an UPDATE query
                System.out.println("[Storage Service] Done with the SPARQL UPDATE.");
                response = "Successfully executed the SPARQL UPDATE.";
            } catch (Exception e) {
                System.out.println("Error: " + e.toString());
                throw e;
            }
        }
        else {
            try {
                // Prepare the query execution
                Query query = QueryFactory.create(queryString);
                qexec = QueryExecutionFactory.sparqlService(endpointURL, query);
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
                    LOGGER.debug("[Storage Service] Results serialized in JSON: \n" + jsonResults);
                    response = jsonResults;
                } else if (query.isConstructType() || query.isDescribeType()) {
                    // Transform the Model into a JSON serialization
                    ByteArrayOutputStream jsonOutputStream = new ByteArrayOutputStream();
                    resultsModel.write(jsonOutputStream, "JSON-LD");
                    String jsonResults = new String(jsonOutputStream.toByteArray(), "UTF-8");
                    LOGGER.debug("[Storage Service] Results serialized in JSON: \n" + jsonResults);
                    response = jsonResults;
                } else if (query.isAskType()) {
                    LOGGER.debug("[Storage Service] Result is: " + resultsBoolean.toString());
                    response = resultsBoolean.toString();
                }
            } catch (Exception e) {
                LOGGER.error("Error: " + e.toString());
                throw e;
            } finally {
                qexec.close();
            }
        }
        return response;
    }

    @Override
    public void init() throws Exception {
        super.init();

        sparqlEndpointUrl = null;
        if (System.getenv().containsKey(SPARQL_ENDPOINT_URL_KEY)) {
            sparqlEndpointUrl = System.getenv().get(SPARQL_ENDPOINT_URL_KEY);
        }
        if (sparqlEndpointUrl == null) {
            throw new IllegalStateException("Couldn't get " + SPARQL_ENDPOINT_URL_KEY + " from the environment.");
        }
        
        queue = createDefaultRabbitQueue(QUEUE_NAME);
        queue.channel.basicQos(1);

        consumer = new QueueingConsumer(queue.channel);
        queue.channel.basicConsume(QUEUE_NAME, false, consumer);
    }

    @Override
    public void run() throws Exception {
        LOGGER.info("[Storage Service] Awaiting Storage Service requests");
        BasicProperties props;
        BasicProperties replyProps;
        while (true) {
            String response = null;

            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            props = delivery.getProperties();
            replyProps = new BasicProperties.Builder().correlationId(props.getCorrelationId()).build();

            try {
                String query = new String(delivery.getBody(), "UTF-8");
                response = callSparqlEndpoint(sparqlEndpointUrl, query);
            } catch (Exception e) {
                LOGGER.error("Exception while calling SPARQL endpoint.", e);
                response = "";
            } finally {
                if (response == null || response.equals("")) {
                    response = "Fallback response, due to an error.";
                }
                queue.channel.basicPublish("", props.getReplyTo(), replyProps, response.getBytes("UTF-8"));
                queue.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(queue);
        super.close();
    }
}
