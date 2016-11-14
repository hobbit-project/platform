package org.hobbit.controller.front;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * This class implements a RabbitMQ {@link DefaultConsumer} and handles request
 * that are coming from the front end.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class FrontEndApiHandler extends DefaultConsumer {

	public FrontEndApiHandler(Channel channel) {
		super(channel);
	}

	@Override
	public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
			throws IOException {
		// TODO handle request and send response
	}
}
