package org.hobbit.controller.cloud.aws.handlers;

import org.hobbit.awscontroller.StackHandlers.AbstractStackHandler;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */

public class VPCStackHandler extends AbstractStackHandler {


    public VPCStackHandler(BasicClusterStackHandler.Builder builder){
        super(builder);
        name = builder.getVpcStackName();
        bodyFilePath = "AWS/vpc-1azs.yaml";
    }

}
