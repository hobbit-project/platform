package org.hobbit.controller.kubernetes;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hobbit.controller.orchestration.ClusterManager;
import org.hobbit.controller.orchestration.objects.ClusterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sClusterManagerImpl implements ClusterManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8sClusterManagerImpl.class);
    /**
     * Kubernetes client instance
     */
    private KubernetesClient k8sClient;


    private long expectedNumberOfNodes = 0;
    private String K8S_NODE_NUMBER = null;

    public K8sClusterManagerImpl() {
        this.k8sClient = K8sUtility.getK8sClient();

        K8S_NODE_NUMBER = System.getenv("K8S_NODE_NUMBER");
        if (K8S_NODE_NUMBER == null){
            expectedNumberOfNodes = 1;
        }else{
            expectedNumberOfNodes = Integer.parseInt(K8S_NODE_NUMBER);
        }

    }

    @Override
    public ClusterInfo getClusterInfo() {
        Config config =  k8sClient.getConfiguration();
        ClusterInfo clusterInfo = new ClusterInfo();
        clusterInfo.setHttpProxy(config.getHttpProxy());
        clusterInfo.setHttpsProxy(config.getHttpsProxy());

        return clusterInfo;
    }

    @Override
    public long getNumberOfNodes() {
        return k8sClient.nodes().list().getItems().size();
    }

    @Override
    public long getNumberOfNodes(String label) {
        return k8sClient.nodes().withLabel(label).list().getItems().size();
    }

    @Override
    public boolean isClusterHealthy() {
        long numberOfNodes = getNumberOfNodes();
        if (numberOfNodes >= expectedNumberOfNodes){
            return true;
        }
        LOGGER.debug("Cluster is not healthy ({}/{})",numberOfNodes, expectedNumberOfNodes);
        return false;
    }

    @Override
    public long getExpectedNumberOfNodes() {
        return expectedNumberOfNodes;
    }


    public void setTaskHistoryLimit(Integer taskHistoryLimit){
        DeploymentList deployments = k8sClient.apps().deployments().list();
        deployments.getItems().forEach(
            d -> d.getSpec().setRevisionHistoryLimit(taskHistoryLimit)
        );
    }

    public int getTaskHistoryLimit(){
        // revision history limit is set at deployment level not cluster level
        // returning the average revision history across the deployments in the cluster instead
        DeploymentList deployments = k8sClient.apps().deployments().list();
        int taskHistoryLimit = 0;
        for (Deployment d : deployments.getItems()){
            taskHistoryLimit += d.getSpec().getRevisionHistoryLimit();
        }
        taskHistoryLimit = taskHistoryLimit/deployments.getItems().size();
        return taskHistoryLimit;
    }

}
