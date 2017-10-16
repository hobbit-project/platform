/**
 * This file is part of gui-serverbackend.
 * <p>
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rest;

import de.usu.research.hobbit.gui.rabbitmq.GUIBackendException;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;
import org.hobbit.core.data.ConfiguredExperiment;
import org.hobbit.core.data.ControllerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Objects;

@Path("status")
public class StatusResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusResources.class);

    private static final String NEWLINE = String.format("%n");

    @GET
    public String getStatus() throws Exception {
        LOGGER.info("Get status ...");
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        if (client == null) {
            throw new GUIBackendException("Couldn't connect to platform controller.");
        }
        ControllerStatus status = client.requestStatus();
        Objects.requireNonNull(status, "Couldn't get status of platform.");

        StringBuilder builder = new StringBuilder();
        builder.append("Current experiment:");
        builder.append(NEWLINE);
        // If there is no experiment
        if (status.currentExperimentId == null) {
            builder.append("    No experiment is executed at the moment.");
            builder.append(NEWLINE);
        } else {
            builder.append("    Id:        ");
            builder.append(status.currentExperimentId);
            builder.append(NEWLINE);
            builder.append("    Benchmark: ");
            builder.append(status.currentBenchmarkUri);
            builder.append(NEWLINE);
            if ((status.currentBenchmarkName != null) && (!status.currentBenchmarkName.isEmpty())) {
                builder.append("               ");
                builder.append(status.currentBenchmarkName);
                builder.append(NEWLINE);
            }
            builder.append("    System:    ");
            builder.append(status.currentSystemUri);
            builder.append(NEWLINE);
            if ((status.currentSystemName != null) && (!status.currentSystemName.isEmpty())) {
                builder.append("               ");
                builder.append(status.currentSystemName);
                builder.append(NEWLINE);
            }
            builder.append("    status:    ");
            builder.append(status.currentStatus);
            builder.append(NEWLINE);
        }
        builder.append(NEWLINE);
        builder.append("Experiment queue:");
        builder.append(NEWLINE);
        // If the queue is empty
        if ((status.queue == null) || (status.queue.length == 0)) {
            builder.append("    There is no experiment in the queue.");
            builder.append(NEWLINE);
        } else {
            ConfiguredExperiment queuedExp;
            for (int i = 0; i < status.queue.length; ++i) {
                queuedExp = status.queue[i];
                builder.append("    Benchmark:   ");
                builder.append(queuedExp.benchmarkUri);
                builder.append(NEWLINE);
                builder.append("    System:      ");
                builder.append(queuedExp.systemUri);
                builder.append(NEWLINE);
                if ((queuedExp.challengeUri != null) && (!queuedExp.challengeUri.isEmpty())) {
                    builder.append("    part of:     ");
                    builder.append(queuedExp.challengeUri);
                    builder.append(NEWLINE);
                }
                if ((queuedExp.executionDate != null) && (!queuedExp.executionDate.isEmpty())) {
                    builder.append("    planned for: ");
                    builder.append(queuedExp.executionDate);
                    builder.append(NEWLINE);
                }
                builder.append(NEWLINE);
            }
        }
        return builder.toString();
    }

}
