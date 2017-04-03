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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hobbit.core.Constants;
import org.hobbit.core.FrontEndApiCommands;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

public class StartBenchmarkRequest extends AbstractCommandReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartBenchmarkRequest.class);

    public static final String BENCHMARK_URI_KEY = "BENCHMARK";
    public static final String SYSTEM_URI_KEY = "SYSTEM";
    public static final String BENCHMARK_PARAM_FILE_KEY = "BENCHMARK_PARAM_FILE";
    
    private static final long REQUEST_TIMEOUT = 60000;

    protected Channel frontEnd2Controller;
    protected QueueingConsumer consumer;

    @Override
    public void init() throws Exception {
        super.init();

        frontEnd2Controller = dataConnection.createChannel();
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
        String systemUri = env.getOrDefault(SYSTEM_URI_KEY, null);
        if (systemUri == null) {
            LOGGER.error("Couldn't get value of " + SYSTEM_URI_KEY + ". Aborting.");
            throw new Exception("Couldn't get value of " + SYSTEM_URI_KEY + ". Aborting.");
        }
        String benchmarkModelFile = env.getOrDefault(BENCHMARK_PARAM_FILE_KEY, null);
        if (benchmarkModelFile == null) {
            LOGGER.error("Couldn't get value of " + BENCHMARK_PARAM_FILE_KEY + ". Aborting.");
            throw new Exception("Couldn't get value of " + BENCHMARK_PARAM_FILE_KEY + ". Aborting.");
        }
        LOGGER.info("Reading model from " + benchmarkModelFile + ".");
        Model model = readModel(benchmarkModelFile);
        byte[] data = RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.ADD_EXPERIMENT_CONFIGURATION },
                new byte[][] { RabbitMQUtils.writeString(benchmarkUri), RabbitMQUtils.writeString(systemUri),
                        RabbitMQUtils.writeModel(model) }, null);

        LOGGER.info("Sending request...");
        BasicProperties props = new BasicProperties.Builder().deliveryMode(2)
                .replyTo(Constants.CONTROLLER_2_FRONT_END_QUEUE_NAME).build();
        frontEnd2Controller.basicPublish("", Constants.FRONT_END_2_CONTROLLER_QUEUE_NAME, props, data);
        LOGGER.info("Waiting for response...");
        QueueingConsumer.Delivery delivery = consumer.nextDelivery(REQUEST_TIMEOUT);
        if (delivery == null) {
            throw new IOException(
                    "Didn't got a response after \"" + REQUEST_TIMEOUT + "\" ms.");
        }
        // parse the response
        LOGGER.info("Response: " + RabbitMQUtils.readString(delivery.getBody()));
    }

    private static Model readModel(String benchmarkModelFile) {
        Model model = ModelFactory.createDefaultModel();
        InputStream in = null;
        try {
            in = new FileInputStream(benchmarkModelFile);
            model.read(in, null, "TTL");
        } catch (IOException e) {
            LOGGER.error("Error while reading model.", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return model;
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
