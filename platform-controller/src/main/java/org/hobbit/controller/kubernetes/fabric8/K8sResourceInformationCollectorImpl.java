package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetricsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.hobbit.controller.data.SetupHardwareInformation;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            .filter(d -> )


        NodeMetricsList nodeMetricList = k8sClient.top().nodes().metrics();

        ResourceUsageInformation resourceInfo =

        nodeMetricList.getItems().forEach(nodeMetrics ->
                nodeMetrics.getMetadata().getName(),
                nodeMetrics.getUsage().get("cpu").getAmount(), nodeMetrics.getUsage().get("cpu").getFormat(),
                nodeMetrics.getUsage().get("memory").getAmount(), nodeMetrics.getUsage().get("memory").getFormat()
            );

        List<Service> svcList = manager.getPods().getItems();

        Map<String, Service> podMapping = new HashMap<>();
        for(Service svc: svcList){
            podMapping.put(svc.getMetadata().getName(), svc);
        }

        /*
        ResourceUsageInformation  resourceInfo = podMapping.keySet().parallelStream()
            .filter(s -> countRunningTasks(s))
        */
        return null;
    }


    private long countRunningTasks(String deploymentName) {

        Deployment d=  k8sClient.apps().deployments().inNamespace("default").withName(deploymentName).get();


        // List number of running replicas in deployment


        k8sClient.apps().deployments().inNamespace("default").withName(deploymentName).watch(
            new Watcher<Deployment>() {
                @Override
                public void eventReceived(Action action, Deployment deployment) {

                }

                @Override
                public void onClose(KubernetesClientException e) {

                }
            }
        );
                //.watch();

        d.
        /*

        try {
            return k8sClient.services().listTasks(
                Task.Criteria.builder().serviceName(serviceName).build()
            )
                .stream()
                .filter(t -> TaskStatus.TASK_STATE_RUNNING.equals(t.status().state()))
                .count();
        } catch (DockerException | InterruptedException e) {
            return 0;
        }

         */
        return 0;
    }



    @Override
    public SetupHardwareInformation getHardwareInformation() {
        return null;
    }
}












