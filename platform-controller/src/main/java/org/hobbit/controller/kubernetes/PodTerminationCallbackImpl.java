package org.hobbit.controller.kubernetes;

public class PodTerminationCallbackImpl implements PodTerminationCallback {

    public String podId;
    public String terminationStatus;

    @Override
    public void notifyTermination(String podId, String terminationStatus) {
        this.podId = podId;
        this.terminationStatus = terminationStatus;
    }
}
