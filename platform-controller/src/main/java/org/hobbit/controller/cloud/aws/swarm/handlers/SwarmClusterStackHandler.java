package org.hobbit.controller.cloud.aws.swarm.handlers;


import org.hobbit.awscontroller.StackHandlers.SSHDependentStackHandler;
import org.hobbit.controller.cloud.aws.handlers.BasicClusterStackHandler;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */


public class SwarmClusterStackHandler extends SSHDependentStackHandler {

    public SwarmClusterStackHandler(SSHDependentStackHandler.Builder builder) {
        super(builder);
    }


    //public SwarmClusterStackHandler(Builder builder) {
//        super(builder);
//    }

    public static class Builder extends SSHDependentStackHandler.Builder<SwarmClusterStackHandler,Builder>
    {

        public String bucketName;
        public String securityGroupsStackName;
        public String keysManagementStackName;

        public String managersStackName;
        public String benchmarkWorkersStackName;
        public String systemWorkersStackName;
        public String workerType;

        public String bastionStackName;
        public String natStackName;

        public Builder(BasicClusterStackHandler.Builder parent) {
            vpcStackName = parent.getVpcStackName();
            bastionStackName = parent.getNatStackName();
            natStackName = parent.getNatStackName();
            sshKeyName = parent.getSshKeyName();
        }

        public Builder bastionStackName(String value){
            this.bastionStackName = value;
            return (Builder) this;
        }

        public Builder natStackName (String value){
            this.natStackName = value;
            return (Builder) this;
        }

        public Builder securityGroupsStackName(String value) {
            this.securityGroupsStackName = value;
            return this;
        }

        public Builder keysManagementStackName(String value) {
            this.keysManagementStackName = value;
            return this;
        }

        public Builder managersStackName(String value) {
            this.managersStackName = value;
            return this;
        }

        public Builder benchmarkWorkersStackName(String value) {
            this.benchmarkWorkersStackName = value;
            return this;
        }

        public Builder systemWorkersStackName(String value) {
            this.systemWorkersStackName = value;
            return this;
        }

        public Builder bucketName(String value) {
            this.bucketName = value;
            return this;
        }

        public Builder workerType(String value) {
            this.workerType = value;
            return this;
        }

    }


}
