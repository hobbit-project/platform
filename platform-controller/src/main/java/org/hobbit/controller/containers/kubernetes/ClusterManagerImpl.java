package org.hobbit.controller.containers.kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import org.hobbit.controller.containers.ClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of the {@link ClusterManager} interface using Kubernetes API.
 * This class provides methods to manage and monitor a Kubernetes cluster, including listing nodes,
 * checking cluster health, and managing task history limits.
 * @author Farshad Afshari farshad.afshari@uni-paderborn.de
 */
public class ClusterManagerImpl implements ClusterManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);
    private final CoreV1Api coreV1Api;
    private Integer taskHistoryLimit = 0; // Default task history limit

    /**
     * Constructs a new {@code ClusterManagerImpl} with the given Kubernetes API client.
     *
     * @param apiClient The Kubernetes API client used to interact with the cluster.
     */
    public ClusterManagerImpl(ApiClient apiClient) {
        LOGGER.info("Creating a new cluster manager ");
        this.coreV1Api = new CoreV1Api(apiClient);
        LOGGER.info("coreV1Api created, lets test it");
        try{

            V1NodeList nodeList = coreV1Api.listNode(null, null, null, null, null, null, null, null, null, false);
            List<V1Node> nodes = nodeList.getItems();
            LOGGER.info(nodes.size() + " nodes found");
        }catch(Exception e){
            LOGGER.error("Failed to create cluster manager coreV1Api is the root of the problem", e);
        }
    }

    /**
     * Returns the total number of nodes in the Kubernetes cluster.
     *
     * @return The total number of nodes.
     * @throws InterruptedException If the operation is interrupted.
     */
    @Override
    public long getNumberOfNodes() throws InterruptedException {
        try {
            V1NodeList nodeList = coreV1Api.listNode(null, null, null, null, null, null, null, null, null, false);
            return nodeList.getItems().size();
        } catch (ApiException e) {
            throw new RuntimeException("Failed to get the number of nodes", e);
        }
    }

    /**
     * Returns the number of nodes in the Kubernetes cluster that match a specific label.
     *
     * @param label The label selector used to filter nodes.
     * @return The number of nodes matching the label.
     * @throws InterruptedException If the operation is interrupted.
     */
    @Override
    public long getNumberOfNodes(String label) throws InterruptedException {
        try {
            String labelSelector = label;
            V1NodeList nodeList = coreV1Api.listNode(null, null, null, labelSelector, null, null, null, null, null, false);
            return nodeList.getItems().size();
        } catch (ApiException e) {
            throw new RuntimeException("Failed to get the number of nodes with label: " + label, e);
        }
    }

    /**
     * Checks if the Kubernetes cluster is healthy by verifying that all nodes are in a 'Ready' state.
     *
     * @return {@code true} if the cluster is healthy, otherwise {@code false}.
     * @throws InterruptedException If the operation is interrupted.
     */
    @Override
    public boolean isClusterHealthy() throws InterruptedException {
        //LOGGER.info("Checking cluster health");
        try {
            V1NodeList nodeList = coreV1Api.listNode(null, null, null, null, null, null, null, null, null, false);
            List<V1Node> nodes = nodeList.getItems();
            //LOGGER.info(nodes.size() + " nodes found");
            for (V1Node node : nodes) {
                String status = node.getStatus().getConditions()
                    .stream()
                    .filter(condition -> "Ready".equals(condition.getType()))
                    .findFirst()
                    .map(condition -> condition.getStatus())
                    .orElse("Unknown");

                if (!"True".equalsIgnoreCase(status)) {
                    return false;
                }
            }
            return true;
        } catch (ApiException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to check cluster health", e);
        }
    }

    // TODO do we need this and should the environment variable be one or define another for kubernetes like KUB_NODE_NUMBER
    @Override
    public long getExpectedNumberOfNodes() {
        String expectedNodesEnv = System.getenv("SWARM_NODE_NUMBER");
        return expectedNodesEnv != null ? Long.parseLong(expectedNodesEnv) : 0;
    }

    @Override
    public void setTaskHistoryLimit(Integer taskHistoryLimit) throws InterruptedException {
        if (taskHistoryLimit == null || taskHistoryLimit < 0) {
            throw new IllegalArgumentException("Task history limit must be non-negative");
        }
        this.taskHistoryLimit = taskHistoryLimit;
        // TODO define this for kubernetes because as default I could not find usage !
    }

    @Override
    public int getTaskHistoryLimit() throws InterruptedException {
        return this.taskHistoryLimit;
    }
}
