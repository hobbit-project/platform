package org.hobbit.controller.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.exceptions.ImagePullFailedException;
import com.spotify.docker.client.messages.ProgressMessage;

/**
 * This class handles the logging of the progress while pulling a Docker image.
 * It is mainly a copy of {@link com.spotify.docker.client.LoggingPullHandler}
 * but produces much less logging messages.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class LoggingPullHandler implements ProgressHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPullHandler.class);

    private static final String SWARM_PULLING_STARTED_STATUS = "Pulling ";
    private static final String SWARM_PULLING_STOPPED_STATUS = ": downloaded";
    private static final String SINGLE_DOWNLOADING_STATUS = "Downloading";
    private static final String SINGLE_IMAGE_UP_TO_DATE_STATUS = "Status: Image is up to date";
    private static final String SINGLE_PULLING_COMPLETE_STATUS = "Status: Downloaded newer image";

    private final String image;
    private boolean pullingStartedLogged = false;
    private int pullStartCount = 0;
    private int pullEndCount = 0;

    public LoggingPullHandler(String image) {
        this.image = image;
    }

    @Override
    public void progress(ProgressMessage message) throws DockerException {
        // Error handling (taken from
        // com.spotify.docker.client.LoggingPullHandler)
        if (message.error() != null) {
            if (message.error().contains("404") || message.error().contains("not found")) {
                throw new ImageNotFoundException(image, message.toString());
            } else {
                throw new ImagePullFailedException(image, message.toString());
            }
        }

        processProgress(message);
    }

    protected synchronized void processProgress(ProgressMessage message) {
        if (message.status().startsWith(SWARM_PULLING_STARTED_STATUS)) {
            ++pullStartCount;
            if (pullStartCount == 2) {
                LOGGER.info("Pulling image {}", image);
                pullingStartedLogged = true;
            }
            return;
        }

        if (!pullingStartedLogged) {
            if (message.status().startsWith(SINGLE_IMAGE_UP_TO_DATE_STATUS)) {
                LOGGER.info("Image {} is up to date und does not have to be pulled.", image);
            } else if (message.status().startsWith(SINGLE_DOWNLOADING_STATUS)) {
                LOGGER.info("Image {} is being pulled...", image);
                pullingStartedLogged = true;
            }
        } else {
            if (message.status().startsWith(SINGLE_PULLING_COMPLETE_STATUS)) {
                LOGGER.info("Pulled image {}.", image);
            } else if ((pullStartCount > 0) && (message.status().endsWith(SWARM_PULLING_STOPPED_STATUS))) {
                ++pullEndCount;
                if (pullStartCount == pullEndCount) {
                    LOGGER.info("Pulled image {}", image);
                }
            }
        }
    }
}
