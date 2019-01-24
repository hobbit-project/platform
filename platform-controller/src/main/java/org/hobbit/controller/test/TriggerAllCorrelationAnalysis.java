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

import org.hobbit.vocab.HOBBIT;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.hobbit.storage.client.StorageServiceClient;

import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.rabbit.DataSender;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.run.ComponentStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.QueueingConsumer;

public class TriggerAllCorrelationAnalysis extends AbstractPlatformConnectorComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerAllCorrelationAnalysis.class);

    protected DataSender sender2Analysis;
    protected StorageServiceClient storage;
    protected QueueingConsumer consumer;

    @Override
    public void init() throws Exception {
        super.init();

        sender2Analysis = DataSenderImpl.builder()
                .queue(outgoingDataQueuefactory, Constants.CONTROLLER_2_ANALYSIS_QUEUE_NAME).build();

        storage = StorageServiceClient.create(outgoingDataQueuefactory.getConnection());
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // nothing to do
    }

    @Override
    public void run() throws Exception {
        LOGGER.info("Starting request...");
        String query =
        "PREFIX hobbit: <" + HOBBIT.getURI() + ">\n"
        + "SELECT DISTINCT ?benchmark ?systemInstance WHERE {\n"
        + "GRAPH <" + Constants.PUBLIC_RESULT_GRAPH_URI + "> {\n"
        + "?benchmark a hobbit:Benchmark .\n"
        + "?systemInstance a hobbit:SystemInstance .\n"
        + "?experiment hobbit:involvesBenchmark ?benchmark .\n"
        + "?experiment hobbit:involvesSystemInstance ?systemInstance .\n"
        + "}}";
        ResultSet results = storage.sendSelectQuery(query);
        while (results.hasNext()) {
            QuerySolution solution = results.next();
            String benchmark = solution.getResource("benchmark").getURI();
            String systemInstance = solution.getResource("systemInstance").getURI();
            LOGGER.info("Benchmark: {}\nSystem instance: {}", benchmark, systemInstance);
            String experimentQuery =
            "PREFIX hobbit: <" + HOBBIT.getURI() + ">\n"
            + "SELECT ?experiment WHERE {\n"
            + "GRAPH <" + Constants.PUBLIC_RESULT_GRAPH_URI + "> {\n"
            + "?experiment hobbit:involvesBenchmark <" + benchmark + "> .\n"
            + "?experiment hobbit:involvesSystemInstance <" + systemInstance + "> .\n"
            + "}} ORDER BY DESC(?experiment) LIMIT 1";

            ResultSet experiments = storage.sendSelectQuery(experimentQuery);
            String experiment = experiments.next().getResource("experiment").getURI();
            LOGGER.info("Experiment: {}", experiment);

            sender2Analysis.sendData(RabbitMQUtils.writeString(experiment));
        }
    }

    public static void main(String[] args) {
        ComponentStarter.main(new String[] { TriggerAllCorrelationAnalysis.class.getCanonicalName() });
    }

}
