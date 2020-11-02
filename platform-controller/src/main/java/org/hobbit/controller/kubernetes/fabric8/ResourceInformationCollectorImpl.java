package org.hobbit.controller.kubernetes.fabric8;

import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.TaskStatus;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hobbit.controller.data.SetupHardwareInformation;
import org.hobbit.controller.docker.ContainerManager;
import org.hobbit.core.Constants;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceInformationCollectorImpl implements ResourceInformationCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceInformationCollectorImpl.class);


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


    public ResourceInformationCollectorImpl(PodsManager manager) {
        this(manager, null, null);
    }


    public ResourceInformationCollectorImpl(PodsManager manager, String prometheusHost, String prometheusPort) {
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

        k8sClient = K8sUtility.getK8sClient();
    }

    @Override
    public ResourceUsageInformation getSystemUsageInformation() {



        return null;
    }

    @Override
    public ResourceUsageInformation getUsageInformation() {
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


    private long countRunningTasks(String serviceName) {
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












