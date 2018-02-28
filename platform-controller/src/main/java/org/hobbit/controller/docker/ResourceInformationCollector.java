package org.hobbit.controller.docker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.data.usage.CpuStats;
import org.hobbit.core.data.usage.DiskStats;
import org.hobbit.core.data.usage.MemoryStats;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.TaskStatus;

/**
 * A class that can collect resource usage information for the containers known
 * by the given {@link ContainerManager} using the given {@link DockerClient}.
 *
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class ResourceInformationCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceInformationCollector.class);
    private static DockerClient dockerClient;

    public static final String PROMETHEUS_HOST_KEY = "PROMETHEUS_HOST";
    public static final String PROMETHEUS_PORT_KEY = "PROMETHEUS_PORT";

    public static final String PROMETHEUS_HOST_DEFAULT = "localhost";
    public static final String PROMETHEUS_PORT_DEFAULT = "9090";

    private ContainerManager manager;
    private String prometheusHost;
    private String prometheusPort;

    public ResourceInformationCollector(ContainerManager manager) {
        this(manager, null, null);
    }

    public ResourceInformationCollector(ContainerManager manager, String prometheusHost, String prometheusPort) {
        try {
            this.dockerClient = DockerUtility.getDockerClient();
        } catch (DockerCertificateException e) {
            LOGGER.error("Could not initialize Docker Client. Resource Information Collector will not work! ", e);
            return;
        }
        this.manager = manager;
        this.prometheusHost = prometheusHost;
        if ((this.prometheusHost == null) && System.getenv().containsKey(PROMETHEUS_HOST_KEY)) {
            this.prometheusHost = System.getenv().get(PROMETHEUS_HOST_KEY);
        }
        if (this.prometheusHost == null) {
            LOGGER.info("Prometheus host env {} is not set. Using default {}.", PROMETHEUS_HOST_KEY, PROMETHEUS_HOST_DEFAULT);
            this.prometheusHost = PROMETHEUS_HOST_DEFAULT;
        }
        this.prometheusPort = prometheusPort;
        if ((this.prometheusPort == null) && System.getenv().containsKey(PROMETHEUS_PORT_KEY)) {
            this.prometheusPort = System.getenv().get(PROMETHEUS_PORT_KEY);
        }
        if (this.prometheusPort == null) {
            LOGGER.info("Prometheus port env {} is not set. Using default {}.", PROMETHEUS_PORT_KEY, PROMETHEUS_PORT_DEFAULT);
            this.prometheusPort = PROMETHEUS_PORT_DEFAULT;
        }
    }

    public ResourceUsageInformation getSystemUsageInformation() {
        return getUsageInformation(Task.Criteria.builder()
                .label(ContainerManager.LABEL_TYPE + "=" + Constants.CONTAINER_TYPE_SYSTEM).build());
    }

    public ResourceUsageInformation getUsageInformation(Task.Criteria criteria) {
        List<Task> tasks = manager.getContainers(criteria);
        
        Map<String, Task> containerMapping = new HashMap<>();
        for (Task c : tasks) {
            containerMapping.put(c.id(), c);
        }
        ResourceUsageInformation resourceInfo = containerMapping.keySet().parallelStream()
                // filter all containers that are not running
                .filter(c -> TaskStatus.TASK_STATE_RUNNING.equals(containerMapping.get(c).status().state()))
                // get the stats for the single
                .map(id -> requestCpuAndMemoryStats(id))
                // sum up the stats
                .collect(Collectors.reducing(ResourceUsageInformation::staticMerge)).orElse(null);
        return resourceInfo;
    }

    protected ResourceUsageInformation requestCpuAndMemoryStats(String taskId) {
        ResourceUsageInformation resourceInfo = new ResourceUsageInformation();
        String value;
        try {
            value = requestPrometheusValue(taskId, "container_cpu_usage_seconds_total");
            if (value != null) {
                resourceInfo.setCpuStats(new CpuStats(Math.round(Double.parseDouble(value) * 1000)));
            }
        } catch (Exception e) {
            LOGGER.error("Could not get cpu usage stats for container {}", taskId, e);
        }
        try {
            value = requestPrometheusValue(taskId, "container_memory_usage_bytes");
            if (value != null) {
                resourceInfo.setMemoryStats(new MemoryStats(Long.parseLong(value)));
            }
        } catch (Exception e) {
            LOGGER.error("Could not get memory usage stats for container {}", taskId, e);
        }
        try {
            value = requestPrometheusValue(taskId, "container_fs_usage_bytes");
            if (value != null) {
                resourceInfo.setDiskStats(new DiskStats(Long.parseLong(value)));
            }
        } catch (Exception e) {
            LOGGER.error("Could not get disk usage stats for container {}", taskId, e);
        }
        return resourceInfo;
    }

    private String requestPrometheusValue(String taskId, String metric) throws IOException, MalformedURLException {
        StringBuilder builder = new StringBuilder();
        builder.append("http://").append(prometheusHost).append(':').append(prometheusPort)
                .append("/api/v1/query?query=")
                // append metric
                .append(metric)
                // append filter
                .append("{container_label_com_docker_swarm_task_id=\"").append(taskId).append("\"}");
        URL url = new URL(builder.toString());
        String content = IOUtils.toString(url.openConnection().getInputStream());
        System.out.println(content);
        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse(content).getAsJsonObject();
        JsonArray result = root.get("data").getAsJsonObject().get("result").getAsJsonArray();
        if (result.size() > 0) {
            return result.get(0).getAsJsonObject().get("value").getAsJsonArray().get(1).getAsString();
        } else {
            LOGGER.warn("Didn't got a result when requesting {} for {}. Returning null", metric, taskId);
            return null;
        }
    }

}
