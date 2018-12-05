package org.hobbit.controller.docker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.data.usage.CpuStats;
import org.hobbit.core.data.usage.DiskStats;
import org.hobbit.core.data.usage.MemoryStats;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.ext.com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
public class ResourceInformationCollectorImpl implements ResourceInformationCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceInformationCollectorImpl.class);

    public static final String PROMETHEUS_HOST_KEY = "PROMETHEUS_HOST";
    public static final String PROMETHEUS_PORT_KEY = "PROMETHEUS_PORT";

    public static final String PROMETHEUS_HOST_DEFAULT = "localhost";
    public static final String PROMETHEUS_PORT_DEFAULT = "9090";

    private static final String PROMETHEUS_METRIC_CPU_USAGE = "container_cpu_usage_seconds_total";
    private static final String PROMETHEUS_METRIC_FS_USAGE = "container_fs_usage_bytes";
    private static final String PROMETHEUS_METRIC_MEMORY_USAGE = "container_memory_usage_bytes";

    private ContainerManager manager;
    private String prometheusHost;
    private String prometheusPort;

    public ResourceInformationCollectorImpl(ContainerManager manager) {
        this(manager, null, null);
    }

    public ResourceInformationCollectorImpl(ContainerManager manager, String prometheusHost, String prometheusPort) {
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

    @Override
    public ResourceUsageInformation getSystemUsageInformation() {
        return getUsageInformation(Task.Criteria.builder()
                .label(ContainerManager.LABEL_TYPE + "=" + Constants.CONTAINER_TYPE_SYSTEM).build());
    }

    @Override
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
        try {
            Double value = requestAveragePrometheusValue(PROMETHEUS_METRIC_CPU_USAGE, taskId);
            if (value != null) {
                resourceInfo.setCpuStats(new CpuStats(Math.round(value * 1000)));
            }
        } catch (Exception e) {
            LOGGER.error("Could not get cpu usage stats for container {}", taskId, e);
        }
        try {
            String value = requestSamplePrometheusValue(PROMETHEUS_METRIC_MEMORY_USAGE, taskId);
            if (value != null) {
                resourceInfo.setMemoryStats(new MemoryStats(Long.parseLong(value)));
            }
        } catch (Exception e) {
            LOGGER.error("Could not get memory usage stats for container {}", taskId, e);
        }
        try {
            String value = requestSamplePrometheusValue(PROMETHEUS_METRIC_FS_USAGE, taskId);
            if (value != null) {
                resourceInfo.setDiskStats(new DiskStats(Long.parseLong(value)));
            }
        } catch (Exception e) {
            LOGGER.error("Could not get disk usage stats for container {}", taskId, e);
        }
        return resourceInfo;
    }

    private JsonArray queryPrometheus(String query) {
        LOGGER.debug("Prometheus query: {}", query);
        UriBuilder uri = UriBuilder.fromPath("/api/v1/query");
        uri.host(prometheusHost);
        uri.port(Integer.parseInt(prometheusPort));
        uri.queryParam("query", "{query}");
        URL url;
        try {
            url = new URI("http:///").resolve(uri.build(query)).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            LOGGER.error("Error while building Prometheus URL", e);
            return null;
        }
        LOGGER.debug("Prometheus URL: {}", url);
        String content = null;
        try {
            content = IOUtils.toString(url.openConnection().getInputStream());
        } catch (IOException e) {
            LOGGER.error("Error while requesting Prometheus", e);
            return null;
        }
        LOGGER.debug("Prometheus response: {}", content);
        JsonObject root = new JsonParser().parse(content).getAsJsonObject();
        return root.getAsJsonObject("data").getAsJsonArray("result");
    }

    private String prometheusMetricValue(JsonObject obj) {
        JsonObject metricObj = obj.getAsJsonObject("metric");
        switch (metricObj.get("__name__").getAsString()) {
            default:
                return obj.get("value").getAsJsonArray().get(1).getAsString();
        }
    }

    // metrics should not contain regular expression special characters
    private Map<String, Map<String, List<String>>> requestPrometheusMetrics(String[] metrics, String taskId) {
        StringBuilder query = new StringBuilder();
        query.append('{');
        query.append("__name__=~").append('"')
                .append("^").append(String.join("|", metrics)).append("$")
                .append('"');
        if (taskId != null) {
            query.append(", ");
            query.append("container_label_com_docker_swarm_task_id=").append('"')
                    .append(taskId)
                    .append('"');
        }
        query.append('}');
        JsonArray result = queryPrometheus(query.toString());
        // result is an array of data from all metrics from all instances
        return Streams.stream(result).map(JsonElement::getAsJsonObject).collect(
            // group by "instance" in the outer map
            // NOTE: Prometheus must be configured
            // so all extractors use the same instance value for the same swarm node
            Collectors.groupingBy(
                obj -> obj.getAsJsonObject("metric").get("instance").getAsString(),
                // group by "__name__" (metric name) in the inner map
                Collectors.groupingBy(
                    obj -> obj.getAsJsonObject("metric").get("__name__").getAsString(),
                    Collectors.mapping(
                        this::prometheusMetricValue,
                        Collectors.toList()
                    )
                )
            )
        );
    }

    private String requestSamplePrometheusValue(String metric, String taskId) {
        Map<String, Map<String, List<String>>> instances = requestPrometheusMetrics(new String[] {metric}, taskId);
        if (instances.size() == 0) {
            return null;
        }
        return instances.values().iterator().next().get(metric).get(0);
    }

    private Double requestAveragePrometheusValue(String metric, String taskId) {
        Map<String, Map<String, List<String>>> instances = requestPrometheusMetrics(new String[] {metric}, taskId);
        if (instances.size() == 0) {
            return null;
        }
        return instances.values().stream().map(item -> item.get(metric).get(0))
                .collect(
                        Collectors.averagingDouble(Double::parseDouble)
                );
    }

}
