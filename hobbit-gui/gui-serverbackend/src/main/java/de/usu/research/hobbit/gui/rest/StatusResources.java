package de.usu.research.hobbit.gui.rest;

import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hobbit.core.data.ConfiguredExperiment;
import org.hobbit.core.data.ControllerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.GUIBackendException;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;

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