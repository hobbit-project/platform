package org.hobbit.controller.containers.kubernetes;

import org.hobbit.controller.containers.ContainerTerminationCallback;

public class ContainerTerminationCallbackImpl implements ContainerTerminationCallback {
    private String containerId;
    private long exitCode;

    @Override
    public void notifyTermination(String containerId, long exitCode) {
        this.containerId = containerId;
        this.exitCode = exitCode;
    }

    public String getContainerId() {
        return containerId;
    }

    public long getExitCode() {
        return exitCode;
    }
}
