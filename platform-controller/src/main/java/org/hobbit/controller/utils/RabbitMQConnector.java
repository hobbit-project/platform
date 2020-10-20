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
package org.hobbit.controller.utils;

import org.hobbit.controller.PlatformController;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.io.Charsets;
import org.hobbit.core.components.AbstractCommandReceivingComponent;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * This class connects the controller to the experiment's command queue.
 */
public class RabbitMQConnector extends AbstractCommandReceivingComponent {
    /**
     * The controller this connector belongs to.
     */
    private PlatformController controller;

    /**
     * Constructor needed for testing.
     */
    public RabbitMQConnector(PlatformController controller, String rabbitMQHostName) {
        super();
        this.controller = controller;
        this.rabbitMQHostName = rabbitMQHostName;
    }

    @Override
    public void run() throws Exception {
        throw new IllegalStateException();
    }

    /**
     * The controller overrides the super method because it does not need to check
     * for the leading hobbit id and delegates the command handling to the
     * {@link #receiveCommand(byte, byte[], String, String)} method.
     */
    @Override
    protected void handleCmd(byte bytes[], AMQP.BasicProperties props) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int idLength = buffer.getInt();
        byte sessionIdBytes[] = new byte[idLength];
        buffer.get(sessionIdBytes);
        String sessionId = new String(sessionIdBytes, Charsets.UTF_8);
        byte command = buffer.get();
        byte remainingData[];
        if (buffer.remaining() > 0) {
            remainingData = new byte[buffer.remaining()];
            buffer.get(remainingData);
        } else {
            remainingData = new byte[0];
        }
        controller.receiveCommand(command, remainingData, sessionId, props);
    }

    public void basicPublish(String exchange, String routingKey, BasicProperties props, byte[] body) throws IOException {
        cmdChannel.basicPublish(exchange, routingKey, props, body);
    }

    @Override
    public String toString() {
        return String.format("{rabbitMQHostName=%s}", this.rabbitMQHostName);
    }

    ///// There are some methods that shouldn't be used by the controller and
    ///// have been marked as deprecated

    /**
     * @deprecated Not used inside the controller. Use
     *             {@link #receiveCommand(byte, byte[], String, String)} instead.
     */
    @Deprecated
    @Override
    public void receiveCommand(byte command, byte[] data) {
        throw new IllegalStateException();
    }
}
