package org.hobbit.controller.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Info;
import com.spotify.docker.client.messages.swarm.Node;
import com.spotify.docker.client.messages.swarm.OrchestrationConfig;
import com.spotify.docker.client.messages.swarm.SwarmSpec;
import com.spotify.docker.client.messages.swarm.Version;

/**
 * ClusterManager implementation
 *
 * @author Ivan Ermilov (iermilov@informatik.uni-leipzig.de)
 *
 */
public class ClusterManagerImpl implements ClusterManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);

    /**
     * Docker client instance
     */
    private DockerClient dockerClient;
    private Integer expectedNumberOfNodes = 0;
    private String SWARM_NODE_NUMBER = null;

    public ClusterManagerImpl() throws DockerCertificateException {
        dockerClient = DockerUtility.getDockerClient();
        SWARM_NODE_NUMBER = System.getenv("SWARM_NODE_NUMBER");
        if(SWARM_NODE_NUMBER == null) {
            expectedNumberOfNodes = 1;
        } else {
            expectedNumberOfNodes = Integer.parseInt(SWARM_NODE_NUMBER);
        }
    }

    public Info getClusterInfo() throws DockerException, InterruptedException {
        return dockerClient.info();
    }

    public int getNumberOfNodes() throws DockerException, InterruptedException {
        final Info info = getClusterInfo();
        return info.swarm().nodes();
    }

    public int getNumberOfNodes(String label) throws DockerException, InterruptedException {
        /*
        // doesn't work
        Node.Criteria criteria = Node.Criteria.builder().label(label).build();
        return dockerClient.listNodes(criteria).size();
        */
        final String[] parts = label.split("=");
        int number = 0;
        for (Node node : dockerClient.listNodes()) {
            if (node.spec().labels().containsKey(parts[0])) {
                if (parts.length == 1 || node.spec().labels().get(parts[0]).equals(parts[1])) {
                    number++;
                }
            }
        }
        return number;
    }

    public boolean isClusterHealthy() throws DockerException, InterruptedException {
        Integer numberOfNodes = getNumberOfNodes();
        if(numberOfNodes >= expectedNumberOfNodes) {
            return true;
        }
        LOGGER.debug("Cluster is not healthy ({}/{})",numberOfNodes, expectedNumberOfNodes);
        return false;
    }

    public int getExpectedNumberOfNodes() {
        return expectedNumberOfNodes;
    }

    public void setTaskHistoryLimit(Integer taskHistoryLimit) throws DockerException, InterruptedException {
        OrchestrationConfig orchestrationConfig = OrchestrationConfig.builder()
                .taskHistoryRetentionLimit(0)
                .build();
        SwarmSpec currentSwarmSpec = dockerClient.inspectSwarm().swarmSpec();
        SwarmSpec updatedSwarmSpec = SwarmSpec.builder()
                .orchestration(orchestrationConfig)
                .caConfig(currentSwarmSpec.caConfig())
                .dispatcher(currentSwarmSpec.dispatcher())
                .encryptionConfig(currentSwarmSpec.encryptionConfig())
                .labels(currentSwarmSpec.labels())
                .name(currentSwarmSpec.name())
                .raft(currentSwarmSpec.raft())
                .taskDefaults(currentSwarmSpec.taskDefaults())
                .build();
        Version swarmVersion = dockerClient.inspectSwarm().version();
        dockerClient.updateSwarm(swarmVersion.index(), updatedSwarmSpec);
    }

    public int getTaskHistoryLimit() throws DockerException, InterruptedException {
        SwarmSpec currentSwarmSpec = dockerClient.inspectSwarm().swarmSpec();
        return currentSwarmSpec.orchestration().taskHistoryRetentionLimit();
    }
}
