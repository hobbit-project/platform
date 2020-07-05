package org.hobbit.controller.kubernetes.fabric8;

public interface PodTerminationCallback {

    public void notifyTermination(String podId, String terminationStatus);
}
