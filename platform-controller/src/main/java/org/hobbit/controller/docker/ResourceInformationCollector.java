package org.hobbit.controller.docker;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hobbit.core.Constants;
import org.hobbit.core.data.usage.CpuStats;
import org.hobbit.core.data.usage.DiskStats;
import org.hobbit.core.data.usage.MemoryStats;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerStats;

/**
 * A class that can collect resource usage information for the containers known
 * by the given {@link ContainerManager} using the given {@link DockerClient}.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class ResourceInformationCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceInformationCollector.class);

    private DockerClient client;
    private ContainerManager manager;

    public ResourceInformationCollector(DockerClient client, ContainerManager manager) {
        this.client = client;
        this.manager = manager;
    }

    public ResourceUsageInformation getSystemUsageInformation() {
        return getUsageInformation(c -> (c.labels().containsValue(ContainerManager.LABEL_TYPE))
                && (c.labels().get(ContainerManager.LABEL_TYPE).equals(Constants.CONTAINER_TYPE_SYSTEM)));
    }

    public ResourceUsageInformation getUsageInformation(Predicate<? super Container> containerFilter) {
        List<Container> containers = manager.getContainers();

        Set<String> systemContainerIds = containers.parallelStream()
                // filter the containers
                .filter(containerFilter)
                // get their IDs
                .map(c -> c.id()).collect(Collectors.toSet());
        ResourceUsageInformation resourceInfo = systemContainerIds.parallelStream()
                // get the stats for the single
                .map(id -> requestCpuAndMemoryStats(id))
                // sum up the stats
                .collect(Collectors.reducing(ResourceUsageInformation::staticMerge)).orElse(null);
        if (resourceInfo != null) {
            // Add disk usage information
            long diskUsage = containers.parallelStream().mapToLong(c -> c.sizeRootFs()).sum();
            resourceInfo.setDiskStats(new DiskStats(diskUsage));
        }
        return resourceInfo;
    }

    protected ResourceUsageInformation requestCpuAndMemoryStats(String containerId) {
        ContainerStats stats;
        try {
            stats = client.stats(containerId);
        } catch (Exception e) {
            LOGGER.warn("Error while requesting usagee stats for {}. Returning null. Error: {}", containerId,
                    e.getLocalizedMessage());
            return null;
        }
        ResourceUsageInformation resourceInfo = new ResourceUsageInformation();
        if ((stats.cpuStats() != null) && (stats.cpuStats().cpuUsage() != null)) {
            resourceInfo.setCpuStats(new CpuStats(stats.cpuStats().cpuUsage().totalUsage()));
        }
        if ((stats.memoryStats() != null) && (stats.cpuStats().cpuUsage() != null)) {
            resourceInfo.setMemoryStats(new MemoryStats(stats.memoryStats().usage()));
        }
        return resourceInfo;
    }

}
