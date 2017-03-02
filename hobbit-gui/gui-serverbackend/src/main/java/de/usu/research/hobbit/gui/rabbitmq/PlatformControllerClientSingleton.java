package de.usu.research.hobbit.gui.rabbitmq;

import org.hobbit.core.rabbit.RabbitRpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton implementation of the {@link PlatformControllerClient} class.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class PlatformControllerClientSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformControllerClientSingleton.class);

    private static PlatformControllerClient instance = null;

    /**
     * Returns the instance of the {@link RabbitRpcClient} or <code>null</code>
     * if it couldn't be instantiated.
     * 
     * @return the instance of the {@link RabbitRpcClient} or <code>null</code>
     */
    public static synchronized PlatformControllerClient getInstance() {
        if (instance == null) {
            try {
                instance = PlatformControllerClient.create(RabbitMQConnectionSingleton.getConnection().getConnection());
            } catch (Exception e) {
                LOGGER.error("Exception while trying to create RabbitRpcClient. Returning null.", e);
            }
        }
        return instance;
    }

}
