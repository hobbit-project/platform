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

import org.hobbit.core.Constants;
import org.hobbit.core.FrontEndApiCommands;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.data.ConfiguredExperiment;
import org.hobbit.core.data.ControllerStatus;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

public class ControllerStatusRequest extends AbstractCommandReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerStatusRequest.class);

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
        LOGGER.info("Sending request...");
        BasicProperties props = new BasicProperties.Builder().deliveryMode(2)
                .replyTo(Constants.CONTROLLER_2_FRONT_END_QUEUE_NAME).build();
        frontEnd2Controller.basicPublish("", Constants.FRONT_END_2_CONTROLLER_QUEUE_NAME, props,
                new byte[] { FrontEndApiCommands.LIST_CURRENT_STATUS });
        LOGGER.info("Waiting for response...");
        QueueingConsumer.Delivery delivery = consumer.nextDelivery(REQUEST_TIMEOUT);
        if (delivery == null) {
            throw new IOException("Didn't got a response after \"" + REQUEST_TIMEOUT + "\" ms.");
        }
        // parse the response
        String response = RabbitMQUtils.readString(delivery.getBody());
        Gson gson = new Gson();
        ControllerStatus status = gson.fromJson(response, ControllerStatus.class);
        // print results
        StringBuilder builder = new StringBuilder();
        builder.append("Response:");
        builder.append(NEWLINE);
        builder.append("currentExperiment:");
        builder.append(NEWLINE);
        builder.append("\texperiment id: ");
        builder.append(status.currentExperimentId);
        builder.append(NEWLINE);
        builder.append("\tbenchmark URI: ");
        builder.append(status.currentBenchmarkUri);
        builder.append(NEWLINE);
        builder.append("\tbenchmark name: ");
        builder.append(status.currentBenchmarkName);
        builder.append(NEWLINE);
        builder.append("\tsystem Uri: ");
        builder.append(status.currentSystemUri);
        builder.append(NEWLINE);
        builder.append("\tstatus: ");
        builder.append(status.currentStatus);
        builder.append(NEWLINE);
        builder.append(NEWLINE);
        builder.append("queue:");
        if (status.queue != null) {
            for (ConfiguredExperiment exp : status.queue) {
                builder.append(NEWLINE);
                builder.append("\tbenchmark URI: ");
                builder.append(NEWLINE);
                builder.append(exp.benchmarkUri);
                builder.append("\tsystem URI: ");
                builder.append(NEWLINE);
                builder.append(exp.systemUri);
                builder.append(NEWLINE);
            }
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
