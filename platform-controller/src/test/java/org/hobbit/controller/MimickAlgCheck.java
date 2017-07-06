package org.hobbit.controller;

import java.io.File;
import java.io.IOException;

import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.mimic.DockerBasedMimickingAlg;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small check of a mimicking algorithm ot make sure that it sends data as
 * expected. Usage: 1. pull the image (and all images it needs); 2. start a
 * local platform (at least the platform controller with
 * CONTAINER_PARENT_CHECK=0, Redis and Rabbit with Rabbits ports forwarded); 3.
 * set the static variables in this class to the correct values; 4. start the
 * main method with the following environmental variables:
 * {@link Constants#CONTAINER_NAME_KEY} and
 * {@link Constants#RABBIT_MQ_HOST_NAME_KEY}.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
@Ignore
public class MimickAlgCheck extends AbstractPlatformConnectorComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(MimickAlgCheck.class);

    private static final String DOCKER_IMAGE = "git.project-hobbit.eu:4567/filipe.teixeira/synthetic-trace-generator";
    private static final String MIMICKING_ALG_ENV_VARS[] = { "hobbit.numtraces=10", "hobbit.seed=3",
            "hobbit.outputformat=rdf" };
    private static final String OUTPUT_DIR = "test_output";
    private static final String RABBIT_HOST_NAME_IN_DOCKER_NETWORK = "rabbit";

    public MimickAlgCheck() {
        defaultContainerType = Constants.CONTAINER_TYPE_BENCHMARK;
    }

    public static void main(String[] args) {
        MimickAlgCheck check = new MimickAlgCheck();
        try {
            check.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            check.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            check.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // check received data
        File outputDir = new File(OUTPUT_DIR);
        if (outputDir.list() != null) {
            System.out.println("Received files:");
            for (String fileName : outputDir.list()) {
                System.out.println(fileName);
            }
        } else {
            System.err.println("The output directory is not available.");
        }
    }

    @Override
    public void run() throws Exception {
        // After this component has been initialized and is connected to rabbit,
        // we need to set the rabbit host name to the value that this container
        // would have if it would be running as Docker container inside the
        // hobbit network.
        this.rabbitMQHostName = RABBIT_HOST_NAME_IN_DOCKER_NETWORK;
        DockerBasedMimickingAlg alg = new DockerBasedMimickingAlg(this, DOCKER_IMAGE);
        alg.generateData(OUTPUT_DIR, MIMICKING_ALG_ENV_VARS);
    }

    public void receiveCommand(byte command, byte[] data, String sessionId, String replyTo) {
        LOGGER.info("received command: session={}, command={}, data={}", sessionId, Commands.toString(command),
                data != null ? RabbitMQUtils.readString(data) : "null");
        super.receiveCommand(command, data);
    }
}
