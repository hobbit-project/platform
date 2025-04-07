package org.hobbit.controller.containers.kubernetes;


import com.google.common.collect.ImmutableMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeSystemInfo;
import io.kubernetes.client.openapi.models.V1Pod;
import org.hobbit.controller.containers.ContainerManager;
import org.hobbit.controller.containers.ResourceInformationCollector;
import org.hobbit.controller.data.ContainerCriteria;
import org.hobbit.controller.data.NodeHardwareInformation;
import org.hobbit.controller.data.SetupHardwareInformation;
import org.hobbit.core.Constants;
import org.hobbit.core.data.usage.CpuStats;
import org.hobbit.core.data.usage.DiskStats;
import org.hobbit.core.data.usage.MemoryStats;
import org.hobbit.core.data.usage.ResourceUsageInformation;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class implements the {@link ResourceInformationCollector} interface to gather resource usage information and hardware details from a Kubernetes cluster. It uses the Kubernetes Java client to interact with the Kubernetes API.
 *
 * The implementation provides methods to:
 *     Get system-wide resource usage information.
 *     Get resource usage information for specific containers based on criteria.
 *     Fetch hardware information of the nodes in the Kubernetes cluster.
 *
 * @author Farshad Afshari farshad.afshari@uni-paderborn.de
 */

public class ResourceInformationCollectorImpl implements ResourceInformationCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceInformationCollectorImpl.class);

    //TODO make configable
    private int TIMEOUT_MILLISECONDS = 60000;
    private final String namespace = "default";

    private ApiClient apiClient;
    private CoreV1Api coreV1Api;
    private ContainerManager manager;

    public ResourceInformationCollectorImpl(ContainerManager manager,ApiClient apiClient, CoreV1Api coreV1Api) {
        this.manager = manager;
        this.apiClient = apiClient;
        this.apiClient.setConnectTimeout(TIMEOUT_MILLISECONDS);
        this.apiClient.setReadTimeout(TIMEOUT_MILLISECONDS);
        this.apiClient.setWriteTimeout(TIMEOUT_MILLISECONDS);
        Configuration.setDefaultApiClient(this.apiClient);

        // Create a CoreV1Api instance
        this.coreV1Api = new CoreV1Api(this.apiClient);
    }

    @Override
    public ResourceUsageInformation getSystemUsageInformation() {
        return getUsageInformation(ContainerCriteria.builder()
            .withLabels(ImmutableMap.of(ContainerManager.LABEL_TYPE, Constants.CONTAINER_TYPE_SYSTEM))
            .build());
    }

    @Override
    public ResourceUsageInformation getUsageInformation(ContainerCriteria criteria) {
        List<String> servicesName = manager.getContainers(criteria);

        LOGGER.info("number of services which has thecriteri {} is: {}", criteria.toString(),servicesName.size());
        ResourceUsageInformation resourceInfo = servicesName.parallelStream()
            .filter(this::isPodRunning) // Use a method to check if the service is running
            .map(this::requestCpuAndMemoryStats)
            .collect(Collectors.reducing(ResourceUsageInformation::staticMerge)).orElse(null);

        return resourceInfo;
    }

    protected ResourceUsageInformation requestCpuAndMemoryStats(String podName) {
        ResourceUsageInformation resourceUsage = fetchPodMetrics(this.apiClient, this.namespace, podName);

        return resourceUsage;
    }

    private boolean isPodRunning(String podName) {
        try {
            // Fetch the pod details by name
            V1Pod pod = this.coreV1Api.readNamespacedPod(podName, this.namespace, null);

            // Check the pod's status
            String status = pod.getStatus().getPhase();
            return "Running".equalsIgnoreCase(status);
        } catch (Exception e) {
            System.err.println("Error checking status for Pod " + podName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public SetupHardwareInformation getHardwareInformation() {
        SetupHardwareInformation information = new SetupHardwareInformation();
        try{
            List<V1Node> nodes = coreV1Api.listNode(
                null,   // pretty
                true,   // allowWatchBookmarks
                null,   // continue
                null,   // fieldSelector
                null,   // labelSelector (not setting any label filter)
                null,   // limit (no limit set, returns all results)
                null,   // resourceVersion
                null,   // resourceVersionMatch
                null,   // timeoutSeconds
                false   // watch
            ).getItems();

            // Iterate through each node
            for (V1Node node : nodes) {
                V1NodeSystemInfo systemInfo = node.getStatus().getNodeInfo();

                LOGGER.debug("Kernel Version: " + systemInfo.getKernelVersion());
                LOGGER.debug("Machine ID: " + systemInfo.getMachineID());
                LOGGER.debug("Container Runtime Version: " + systemInfo.getContainerRuntimeVersion());
                LOGGER.debug("Kubelet Version: " + systemInfo.getKubeletVersion());
                LOGGER.debug("KubeProxy Version: " + systemInfo.getKubeProxyVersion());

                Map<String, Quantity> capacity = node.getStatus().getCapacity();
//                Map<String, Quantity> allocatable = node.getStatus().getAllocatable();

                String cpu = capacity.get("cpu").toSuffixedString();  // e.g., "8"
                String memory = capacity.get("memory").toSuffixedString(); // e.g., "32Gi"


                NodeHardwareInformation nodeInfo = new NodeHardwareInformation();
                nodeInfo.setOs(systemInfo.getOsImage());
                nodeInfo.setCpu(Long.valueOf(cpu),new LinkedList<>());
                nodeInfo.setMemory(parseMemoryUsage(memory),0L);
                information.addNode(nodeInfo);
            }
        } catch (Exception e) {
            LOGGER.error("Error getting hardware information", e);
        }
        return information;
    }

    private static ResourceUsageInformation fetchPodMetrics(ApiClient client, String namespace, String podName) {
        try {
            // Metrics server endpoint for the pod
            String metricsUrl = "/apis/metrics.k8s.io/v1beta1/namespaces/" + namespace + "/pods/" + podName;

            // Send a request to the metrics API
            okhttp3.Request request = new okhttp3.Request.Builder()
                .url(client.getBasePath() + metricsUrl)
                .get()
                .build();

            okhttp3.Response response = client.getHttpClient().newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();

                // Parse the JSON response
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(responseBody);

                // Extract metrics for the pod
                ResourceUsageInformation resourceUsage = new ResourceUsageInformation();

                long cpuTotalUsage = 0;
                long memoryTotalUsage = 0;

                // Iterate over containers in the pod
                JsonNode containers = root.path("containers");
                for (JsonNode container : containers) {
                    String cpuUsageStr = container.path("usage").path("cpu").asText();
                    String memoryUsageStr = container.path("usage").path("memory").asText();

                    cpuTotalUsage += parseCpuUsage(cpuUsageStr);
                    memoryTotalUsage += parseMemoryUsage(memoryUsageStr);
                }

                // Set resource usage data
                CpuStats cpuStats = new CpuStats();
                cpuStats.setTotalUsage(cpuTotalUsage);

                MemoryStats memoryStats = new MemoryStats();
                memoryStats.setUsageSum(memoryTotalUsage);

                DiskStats diskStats = new DiskStats(); // Metrics Server does not provide disk usage
                diskStats.setFsSizeSum(0); // Set to 0 or implement a disk usage fetcher

                resourceUsage.setCpuStats(cpuStats);
                resourceUsage.setMemoryStats(memoryStats);
                resourceUsage.setDiskStats(diskStats);

                return resourceUsage;
            } else {
                System.err.println("Failed to fetch metrics for Pod " + podName + ": " + response.message());
            }

        } catch (Exception e) {
            System.err.println("Error fetching metrics for Pod " + podName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static long parseCpuUsage(String cpuUsageStr) {
        // Convert CPU usage string (e.g., "12345n" for nanoseconds) to long
        if (cpuUsageStr.endsWith("n")) {
            return Long.parseLong(cpuUsageStr.replace("n", ""));
        }
        return 0;
    }

    private static long parseMemoryUsage(String memoryUsageStr) {
        // Convert memory usage string (e.g., "128Mi", "512Ki") to bytes
        if (memoryUsageStr.endsWith("Mi")) {
            return Long.parseLong(memoryUsageStr.replace("Mi", "")) * 1024 * 1024;
        } else if (memoryUsageStr.endsWith("Ki")) {
            return Long.parseLong(memoryUsageStr.replace("Ki", "")) * 1024;
        } else if (memoryUsageStr.endsWith("Gi")) {
            return Long.parseLong(memoryUsageStr.replace("Gi", "")) * 1024 * 1024 * 1024;
        }
        return 0;
    }

}
