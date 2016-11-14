package org.hobbit.controller.test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.hobbit.core.FrontEndApiCommands;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

public class RequestBenchmarkDetails extends AbstractCommandReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestBenchmarkDetails.class);

    public static final String BENCHMARK_URI_KEY = "BENCHMARK";

    private static final long REQUEST_TIMEOUT = 60000;
    
    private static final String NEWLINE = String.format("%n") + String.format("%n");

    protected Channel frontEnd2Controller;
    protected QueueingConsumer consumer;

    @Override
    public void init() throws Exception {
        super.init();

        frontEnd2Controller = connection.createChannel();
        frontEnd2Controller.queueDeclare(Constants.FRONT_END_2_CONTROLLER_QUEUE_NAME, false, false, true, null);

        frontEnd2Controller.queueDeclare(Constants.CONTROLLER_2_FRONT_END_QUEUE_NAME, false, false, true, null);

        consumer = new QueueingConsumer(frontEnd2Controller);
        frontEnd2Controller.basicConsume(Constants.CONTROLLER_2_FRONT_END_QUEUE_NAME, true, consumer);
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // nothing to do
    }

    @Override
    public void run() throws Exception {
        Map<String, String> env = System.getenv();
        String benchmarkUri = env.getOrDefault(BENCHMARK_URI_KEY, null);
        if (benchmarkUri == null) {
            LOGGER.error("Couldn't get value of " + BENCHMARK_URI_KEY + ". Aborting.");
            throw new Exception("Couldn't get value of " + BENCHMARK_URI_KEY + ". Aborting.");
        }
        LOGGER.info("Sending request...");
        BasicProperties props = new BasicProperties.Builder().deliveryMode(2)
                .replyTo(Constants.CONTROLLER_2_FRONT_END_QUEUE_NAME).build();
        frontEnd2Controller.basicPublish("", Constants.FRONT_END_2_CONTROLLER_QUEUE_NAME, props,
                RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.GET_BENCHMARK_DETAILS },
                        new byte[][] { RabbitMQUtils.writeString(benchmarkUri) }, null));
        LOGGER.info("Waiting for response...");
        QueueingConsumer.Delivery delivery = consumer.nextDelivery(REQUEST_TIMEOUT);
        if (delivery == null) {
            throw new IOException("Didn't got a response after \"" + REQUEST_TIMEOUT + "\" ms.");
        }
        // parse the response
        if(delivery.getBody().length == 0) {
            LOGGER.error("Got an empty response.");
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(delivery.getBody());
        Model benchmarkModel = RabbitMQUtils.readModel(buffer);
        String jsonString = RabbitMQUtils.readString(buffer);
        Gson gson = new Gson();
        Collection<SystemMetaData> systems = gson.fromJson(jsonString, new TypeToken<Collection<SystemMetaData>>() {
        }.getType());
        // print results
        StringWriter writer = new StringWriter();
        benchmarkModel.write(writer, "TTL");
        StringBuilder builder = new StringBuilder();
        builder.append("Response:");
        builder.append(NEWLINE);
        builder.append("benchmarkModel:");
        builder.append(NEWLINE);
        builder.append(writer.toString());
        builder.append(NEWLINE);
        builder.append(NEWLINE);
        builder.append("systems:");
        for (SystemMetaData system : systems) {
            builder.append(NEWLINE);
            builder.append("\tname: ");
            builder.append(system.systemName);
            builder.append(NEWLINE);
            builder.append("\turi: ");
            builder.append(system.systemUri);
            builder.append(NEWLINE);
            builder.append("\timage: ");
            builder.append(system.system_image_name);
            builder.append(NEWLINE);
            builder.append("\tdescription: ");
            builder.append(system.systemDescription);
            builder.append(NEWLINE);
        }
        LOGGER.info(builder.toString());
    }

    @Override
    public void close() throws IOException {
        if (frontEnd2Controller != null) {
            try {
                frontEnd2Controller.close();
            } catch (Exception e) {
            }
        }
        super.close();
    }

}
