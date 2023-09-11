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
import java.util.Map;

import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.components.utils.SystemResourceUsageRequester;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.hobbit.core.rabbit.QueueingConsumer;
import org.hobbit.core.run.ComponentStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestSystemResources extends AbstractPlatformConnectorComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestSystemResources.class);

    private static String cmdLineHobbitId = null;

    protected SystemResourceUsageRequester resUsageRequester;
    protected QueueingConsumer consumer;

    @Override
    public void init() throws Exception {
        super.init();

        Map<String, String> env = System.getenv();
        String sessionId = env.getOrDefault(Constants.HOBBIT_SESSION_ID_KEY, null);
        if (sessionId == null) {
            sessionId = cmdLineHobbitId;
            if (sessionId == null) {
                LOGGER.error("Couldn't get value of " + Constants.HOBBIT_SESSION_ID_KEY + ". Aborting.");
                throw new Exception("Couldn't get value of " + Constants.HOBBIT_SESSION_ID_KEY + ". Aborting.");
            }
        }
        resUsageRequester = SystemResourceUsageRequester.create(this, sessionId);
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // nothing to do
    }

    @Override
    public void run() throws Exception {
        LOGGER.info("Starting request...");
        ResourceUsageInformation info = resUsageRequester.getSystemResourceUsage();
        if (info != null) {
            LOGGER.info(info.toString());
        } else {
            LOGGER.warn("got null as response");
        }
    }

    @Override
    public void close() throws IOException {
        if (resUsageRequester != null) {
            resUsageRequester.close();
        }
        super.close();
    }

    public static void main(String[] args) {
        if (args.length >= 1) {
            cmdLineHobbitId = args[0];
            System.out.println("found hobbit id on cmd line: " + cmdLineHobbitId);
        }
        ComponentStarter.main(new String[] { RequestSystemResources.class.getCanonicalName() });
    }

}
