package org.hobbit.controller.cloud.aws.swarm.handlers;


/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */


public class SwarmManagerStackHandler extends DockerSwarmStackHandler {

    public SwarmManagerStackHandler(SwarmClusterStackHandler.Builder builder) {
        super(builder);
        name = builder.managersStackName;
        bodyFilePath = "AWS/swarm-mode/manager.yaml";
    }



}

