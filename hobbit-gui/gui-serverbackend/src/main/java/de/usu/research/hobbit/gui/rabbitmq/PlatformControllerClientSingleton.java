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
