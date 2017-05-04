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

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.hobbit.core.Constants;
import org.hobbit.storage.client.StorageServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Milos Jovanovik (mjovanovik@openlinksw.com)
 */
public class TestRPCClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestRPCClient.class);

    /**
     * Maximum number of retries that are executed to connect to RabbitMQ.
     */
    public static final int NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ = 5;
    /**
     * Time, the system waits before retrying to connect to RabbitMQ. Note that
     * this time will be multiplied with the number of already failed tries.
     */
    public static final long START_WAITING_TIME_BEFORE_RETRY = 5000;

    private Connection connection;
    private Channel channel;
    private String requestQueueName = Constants.STORAGE_QUEUE_NAME;
    private String replyQueueName;
    private QueueingConsumer consumer;

    public TestRPCClient() throws Exception {

        if (System.getenv().containsKey(Constants.RABBIT_MQ_HOST_NAME_KEY)) {
            ConnectionFactory factory = new ConnectionFactory();
            String rabbitMQHostName = System.getenv().get(Constants.RABBIT_MQ_HOST_NAME_KEY);
            factory.setHost(rabbitMQHostName);
            for (int i = 0; (connection == null) && (i <= NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ); ++i) {
                try {
                    connection = factory.newConnection();
                } catch (Exception e) {
                    if (i < NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ) {
                        long waitingTime = START_WAITING_TIME_BEFORE_RETRY * (i + 1);
                        LOGGER.warn(
                                "Couldn't connect to RabbitMQ with try #" + i + ". Next try in " + waitingTime + "ms.",
                                e);
                        try {
                            Thread.sleep(waitingTime);
                        } catch (Exception e2) {
                            LOGGER.warn("Interrupted while waiting before retrying to connect to RabbitMQ.", e2);
                        }
                    }
                }
            }
            if (connection == null) {
                String msg = "Couldn't connect to RabbitMQ after " + NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ
                        + " retries.";
                LOGGER.error(msg);
                throw new Exception(msg);
            }
        } else {
            String msg = "Couldn't get " + Constants.RABBIT_MQ_HOST_NAME_KEY
                    + " from the environment. This component won't be able to connect to RabbitMQ.";
            LOGGER.error(msg);
            throw new Exception(msg);
        }
        channel = connection.createChannel();

        replyQueueName = channel.queueDeclare().getQueue();
        consumer = new QueueingConsumer(channel);
        channel.basicConsume(replyQueueName, true, consumer);
    }

    public String call(String message) throws Exception {
        String response = null;
        String corrId = UUID.randomUUID().toString();

        BasicProperties props = new BasicProperties.Builder().correlationId(corrId).replyTo(replyQueueName).build();

        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response = new String(delivery.getBody(), "UTF-8");
                break;
            }
        }

        return response;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws Exception {
        connection.close();
    }

    public static void main(String[] argv) {
        TestRPCClient rcpSPARQL = null;
        StorageServiceClient storageClient = null;
        String response = null;
        try {
            rcpSPARQL = new TestRPCClient();

            Model model = ModelFactory.createDefaultModel();
            model.add(model.createResource("http://example.org/TestClass1"), RDF.type, RDFS.Class);
            storageClient = StorageServiceClient.create(rcpSPARQL.getConnection());
            storageClient.sendInsertQuery(model, Constants.PUBLIC_RESULT_GRAPH_URI);

            model.add(model.createResource("http://example.org/TestClass2"), RDF.type, RDFS.Class);
            model.setNsPrefix("ex", "http://example.org/");
            storageClient.sendInsertQuery(model, Constants.PUBLIC_RESULT_GRAPH_URI);

            System.out.println("[Test Client] Sending a test SPARQL Endpoint call.");
            response = rcpSPARQL.call("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o } LIMIT 100");
            System.out.println("[Test Client] Got the following response: \n'" + response + "'");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rcpSPARQL != null) {
                try {
                    rcpSPARQL.close();
                } catch (Exception ignore) {
                }
            }
            IOUtils.closeQuietly(storageClient);
        }
    }
}
