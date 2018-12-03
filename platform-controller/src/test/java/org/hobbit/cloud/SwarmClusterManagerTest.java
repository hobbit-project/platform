package org.hobbit.cloud;

import com.google.gson.JsonObject;
import org.hobbit.controller.cloud.aws.swarm.SwarmClusterManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class SwarmClusterManagerTest {

    SwarmClusterManager manager;
    String clusterName = "hobbit";


    @Before
    public void init() throws Exception {
        manager = new SwarmClusterManager(clusterName);
    }

    @Test
    @Ignore
    public void createManagersTest() throws Exception {
        //manager.createManagers(null);
        Assert.assertTrue(true);
    }

    @Test
    @Ignore
    public void createClusterTest() throws Exception {

        JsonObject config = new JsonObject();

        JsonObject nodes = new JsonObject();
//        nat.addProperty("InstanceType", "m5.large");
//        //nat.addProperty("instanceType", "c5.large");
//        //nat.addProperty("InstanceType", "t2.medium");
//        config.add(clusterName+"-nat", nat);

//        JsonObject nodes = new JsonObject();
//        nodes.addProperty("DesiredCapacity", "1");
//        //managerNodes.addProperty("InstanceType", "t2.small");
//        nodes.addProperty("InstanceType", "t2.medium");
//        //managerNodes.addProperty("InstanceType", "c4.large");
//        config.add(clusterName+"-swarm-manager", nodes);
//
//        nodes = new JsonObject();
        nodes.addProperty("DesiredCapacity", "1");
        nodes.addProperty("InstanceType", "t2.medium");
        //nat.addProperty("InstanceType", "t2.small");
        config.add(clusterName+"-swarm-benchmark-worker", nodes);
//
        nodes = new JsonObject();
        nodes.addProperty("DesiredCapacity", "1");
        nodes.addProperty("InstanceType", "t2.micro");
        //nodes.addProperty("InstanceType", "c4.large");
        //nodes.addProperty("InstanceType", "t2.medium");
        config.add(clusterName+"-swarm-system-worker", nodes);

        manager.createCluster(config.toString());
        //manager.createCluster("{hobbit-nat:{InstanceType:t2.small},hobbit-swarm-manager:{DesiredCapacity:1,InstanceType:t2.small},hobbit-swarm-benchmark-worker:{DesiredCapacity:1},hobbit-swarm-system-worker:{DesiredCapacity:1,InstanceType:t2.micro}}");
        Assert.assertTrue(true);
    }

    @Test
    @Ignore
    public void deleteClusterTest() throws Exception {
        //manager.reactOnQueue();
        manager.deleteCluster();
        //manager.deleteSwarmCluster();
        Assert.assertTrue(true);
    }
}

