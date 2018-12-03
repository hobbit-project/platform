package org.hobbit.controller.docker;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import org.hobbit.controller.cloud.CloudSshTunnelsProvider;
import org.hobbit.controller.PlatformController;
import org.hobbit.controller.cloud.aws.swarm.SwarmClusterManager;
import org.hobbit.controller.cloud.ClusterManagerProvider;
import org.hobbit.controller.cloud.DockerClientProvider;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import org.hobbit.controller.queue.ExperimentQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * ClusterManager implementation on AWS resources
 *
 * @author Pavel Smirnov (smirnp@gmail.com)
 *
 */
public class CloudClusterManager extends ClusterManagerImpl {
    public static final String CLOUD_EXPIRE_TIMEOUT_MIN_KEY ="CLOUD_EXPIRE_TIMEOUT_MIN";
    public static final String CLOUD_SSH_KEY_NAME_KEY ="CLOUD_SSH_KEY_NAME";
    public static final String CLOUD_SSH_KEY_FILE_PATH_KEY ="CLOUD_SSH_KEY_FILE_PATH";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudClusterManager.class);

    private SwarmClusterManager swarmClusterManager;
    private CloudSshTunnelsProvider commonSshTunnelsProvider;

    private long clusterDeletionTime=0;
    private boolean clusterDeleted;
    private boolean creationRequested;
    private long prevDelta;
    private int clusterExpireTimeoutMin = 30;


    public CloudClusterManager() throws DockerCertificateException{
        super();
        swarmClusterManager = ClusterManagerProvider.getManager();
        if(System.getenv().containsKey(CLOUD_EXPIRE_TIMEOUT_MIN_KEY))
            clusterExpireTimeoutMin = Integer.parseInt(System.getenv().get(CLOUD_EXPIRE_TIMEOUT_MIN_KEY));

    }

    public CloudClusterManager(PlatformController platformController) throws DockerCertificateException{
        this();
        commonSshTunnelsProvider = new CloudSshTunnelsProvider(platformController);
    }

    @Override
    public boolean isClusterHealthy() throws DockerException, InterruptedException {
        try {
            if(swarmClusterManager.getManagerNodes().size()==0)
                return false;
        } catch (Exception e) {
            LOGGER.error("Cannot get manager nodes: {}", e.getLocalizedMessage());
            return false;
        }

        return super.isClusterHealthy();
    }

    @Override
    public DockerClient getDockerClient(){
        return DockerClientProvider.getDockerClient();
    }

    public SwarmClusterManager getSwarmClusterManager() {
        return swarmClusterManager;
    }

    @Override
    public Integer getNumberOfNodes() throws DockerException, InterruptedException {
        //final Info info = getClusterInfo();
        //return info.swarm().nodes();
        try {
            if(swarmClusterManager.getManagerNodes().size()==0)
                return 0;
        } catch (Exception e) {
            LOGGER.error("Cannot get manager nodes: {}", e.getLocalizedMessage());
            return 0;
        }
        return super.getNumberOfNodes();
    }

    public void createCluster(String clusterConfiguration) throws Exception {
        //if(!creationRequested){
        //    creationRequested = true;
        swarmClusterManager.createCluster(clusterConfiguration);

        if(!commonSshTunnelsProvider.isConnected()) {
            if(!commonSshTunnelsProvider.isProviderInitialized())
                commonSshTunnelsProvider.initSshProvider();
            commonSshTunnelsProvider.newSshTunnel(null, null);
        }

        creationRequested = false;
        //}

    }

    public String getClusterConfiguration(){
        return swarmClusterManager.getClusterConfiguration();
    }

    public void reactOnQueue(ExperimentQueue queue){

        if(queue.listAll().size()==0){
            if(clusterExpireTimeoutMin>0)
                if(!clusterDeleted){
                    if(clusterDeletionTime==0)
                        clusterDeletionTime = new Date().getTime() + clusterExpireTimeoutMin * 60 * 1000;
                    else{
                        long delta = ((clusterDeletionTime - new Date().getTime())/60000)+1;
                        if(prevDelta!=delta)
                            LOGGER.info("The queue is empty. Cluster (if exists) will be deleted in {} minutes", delta);
                        prevDelta = delta;

                        if(delta<=0){
                            LOGGER.info("Deleting cluster");
                            try {
                                swarmClusterManager.deleteCluster();
                                clusterDeletionTime=0;
                                clusterDeleted = true;
                            } catch (Exception e) {
                                LOGGER.error("Failed to delete cluster: {}", e.getLocalizedMessage());
                                e.printStackTrace();
                            }
                        }

                    }
                }
        }else{
            clusterDeleted=false;
            clusterDeletionTime=0;
        }
    }
}
