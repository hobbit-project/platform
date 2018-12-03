package org.hobbit.controller.cloud.aws.swarm.handlers;

import org.hobbit.awscontroller.StackHandlers.VpcDependentStackHandler;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */


public class SecurityGroupsStackHandler extends VpcDependentStackHandler {


    public SecurityGroupsStackHandler(SwarmClusterStackHandler.Builder builder) {
        super(builder);
        name = builder.securityGroupsStackName;
        bodyFilePath = "AWS/swarm-mode/securitygroups.yaml";
    }




}
