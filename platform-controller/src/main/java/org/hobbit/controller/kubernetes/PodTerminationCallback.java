package org.hobbit.controller.kubernetes;

public interface PodTerminationCallback {

    public void notifyTermination(String podId, String terminationStatus);
}
