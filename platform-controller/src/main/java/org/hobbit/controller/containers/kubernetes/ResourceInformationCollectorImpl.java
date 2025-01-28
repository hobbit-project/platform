package org.hobbit.controller.containers.kubernetes;
import com.spotify.docker.client.messages.swarm.Service;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.MetricsApi;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1PodMetrics;
import io.kubernetes.client.openapi.models.V1PodMetricsList;
import io.kubernetes.client.util.Config;
import org.hobbit.controller.containers.ResourceInformationCollector;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
public class ResourceInformationCollectorImpl implements ResourceInformationCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceInformationCollectorImpl.class);

    private final ApiClient apiClient;
    private final MetricsApi metricsApi;

    public ResourceInformationCollectorImpl() throws IOException {
        this.apiClient = Config.defaultClient();
        this.metricsApi = new MetricsApi(apiClient);
    }

    @Override
    public ResourceUsageInformation getSystemUsageInformation() {
        return getUsageInformation(null);
    }

    @Override
    public ResourceUsageInformation getUsageInformation(Service.Criteria criteria) {
        return null;
    }

    @Override
    public ResourceUsageInformation getUsageInformation( criteria) {
        try {
            V1PodMetricsList podMetricsList = metricsApi.listPodMetricsForAllNamespaces(
                null, null, null, null, null, null, null, null, null);

            List<V1PodMetrics> filteredMetrics = podMetricsList.getItems();

            // If criteria exist, filter pods accordingly (example: by namespace or labels)
            if (criteria != null) {
                filteredMetrics = filteredMetrics.stream()
                    .filter(metrics -> matchesCriteria(metrics, criteria))
                    .collect(Collectors.toList());
            }

            return aggregatePodMetrics(filteredMetrics);

        } catch (ApiException e) {
            LOGGER.error("Error retrieving pod metrics from Kubernetes", e);
            return null;
        }
    }

    @Override
    public SetupHardwareInformation getHardwareInformation() {
        SetupHardwareInformation hardwareInfo = new SetupHardwareInformation();
        try {
            V1NodeList nodeList = new MetricsApi(apiClient).listNode(null, null, null, null, null, null, null, null, null);

            for (V1Node node : nodeList.getItems()) {
                NodeHardwareInformation nodeInfo = new NodeHardwareInformation();
                nodeInfo.setInstance(node.getMetadata().getName());
                nodeInfo.setCpu(
                    Long.parseLong(node.getStatus().getCapacity().get("cpu").toString()),
                    Collections.singletonList(Long.parseLong(node.getStatus().getCapacity().get("cpu").toString()))
                );
                nodeInfo.setMemory(
                    Long.parseLong(node.getStatus().getCapacity().get("memory").toString().replaceAll("[^\\d]", "")),
                    null
                );
                nodeInfo.setOs(node.getStatus().getNodeInfo().getOsImage());
                hardwareInfo.addNode(nodeInfo);
            }

        } catch (ApiException e) {
            LOGGER.error("Error retrieving hardware information from Kubernetes nodes", e);
        }

        return hardwareInfo;
    }

    private boolean matchesCriteria(V1PodMetrics metrics, Criteria criteria) {
        // Example implementation: filter by namespace
        if (criteria.getNamespace() != null && !criteria.getNamespace().equals(metrics.getMetadata().getNamespace())) {
            return false;
        }
        // Add additional criteria filtering logic if necessary
        return true;
    }

    private ResourceUsageInformation aggregatePodMetrics(List<V1PodMetrics> podMetrics) {
        ResourceUsageInformation resourceInfo = new ResourceUsageInformation();
        podMetrics.forEach(metrics -> {
            metrics.getContainers().forEach(container -> {
                try {
                    long memoryUsage = Long.parseLong(
                        container.getUsage().get("memory").replaceAll("[^\\d]", ""));
                    long cpuUsage = Long.parseLong(
                        container.getUsage().get("cpu").replaceAll("[^\\d]", ""));

                    resourceInfo.addMemoryUsage(memoryUsage);
                    resourceInfo.addCpuUsage(cpuUsage);

                } catch (NumberFormatException e) {
                    LOGGER.warn("Error parsing resource usage for container: {}", container.getName(), e);
                }
            });
        });

        return resourceInfo;
    }
}
