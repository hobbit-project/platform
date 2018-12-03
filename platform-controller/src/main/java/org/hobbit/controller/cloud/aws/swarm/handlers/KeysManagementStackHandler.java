package org.hobbit.controller.cloud.aws.swarm.handlers;

import org.hobbit.awscontroller.StackHandlers.AbstractStackHandler;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */

public class KeysManagementStackHandler extends AbstractStackHandler {

    public KeysManagementStackHandler(SwarmClusterStackHandler.Builder builder){
        super(builder);
        name = builder.keysManagementStackName;
        bodyFilePath = "AWS/swarm-mode/kms.yaml";
    }




}