package org.hobbit.controller.docker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hobbit.core.Constants;
import org.hobbit.core.data.usage.CpuStats;
import org.hobbit.core.data.usage.DiskStats;
import org.hobbit.core.data.usage.MemoryStats;
import org.hobbit.core.data.usage.ResourceUsageInformation;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListContainersParam;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerStats;
import com.spotify.docker.client.messages.swarm.TaskStatus;

/**
 * A class that can collect resource usage information for the containers known
 * by the given {@link ContainerManager} using the given {@link DockerClient}.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class ResourceInformationCollector {

    private ContainerManager manager;

    public ResourceInformationCollector(ContainerManager manager) {
        this.manager = manager;
    }

    public ResourceUsageInformation getSystemUsageInformation() {
        return getUsageInformation(DockerClient.ListContainersParam.withLabel(ContainerManager.LABEL_TYPE,
                Constants.CONTAINER_TYPE_SYSTEM));
    }

    public ResourceUsageInformation getUsageInformation(ListContainersParam... params) {
        params = Arrays.copyOf(params, params.length + 1);
        params[params.length - 1] = ListContainersParam.withContainerSizes(true);
        List<Container> containers = manager.getContainers(params);

        Map<String, Container> containerMapping = new HashMap<>();
        for (Container c : containers) {
            containerMapping.put(c.id(), c);
        }
        ResourceUsageInformation resourceInfo = containerMapping.keySet().parallelStream()
                // filter all containers that are not running
                .filter(c -> TaskStatus.TASK_STATE_RUNNING.equals(containerMapping.get(c).state()))
                // get the stats for the single
                .map(id -> requestCpuAndMemoryStats(id))
                // sum up the stats
                .collect(Collectors.reducing(ResourceUsageInformation::staticMerge)).orElse(null);
        if (resourceInfo != null) {
            // Add disk usage information
            long diskUsage = containers.parallelStream().map(c -> c.sizeRootFs()).filter(s -> s != null)
                    .mapToLong(s -> s).sum();
            resourceInfo.setDiskStats(new DiskStats(diskUsage));
        }
        return resourceInfo;
    }

    protected ResourceUsageInformation requestCpuAndMemoryStats(String containerId) {
        ContainerStats stats = manager.getStats(containerId);
        ResourceUsageInformation resourceInfo = new ResourceUsageInformation();
        if (stats != null) {
            if ((stats.cpuStats() != null) && (stats.cpuStats().cpuUsage() != null)) {
                resourceInfo.setCpuStats(new CpuStats(stats.cpuStats().cpuUsage().totalUsage()));
            }
            if ((stats.memoryStats() != null) && (stats.cpuStats().cpuUsage() != null)) {
                resourceInfo.setMemoryStats(new MemoryStats(stats.memoryStats().usage()));
            }
        }
        return resourceInfo;
    }

}
