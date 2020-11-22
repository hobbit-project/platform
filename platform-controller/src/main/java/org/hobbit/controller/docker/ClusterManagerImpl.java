package org.hobbit.controller.docker;

import java.util.Date;
import java.util.stream.Stream;

import org.hobbit.controller.ochestration.ClusterManager;
import org.hobbit.controller.ochestration.objects.ClusterInfo;
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

    private long expectedNumberOfNodes = 0;
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

    public ClusterInfo getClusterInfo(){
        Info info = null;
        try {
            info = dockerClient.info();
        } catch (DockerException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }

        ClusterInfo clusterInfo = new ClusterInfo(info.architecture(), info.clusterStore(), info.cgroupDriver(), info.containers(), info.containersRunning(),
            info.containersStopped(), info.containersPaused(), info.cpuCfsPeriod(), info.cpuCfsQuota(), info.debug(), info.dockerRootDir(), info.storageDriver(),
            info.driverStatus(), info.experimentalBuild(), info.httpProxy(), info.httpsProxy(), info.id(), info.ipv4Forwarding(),
            info.images(), info.indexServerAddress(), info.initPath(), info.initSha1(), info.kernelMemory(), info.kernelVersion(), info.labels(), info.memTotal(),
            info.memoryLimit(), info.cpus(), info.eventsListener(), info.fileDescriptors(), info.goroutines(), info.name(), info.noProxy(), info.oomKillDisable(), info.operatingSystem(),
            info.osType(), info.systemStatus(), info.systemTime());


        return clusterInfo;
    }

    private Stream<Node> streamReadyNodes() throws DockerException, InterruptedException {
        return dockerClient.listNodes().stream().filter(n->n.status().state().equalsIgnoreCase("ready"));
    }

    public long getNumberOfNodes() {
        Long nodes = 0L;
        try {
            nodes = streamReadyNodes().count();
        } catch (DockerException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return nodes;
    }

    public long getNumberOfNodes(String label) {
        Long nodes = 0L;
        final String[] parts = label.split("=");
        try {
            nodes =  streamReadyNodes().filter(n->parts[1].equals(n.spec().labels().get(parts[0]))).count();
        } catch (DockerException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return nodes;
    }

    public boolean isClusterHealthy(){
        long numberOfNodes = getNumberOfNodes();
        if(numberOfNodes >= expectedNumberOfNodes) {
            return true;
        }
        LOGGER.debug("Cluster is not healthy ({}/{})",numberOfNodes, expectedNumberOfNodes);
        return false;
    }

    public long getExpectedNumberOfNodes() {
        return expectedNumberOfNodes;
    }

    public void setTaskHistoryLimit(Integer taskHistoryLimit){
        OrchestrationConfig orchestrationConfig = OrchestrationConfig.builder()
                .taskHistoryRetentionLimit(0)
                .build();
        SwarmSpec currentSwarmSpec = null;
        try {
            currentSwarmSpec = dockerClient.inspectSwarm().swarmSpec();
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
            Version swarmVersion = null;
            swarmVersion = dockerClient.inspectSwarm().version();

            dockerClient.updateSwarm(swarmVersion.index(), updatedSwarmSpec);
        } catch (DockerException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    public int getTaskHistoryLimit(){
        SwarmSpec currentSwarmSpec = null;
        try {
            currentSwarmSpec = dockerClient.inspectSwarm().swarmSpec();
        } catch (DockerException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return currentSwarmSpec.orchestration().taskHistoryRetentionLimit();
    }
}
