package org.hobbit.controller.cloud.aws.handlers;

import org.hobbit.awscontroller.StackHandlers.SSHDependentStackHandler;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */

public class BasicClusterStackHandler extends SSHDependentStackHandler{

    public BasicClusterStackHandler(SSHDependentStackHandler.Builder builder) {
        super(builder);
    }

    public static class Builder
            extends SSHDependentStackHandler.Builder<BasicClusterStackHandler,Builder>
    {

        protected String bastionStackName;
        protected String natStackName;

        public String getBastionStackName() {
            return bastionStackName;
        }

        public String getNatStackName() {
            return natStackName;
        }

        public Builder bastionStackName(String value){
            this.bastionStackName = value;
            return this;
        }

            public Builder natStackName (String value){
            this.natStackName = value;
            return this;
        }

    }
}