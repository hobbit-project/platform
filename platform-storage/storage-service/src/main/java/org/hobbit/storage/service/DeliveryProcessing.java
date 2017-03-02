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
