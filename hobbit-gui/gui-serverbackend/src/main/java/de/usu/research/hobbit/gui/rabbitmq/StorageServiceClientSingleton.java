package de.usu.research.hobbit.gui.rabbitmq;

import org.hobbit.storage.client.StorageServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton implementation of the {@link StorageServiceClient} class.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class StorageServiceClientSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformControllerClientSingleton.class);

    private static StorageServiceClient instance = null;

    /**
     * Returns the instance of the {@link StorageServiceClient} or
     * <code>null</code> if it couldn't be instantiated.
     * 
     * @return the instance of the {@link StorageServiceClient} or
     *         <code>null</code>
     */
    public static synchronized StorageServiceClient getInstance() {
        if (instance == null) {
            try {
                instance = StorageServiceClient.create(RabbitMQConnectionSingleton.getConnection().getConnection());
            } catch (Exception e) {
                LOGGER.error("Exception while trying to create RabbitRpcClient. Returning null.", e);
            }
        }
        return instance;
    }

}
