/**
 * This file is part of gui-serverbackend.
 *
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rabbitmq;

import java.io.IOException;

import org.hobbit.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Managing the connection to the HOBBIT RabbitMQ instance (partly based on
 * org.hobbit.controller.test.RequestBenchmarkDetails and
 * org.hobbit.controller.test.RequestBenchmarks)
 * 
 * @author Roman Korf
 *
 */
public class RabbitMQConnection implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformControllerClient.class);

    /**
     * Maximum number of retries that are executed to connect to RabbitMQ at
     * initialisation.
     */
    public static final int NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ = 5;

    /**
     * Time, the system waits before retrying to connect to RabbitMQ. Note that
     * this time will be multiplied with the number of already failed tries.
     */
    public static final long START_WAITING_TIME_BEFORE_RETRY = 5000;
    // private String hobbitSessionId;
    protected String rabbitMQHostName;
    protected Connection connection = null;

    /**
     * Initialize connection to HOBBIT RabbitMQ instance.
     * 
     * @throws GUIBackendException
     * @throws IOException
     */
    public void open() throws GUIBackendException, IOException {
        // hobbitSessionId = null;
        // if (System.getenv().containsKey(Constants.HOBBIT_SESSION_ID_KEY)) {
        // hobbitSessionId =
        // System.getenv().get(Constants.HOBBIT_SESSION_ID_KEY);
        // }
        // if (hobbitSessionId == null) {
        // hobbitSessionId =
        // Constants.HOBBIT_SESSION_ID_FOR_PLATFORM_COMPONENTS;
        // }

        // RABBIT_MQ_HOST_NAME_KEY = "HOBBIT_RABBIT_HOST"
        if (System.getenv().containsKey(Constants.RABBIT_MQ_HOST_NAME_KEY)) {
            ConnectionFactory factory = new ConnectionFactory();
            rabbitMQHostName = System.getenv().get(Constants.RABBIT_MQ_HOST_NAME_KEY);
            factory.setHost(rabbitMQHostName);
            for (int i = 0; (connection == null) && (i <= NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ); ++i) {
                try {
                    connection = factory.newConnection();
                } catch (Exception e) {
                    if (i < NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ) {
                        long waitingTime = START_WAITING_TIME_BEFORE_RETRY * (i + 1);
                        LOGGER.warn(
                                "Couldn't connect to RabbitMQ at " + rabbitMQHostName + " with try #" + i + ". Next try in " + waitingTime + "ms.",
                                e);
                        try {
                            Thread.sleep(waitingTime);
                        } catch (Exception e2) {
                            LOGGER.warn("Interrupted while waiting before retrying to connect to RabbitMQ.", e2);
                        }
                    }
                }
            }
            if (connection == null) {
                String msg = "Couldn't connect to RabbitMQ after " + NUMBER_OF_RETRIES_TO_CONNECT_TO_RABBIT_MQ
                        + " retries.";
                LOGGER.error(msg);
                throw new GUIBackendException(msg);
            }
        } else {
            String msg = "Couldn't get " + Constants.RABBIT_MQ_HOST_NAME_KEY
                    + " from the environment. This component won't be able to connect to RabbitMQ.";
            LOGGER.error(msg);
            throw new GUIBackendException(msg);
        }
    }

    /**
     * Don't forget to close the connection.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
            }
        }
    }

	public Channel createChannel() throws IOException {
		return connection.createChannel();
	}
	
	public Connection getConnection() {
        return connection;
    }
}
