package org.hobbit.storage.service;

import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.QueueingConsumer.Delivery;

public class DeliveryProcessing implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryProcessing.class);

    private StorageService storage;
    private QueueingConsumer.Delivery delivery;
    private RabbitQueue queue;

    public DeliveryProcessing(StorageService storage, Delivery delivery, RabbitQueue queue) {
        this.storage = storage;
        this.delivery = delivery;
        this.queue = queue;
    }

    @Override
    public void run() {
        BasicProperties props = delivery.getProperties();
        BasicProperties replyProps = new BasicProperties.Builder().correlationId(props.getCorrelationId()).build();
        String response = null;
        try {
            String query = RabbitMQUtils.readString(delivery.getBody());
            response = storage.callSparqlEndpoint(query);
        } catch (com.rabbitmq.client.ShutdownSignalException e) {
            LOGGER.info("Got a ShutdownSignalException. Aborting.");
            return;
        } catch (Exception e) {
            LOGGER.error("Exception while calling SPARQL endpoint.", e);
            response = "";
        } finally {
            // if (response == null || response.equals("")) {
            // response = "Fallback response, due to an error.";
            // }
            try {
                queue.channel.basicPublish("", props.getReplyTo(), replyProps, RabbitMQUtils.writeString(response));
                queue.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                LOGGER.error("Exception while trying to send response.", e);
            }
        }
    }

}
