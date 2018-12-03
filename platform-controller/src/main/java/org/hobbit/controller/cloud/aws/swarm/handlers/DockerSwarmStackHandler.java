package org.hobbit.controller.cloud.aws.swarm.handlers;

import org.hobbit.awscontroller.StackHandlers.SSHDependentStackHandler;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */

public class DockerSwarmStackHandler extends SSHDependentStackHandler {

    public DockerSwarmStackHandler(SwarmClusterStackHandler.Builder builder) {
        super(builder);
        parameters.put("ParentSecurityGroupsStack", builder.securityGroupsStackName);
        parameters.put("ParentKeysManagementStack", builder.keysManagementStackName);
        parameters.put("DockerVersion", "17.12.1");
        parameters.put("BucketName", builder.bucketName);

    }

}