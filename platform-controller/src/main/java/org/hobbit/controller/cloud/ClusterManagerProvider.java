package org.hobbit.controller.cloud;

import org.hobbit.controller.cloud.aws.swarm.SwarmClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */


public class ClusterManagerProvider {
    public static final String CLOUD_VPC_CLUSTER_NAME_KEY ="CLOUD_VPC_CLUSTER_NAME";
    private static Logger LOGGER = LoggerFactory.getLogger(ClusterManagerProvider.class);
    static SwarmClusterManager swarmClusterManager;

    public static SwarmClusterManager getManager(){
        if (swarmClusterManager ==null) {
            try {
                String vpcClusterName = (System.getenv().containsKey(CLOUD_VPC_CLUSTER_NAME_KEY)?System.getenv().get(CLOUD_VPC_CLUSTER_NAME_KEY):"hobbit");

                swarmClusterManager = new SwarmClusterManager(vpcClusterName);
            }
            catch (Exception e){
                LOGGER.error("Failed to initialize swarmClusterManager");
            }
        }
        return swarmClusterManager;
    }
}
