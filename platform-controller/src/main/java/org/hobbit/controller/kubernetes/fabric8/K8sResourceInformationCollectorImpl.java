package org.hobbit.controller.kubernetes.fabric8;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetricsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.commons.io.IOUtils;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.hobbit.controller.data.SetupHardwareInformation;
import org.hobbit.core.data.usage.CpuStats;
import org.hobbit.core.data.usage.DiskStats;
import org.hobbit.core.data.usage.MemoryStats;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class K8sResourceInformationCollectorImpl implements K8sResourceInformationCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8sResourceInformationCollectorImpl.class);


    public static final String PROMETHEUS_HOST_KEY = "PROMETHEUS_HOST";
    public static final String PROMETHEUS_PORT_KEY = "PROMETHEUS_PORT";

    public static final String PROMETHEUS_HOST_DEFAULT = "localhost";
    public static final String PROMETHEUS_PORT_DEFAULT = "9090";

    private static final String PROMETHEUS_METRIC_CPU_CORES = "machine_cpu_cores";
    private static final String PROMETHEUS_METRIC_CPU_FREQUENCY = "node_cpu_frequency_max_hertz";
    private static final String PROMETHEUS_METRIC_CPU_USAGE = "container_cpu_usage_seconds_total";
    private static final String PROMETHEUS_METRIC_FS_USAGE = "container_fs_usage_bytes";
    private static final String PROMETHEUS_METRIC_MEMORY = "node_memory_MemTotal_bytes";
    private static final String PROMETHEUS_METRIC_MEMORY_USAGE = "container_memory_usage_bytes";
    private static final String PROMETHEUS_METRIC_SWAP = "node_memory_SwapTotal_bytes";
    private static final String PROMETHEUS_METRIC_UNAME = "node_uname_info";


    private PodsManager manager;
    private KubernetesClient  k8sClient;
    private String prometheusHost;
    private String prometheusPort;

    private NamespaceManager namespaceManager;


    public K8sResourceInformationCollectorImpl(PodsManager manager, NamespaceManager namespaceManager) {
        this(manager, namespaceManager, null, null);
    }


    public K8sResourceInformationCollectorImpl(PodsManager manager,NamespaceManager namespaceManager, String prometheusHost, String prometheusPort) {
        this.manager = manager;
        this.namespaceManager = namespaceManager;
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

        k8sClient = K8sUtility.getK8sClient();
    }

    @Override
    public ResourceUsageInformation getSystemUsageInformation() {
        DeploymentList deployments = k8sClient.apps().deployments().inAnyNamespace().withLabel(NamespaceManager.APP_CATEGORY_KEY, NamespaceManager.APP_CATEGORY_VALUE )
            .withLabel(PodsManager.LABEL_TYPE, PodsManager.LABEL_PARENT).list();

        getUsageInformation(deployments);


        return null;
    }

    @Override
    public ResourceUsageInformation getUsageInformation(DeploymentList deployments) {

        Map<String, Deployment> podMapping = new HashMap<>();

        for (Deployment d: deployments.getItems()){
            podMapping.put(d.getMetadata().getName(), d);
        }

        ResourceUsageInformation resourceInfo = podMapping.keySet().parallelStream()
            // filter all pods that are not running
            .filter(d -> countRunningReplicas(d) !=0)
            // get the stats for the single deployment
            .map(id -> requestCpuAndMemoryStats(id))
            // sum up the stats
            .collect(Collectors.reducing(ResourceUsageInformation::staticMerge)).orElse(null);


        return resourceInfo;
    }


    private long countRunningReplicas(String deploymentName) {
        Deployment d =  k8sClient.apps().deployments().inNamespace("default").withName(deploymentName).get();
        return d.getStatus().getAvailableReplicas();
    }


    protected ResourceUsageInformation requestCpuAndMemoryStats(String deploymentName) {
        ResourceUsageInformation resourceInfo = new ResourceUsageInformation();
        try {
            Double value = requestAveragePrometheusValue(PROMETHEUS_METRIC_CPU_USAGE, deploymentName);
            if (value != null) {
                resourceInfo.setCpuStats(new CpuStats(Math.round(value * 1000)));
            }
        } catch (Exception e) {
            LOGGER.error("Could not get cpu usage stats for container {}", deploymentName, e);
        }
        try {
            String value = requestSamplePrometheusValue(PROMETHEUS_METRIC_MEMORY_USAGE, deploymentName);
            if (value != null) {
                resourceInfo.setMemoryStats(new MemoryStats(Long.parseLong(value)));
            }
        } catch (Exception e) {
            LOGGER.error("Could not get memory usage stats for container {}", deploymentName, e);
        }
        try {
            String value = requestSamplePrometheusValue(PROMETHEUS_METRIC_FS_USAGE, deploymentName);
            if (value != null) {
                resourceInfo.setDiskStats(new DiskStats(Long.parseLong(value)));
            }
        } catch (Exception e) {
            LOGGER.error("Could not get disk usage stats for container {}", deploymentName, e);
        }
        return resourceInfo;
    }

    private Double requestAveragePrometheusValue(String metric, String deploymentName) {
        Map<String, Map<String, List<String>>> instances = requestPrometheusMetrics(new String[] {metric}, deploymentName);
        if (instances.size() == 0) {
            return null;
        }
        return instances.values().stream().map(item -> item.get(metric).get(0))
            .collect(
                Collectors.averagingDouble(Double::parseDouble)
            );
    }

    private String requestSamplePrometheusValue(String metric, String deploymentName) {
        Map<String, Map<String, List<String>>> instances = requestPrometheusMetrics(new String[] {metric}, deploymentName);
        if (instances.size() == 0) {
            return null;
        }
        return instances.values().iterator().next().get(metric).get(0);
    }

    // metrics should not contain regular expression special characters
    private Map<String, Map<String, List<String>>> requestPrometheusMetrics(String[] metrics, String deploymentName) {
        StringBuilder query = new StringBuilder();
        query.append('{');
        query.append("__name__=~").append('"')
            .append("^").append(String.join("|", metrics)).append("$")
            .append('"');
        if (deploymentName != null) {
            query.append(", ");
            query.append("pod_label_com_kubernetes_deployment_name=").append('"')
                .append(deploymentName)
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

    private String prometheusMetricValue(JsonObject obj) {
        JsonObject metricObj = obj.getAsJsonObject("metric");
        switch (metricObj.get("__name__").getAsString()) {
            case PROMETHEUS_METRIC_UNAME:
                StringBuilder builder = new StringBuilder();
                builder.append(metricObj.get("sysname").getAsString());
                builder.append(metricObj.get("release").getAsString());
                builder.append(metricObj.get("version").getAsString());
                builder.append(metricObj.get("machine").getAsString());
                return builder.toString();
            default:
                return obj.get("value").getAsJsonArray().get(1).getAsString();
        }
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

    @Override
    public SetupHardwareInformation getHardwareInformation() {
        return null;
    }
}












