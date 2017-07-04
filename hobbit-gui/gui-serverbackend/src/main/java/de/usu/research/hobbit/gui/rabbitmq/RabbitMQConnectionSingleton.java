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

public class RabbitMQConnectionSingleton {

    private static volatile RabbitMQConnection theConnection = null;

    public static RabbitMQConnection getConnection() throws Exception {
        if (theConnection == null) {
            synchronized (RabbitMQConnectionSingleton.class) {
                if (theConnection == null) {
                    theConnection = new RabbitMQConnection();
                    theConnection.open();
                }
            }
        }
        return theConnection;
    }

    public static void shutdown() {
        try {
            if (theConnection != null) {
                theConnection.close();
                theConnection = null;
            }
        } catch (Exception e) {
        }
    }
}
