package org.hobbit.controller.cloud.aws.handlers;


import org.hobbit.awscontroller.StackHandlers.SSHDependentStackHandler;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */

public class BastionStackHandler extends SSHDependentStackHandler {


    public BastionStackHandler(BasicClusterStackHandler.Builder builder){
        super(builder);
        name = builder.bastionStackName;
        bodyFilePath = "AWS/bastion.yaml";
    }


}
