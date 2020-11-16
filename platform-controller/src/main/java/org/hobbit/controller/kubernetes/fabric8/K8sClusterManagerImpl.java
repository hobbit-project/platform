package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K8sClusterManagerImpl implements K8sClusterManager {

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
    public NodeStatus getClusterNodeInfo(String nodeName) {
        Node node = k8sClient.nodes().withName(nodeName).get();
        return node.getStatus();
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
        // kubernetes implementation not fully understood
    }

    public int getTaskHistoryLimit(){
        // kubernetes implementation not fully understood
        return 0;
    }

}
