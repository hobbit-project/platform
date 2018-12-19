package org.hobbit.controller.cloud.aws.swarm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hobbit.awscontroller.StackHandlers.AbstractStackHandler;
import org.hobbit.cloud.interfaces.Node;
import org.hobbit.cloud.vpc.VpcClusterManager;
import org.hobbit.controller.cloud.aws.handlers.BasicClusterStackHandler;
import org.hobbit.controller.cloud.aws.handlers.BastionStackHandler;
import org.hobbit.controller.cloud.aws.handlers.VPCStackHandler;
import org.hobbit.controller.cloud.aws.swarm.handlers.*;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.hobbit.controller.docker.CloudClusterManager.CLOUD_SSH_KEY_NAME_KEY;


/**
 * ClusterManager implementation on AWS resources
 *
 * @author Pavel Smirnov (smirnp@gmail.com)
 *
 */
public class SwarmClusterManager extends VpcClusterManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);
    private final BasicClusterStackHandler.Builder basicStacksBuilder;
    private final SwarmClusterStackHandler.Builder swarmStacksBuilder;

    protected AbstractStackHandler securityGroupsStackHandler;
    protected AbstractStackHandler keysManagementStackHandler;
    protected AbstractStackHandler swarmManagerStackHandler;
    protected AbstractStackHandler swarmBenchmarkWorkersStackHandler;
    protected AbstractStackHandler swarmSystemWorkersStackHandler;
    //protected SwarmClusterStackHandler.Builder swarmStacksBuilder;

    protected String swarmClusterName;
    protected String bucketName;

    boolean clusterReady = false;

    public SwarmClusterManager(String clusterName){
        super(clusterName, System.getenv(CLOUD_SSH_KEY_NAME_KEY));

        swarmClusterName = clusterName+"-swarm";
        bucketName = swarmClusterName+"-bucket";

        basicStacksBuilder = new BasicClusterStackHandler.Builder()
                                .vpcStackName(clusterName+"-vpc")
                                .bastionStackName(clusterName+"-bastion")
                                .natStackName(clusterName+"-nat")
                                .sshKeyName(System.getenv(CLOUD_SSH_KEY_NAME_KEY));

        swarmStacksBuilder = new SwarmClusterStackHandler.Builder(basicStacksBuilder)
                                .securityGroupsStackName(swarmClusterName+"-security-group")
                                .keysManagementStackName(swarmClusterName+"-keys-management")
                                .bucketName(bucketName)
                                .managersStackName(swarmClusterName+"-manager")
                                //.managersStackName(vpcClusterName+"2-swarm-manager")
                                .benchmarkWorkersStackName(swarmClusterName+"-benchmark-worker")
                                .systemWorkersStackName(swarmClusterName+"-system-worker");

        initStackHandlers(null);
    }

    protected void initStackHandlers(JsonObject configuration){

        Map<String, String> bastionConfig = getStackConfig(configuration, clusterName+"-bastion");
        Map<String, String> natConfig = getStackConfig(configuration, clusterName+"-nat");
        Map<String, String> managerConfig = getStackConfig(configuration, swarmClusterName+"-manager");
        Map<String, String> benchmarkWorkersConfig = getStackConfig(configuration, swarmClusterName+"-benchmark-worker");
        Map<String, String> systemWorkersConfig = getStackConfig(configuration, swarmClusterName+"-system-worker");

        vpcStackHandler = new VPCStackHandler(basicStacksBuilder);
        bastionStackHandler = new BastionStackHandler(basicStacksBuilder).appendParameters(bastionConfig);

        securityGroupsStackHandler = new SecurityGroupsStackHandler(swarmStacksBuilder);
        keysManagementStackHandler = new KeysManagementStackHandler(swarmStacksBuilder);
        swarmManagerStackHandler = new SwarmManagerStackHandler(swarmStacksBuilder).appendParameters(managerConfig);
        swarmBenchmarkWorkersStackHandler = new SwarmWorkerStackHandler(swarmStacksBuilder.workerType("benchmark")).appendParameters(benchmarkWorkersConfig);
        swarmSystemWorkersStackHandler = new SwarmWorkerStackHandler(swarmStacksBuilder.workerType("system")).appendParameters(systemWorkersConfig);

        stackList = new ArrayList<List<AbstractStackHandler>>() {{
            add(Arrays.asList(new AbstractStackHandler[]{vpcStackHandler}));
            add(Arrays.asList(new AbstractStackHandler[]{
                    securityGroupsStackHandler,
                    keysManagementStackHandler
            }));
            add(Arrays.asList(new AbstractStackHandler[]{
                    bastionStackHandler,
                    //natStackHandler,
                    swarmManagerStackHandler,
                    swarmBenchmarkWorkersStackHandler,
                    swarmSystemWorkersStackHandler
            }));
        }};
    }


    public List<Node> getManagerNodes() throws Exception{
        if(!clusterReady)
            return new ArrayList<>();

        List<Node> ret = getNodesFromAutoscalingGroup(swarmStacksBuilder.managersStackName);

        return ret;
    }

    public List<Node> getBechmarkNodes() throws Exception{
        if(!clusterReady)
            return new ArrayList<>();
        List<Node> ret = getNodesFromAutoscalingGroup(swarmStacksBuilder.benchmarkWorkersStackName);
        return ret;
    }

    public List<Node> getSystemNodes() throws Exception{
        if(!clusterReady)
            return new ArrayList<>();
        List<Node> ret = getNodesFromAutoscalingGroup(swarmStacksBuilder.systemWorkersStackName);
        return ret;
    }

    protected Map<String, String> getStackConfig(JsonObject configuration, String stackName){

        if (configuration != null && configuration.has(stackName)) {
            Map<String, String> ret = new HashMap<>();
            JsonObject stackParams = configuration.get(stackName).getAsJsonObject();
            for(Map.Entry<String, JsonElement> entry : stackParams.entrySet()) {

                ret.put(entry.getKey(), entry.getValue().getAsString());
                if(entry.getKey().equals("DesiredCapacity"))
                    ret.put("MaxSize", entry.getValue().getAsString());
                    //ret.put("MaxSize", String.valueOf(Integer.parseInt(entry.getValue().toString())+1));
            }
            return ret;
        }

        return null;
    }

   public JsonObject parseConfiguration(String desiredConfiguration){
        JsonObject configuration = null;
        if(desiredConfiguration==null || desiredConfiguration.equals(""))
            return configuration;
        try {
            JsonParser jsonParser = new JsonParser();
            configuration = jsonParser.parse(desiredConfiguration).getAsJsonObject();
        }
        catch (Exception e){
            LOGGER.error("Failed to parse configuraiton");
        }
        return configuration;
    }

    public String getClusterConfiguration() {
        return null;
    }


    @Override
    public void createCluster(String configuration) throws Exception {

        JsonObject jsonConfiguration = parseConfiguration(configuration);

        initStackHandlers(jsonConfiguration);

        Boolean updateStacksIfNotMatching = false;
        if(jsonConfiguration!=null)
            updateStacksIfNotMatching = true;


        long started = new Date().getTime();

        awsController.createBucket(bucketName);

        clusterReady = false;
        awsController.createStacks(stackList, updateStacksIfNotMatching);
        clusterReady = true;
        clusterCreated = started;
    }

//    public void createManagers(String configuration) throws Exception {
//
//        awsController.createBucket(bucketName);
//
//        ArrayList<List<AbstractStackHandler>> managerStackList = new ArrayList<List<AbstractStackHandler>>() {{
//            add(Arrays.asList(new AbstractStackHandler[]{securityGroupsStackHandler, keysManagementStackHandler}));
//            add(Arrays.asList(new AbstractStackHandler[]{swarmManagerStackHandler}));
//
//        }};
//
//        awsController.createStacks(managerStackList);
//    }
//
//    public void createWorkers(String configuration) throws Exception {
//        ArrayList<List<AbstractStackHandler>> workersStackList = new ArrayList<List<AbstractStackHandler>>() {{
//            add(Arrays.asList(new AbstractStackHandler[]{swarmBenchmarkWorkersStackHandler, swarmSystemWorkersStackHandler}));
//        }};
//        awsController.createStacks(workersStackList);
//    }

    @Override
    public void deleteCluster() throws Exception {

        //deleteSwarmCluster();
        //LOGGER.warn("Deletion of vpc, bastion, nat is disabled");
        awsController.deleteStacks(stackList);

        clusterCreated = 0;
        clusterReady=false;
    }

//    public void deleteSwarmCluster() throws Exception {
//        List<List<AbstractStackHandler>> stackList = new ArrayList<List<AbstractStackHandler>>() {{
//            add(Arrays.asList(new AbstractStackHandler[]{securityGroupsStackHandler,  keysManagementStackHandler }));
//            add(Arrays.asList(new AbstractStackHandler[]{swarmManagerStackHandler, swarmBenchmarkWorkersStackHandler, swarmSystemWorkersStackHandler }));
//        }};
//        awsController.deleteStacks(stackList);
//    }

//    @Override
//    public void deleteManagers() throws Exception {
//        awsController.deleteStacks(managerStackList);
//    }
//
//    public void deleteWorkers() throws Exception{
//        awsController.deleteStacks(workersStackList);
//    }

}
