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
import org.hobbit.encryption.AES;
import org.hobbit.encryption.AESException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Delivery;

public class DeliveryProcessing implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryProcessing.class);

    private StorageService storage;
    private Delivery delivery;
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
        AES encryption = null;
        String AES_PASSWORD = System.getenv("AES_PASSWORD");
        String AES_SALT = System.getenv("AES_SALT");
        if(AES_PASSWORD != null && AES_SALT != null) {
            encryption = new AES(AES_PASSWORD, AES_SALT);
        }

        try {
            String query = null;
            byte[] message = delivery.getBody();
            if(encryption != null) {
                byte[] decryptedMessage = encryption.decrypt(message);
                query = new String(decryptedMessage);
            } else {
                query = RabbitMQUtils.readString(delivery.getBody());
            }
            response = storage.callSparqlEndpoint(query);
        } catch (com.rabbitmq.client.ShutdownSignalException e) {
            LOGGER.info("Got a ShutdownSignalException. Aborting.");
            return;
        } catch (AESException e ) {
            LOGGER.error("Encryption failed while trying to decrypt incoming message.", e);
        } catch (Exception e) {
            LOGGER.error("Exception while calling SPARQL endpoint.", e);
            response = "";
        } finally {
            // if (response == null || response.equals("")) {
            // response = "Fallback response, due to an error.";
            // }
            try {
                if(encryption != null) {
                    byte[] encryptedResponse = null;
                    encryptedResponse = encryption.encrypt(response);
                    queue.channel.basicPublish(
                            "",
                            props.getReplyTo(),
                            replyProps,
                            encryptedResponse
                    );
                } else {
                    queue.channel.basicPublish(
                            "",
                            props.getReplyTo(),
                            replyProps,
                            RabbitMQUtils.writeString(response)
                    );
                }
                queue.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (AESException e ) {
                LOGGER.error("Encryption failed while trying to send response.", e);
            } catch (Exception e) {
                LOGGER.error("Exception while trying to send response.", e);
            }
        }
    }

}
