package org.hobbit.controller.docker;

/**
 * Created by Timofey Ermilov on 01/09/16.
 */
public class ContainerTerminationCallbackImpl implements ContainerTerminationCallback {
    public String containerId;
    public int exitCode;

    @Override
    public void notifyTermination(String containerId, int exitCode) {
        this.containerId = containerId;
        this.exitCode = exitCode;
    }
}
