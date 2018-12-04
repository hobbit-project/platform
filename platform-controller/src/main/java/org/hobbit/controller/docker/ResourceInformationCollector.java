package org.hobbit.controller.docker;

import org.hobbit.core.data.usage.ResourceUsageInformation;
import com.spotify.docker.client.messages.swarm.Task;

/**
 * A class that can collect resource usage information.
 *
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public interface ResourceInformationCollector {

    public ResourceUsageInformation getSystemUsageInformation();

    public ResourceUsageInformation getUsageInformation(Task.Criteria criteria);

}
