package org.hobbit.controller.cloud.aws.swarm.handlers;


/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */


public class SwarmWorkerStackHandler extends DockerSwarmStackHandler {


    public SwarmWorkerStackHandler(SwarmClusterStackHandler.Builder builder) {
        super(builder);

        name = (builder.workerType.equals("benchmark")?builder.benchmarkWorkersStackName:builder.systemWorkersStackName);
        parameters.put("WorkerType", builder.workerType);
        bodyFilePath = "AWS/swarm-mode/worker.yaml";
    }




}


