package de.usu.research.hobbit.gui.rabbitmq;


import java.util.UUID;

import org.hobbit.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

@Deprecated
public class StorageServiceChannel implements AutoCloseable {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceChannel.class);

    /**
     * Maximum number of retries that are executed to connect to RabbitMQ.
     */
    public static final int NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ = 5;
    /**
     * Time, the system waits before retrying to connect to RabbitMQ. Note that
     * this time will be multiplied with the number of already failed tries.
     */
    public static final long START_WAITING_TIME_BEFORE_RETRY = 5000;

    private Channel channel;
    private String requestQueueName = Constants.STORAGE_QUEUE_NAME;
    private String replyQueueName;
    private QueueingConsumer consumer;

    public StorageServiceChannel(RabbitMQConnection connection) throws Exception {
        channel = connection.createChannel();

        replyQueueName = channel.queueDeclare().getQueue();
        consumer = new QueueingConsumer(channel);
        channel.basicConsume(replyQueueName, true, consumer);
    }

    public String call(String message) throws Exception {
        String response;
        String corrId = UUID.randomUUID().toString();

        BasicProperties props = new BasicProperties
                                    .Builder()
                                    .correlationId(corrId)
                                    .replyTo(replyQueueName)
                                    .build();

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

    @Override
    public void close() throws Exception {
        if (channel != null) {
            try {
            	channel.close();
            } catch (Exception e) {
              // ignore
            }
        }
    }

}
