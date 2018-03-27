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
package org.hobbit.controller.front;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.hobbit.controller.PlatformController;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.RabbitQueueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;

/**
 * This class implements a RabbitMQ {@link DefaultConsumer} and handles request
 * that are coming from the front end.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class FrontEndApiHandler extends DataReceiverImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontEndApiHandler.class);

    private static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 50;
    
    private final PlatformController controller;

    protected FrontEndApiHandler(RabbitQueue queue, PlatformController controller, int maxParallelProcessedMsgs)
            throws IOException {
        super(queue, null, maxParallelProcessedMsgs);
        this.controller = controller;
    }

    protected class MsgProcessingTask implements Runnable {

        private Delivery delivery;

        public MsgProcessingTask(Delivery delivery) {
            this.delivery = delivery;
        }

        @Override
        public void run() {
            try {
                byte[] body = delivery.getBody();
                BasicProperties properties = delivery.getProperties();
                if (body.length > 0) {
                    BasicProperties replyProperties;
                    replyProperties = new BasicProperties.Builder().correlationId(properties.getCorrelationId())
                            .deliveryMode(2).build();
                    controller.handleFrontEndCmd(body, properties.getReplyTo(), replyProperties);
                }
            } catch (Throwable e) {
                LOGGER.error("Uncatched throwable inside the data handler.", e);
            }
        }

    }

    public static class Builder {

        protected static final String QUEUE_INFO_MISSING_ERROR = "There are neither a queue nor a queue name and a queue factory provided for the DataReceiver. Either a queue or a name and a factory to create a new queue are mandatory.";
        protected static final String CONTROLLER_MISSING_ERROR = "The necessary platform controller has not been provided.";

        protected PlatformController controller;
        protected RabbitQueue queue;
        protected String queueName;
        protected int maxParallelProcessedMsgs = DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES;
        protected RabbitQueueFactory factory;

        public Builder() {
        };

        /**
         * Sets the reference to the platform controller that is called if data is incoming.
         * 
         * @param controller
         *            the platform controller that is called if data is incoming
         * @return this builder instance
         */
        public Builder platformController(PlatformController controller) {
            this.controller = controller;
            return this;
        }

        /**
         * Sets the queue that is used to receive data.
         * 
         * @param queue
         *            the queue that is used to receive data
         * @return this builder instance
         */
        public Builder queue(RabbitQueue queue) {
            this.queue = queue;
            return this;
        }

        /**
         * Method for providing the necessary information to create a queue if it has
         * not been provided with the {@link #queue(RabbitQueue)} method. Note that this
         * information is not used if a queue has been provided.
         * 
         * @param factory
         *            the queue factory used to create a queue
         * @param queueName
         *            the name of the newly created queue
         * @return this builder instance
         */
        public Builder queue(RabbitQueueFactory factory, String queueName) {
            this.factory = factory;
            this.queueName = queueName;
            return this;
        }

        /**
         * Sets the maximum number of incoming messages that are processed in parallel.
         * Additional messages have to wait in the queue.
         * 
         * @param maxParallelProcessedMsgs
         *            the maximum number of incoming messages that are processed in
         *            parallel
         * @return this builder instance
         */
        public Builder maxParallelProcessedMsgs(int maxParallelProcessedMsgs) {
            this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
            return this;
        }

        /**
         * Builds the {@link DataReceiverImpl} instance with the previously given
         * information.
         * 
         * @return The newly created DataReceiver instance
         * @throws IllegalStateException
         *             if the dataHandler is missing or if neither a queue nor the
         *             information needed to create a queue have been provided.
         * @throws IOException
         *             if an exception is thrown while creating a new queue or if the
         *             given queue can not be configured by the newly created
         *             DataReceiver. <b>Note</b> that in the latter case the queue will
         *             be closed.
         */
        public FrontEndApiHandler build() throws IllegalStateException, IOException {
            if (controller == null) {
                throw new IllegalStateException(CONTROLLER_MISSING_ERROR);
            }
            if (queue == null) {
                if ((queueName == null) || (factory == null)) {
                    throw new IllegalStateException(QUEUE_INFO_MISSING_ERROR);
                } else {
                    // create a new queue
                    queue = factory.createDefaultRabbitQueue(queueName);
                }
            }
            try {
                return new FrontEndApiHandler(queue, controller, maxParallelProcessedMsgs);
            } catch (IOException e) {
                IOUtils.closeQuietly(queue);
                throw e;
            }
        }
    }
}
