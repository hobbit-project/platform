/**
 * This file is part of platform-controller.
 *
 * platform-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * platform-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with platform-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.controller.test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.hobbit.core.FrontEndApiCommands;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.core.rabbit.QueueingConsumer;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

public class RequestBenchmarkDetails extends AbstractCommandReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestBenchmarkDetails.class);

    public static final String BENCHMARK_URI_KEY = "BENCHMARK";
    public static final String USER_NAME_KEY = "USERNAME";

    private static final long REQUEST_TIMEOUT = 60000;

    private static final String NEWLINE = String.format("%n") + String.format("%n");

    protected Channel frontEnd2Controller;
    protected QueueingConsumer consumer;

    @Override
    public void init() throws Exception {
        super.init();

        frontEnd2Controller = outgoingDataQueuefactory.getConnection().createChannel();
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
    String userName =  env.getOrDefault(USER_NAME_KEY, null);
        LOGGER.info("Sending request...");
        BasicProperties props = new BasicProperties.Builder().deliveryMode(2)
                .replyTo(Constants.CONTROLLER_2_FRONT_END_QUEUE_NAME).build();
        frontEnd2Controller.basicPublish("", Constants.FRONT_END_2_CONTROLLER_QUEUE_NAME, props,
                RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.GET_BENCHMARK_DETAILS },
                        new byte[][] { RabbitMQUtils.writeString(benchmarkUri), RabbitMQUtils.writeString(userName) }, null));
        LOGGER.info("Waiting for response...");
        Delivery delivery = consumer.getDeliveryQueue().poll(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);
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
            builder.append(system.name);
            builder.append(NEWLINE);
            builder.append("\turi: ");
            builder.append(system.uri);
            builder.append(NEWLINE);
            builder.append("\timage: ");
            builder.append(system.mainImage);
            builder.append(NEWLINE);
            builder.append("\tdescription: ");
            builder.append(system.description);
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
