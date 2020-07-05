package org.hobbit.controller.kubernetes.fabric8;


import java.util.List;

public interface PodStateObserver {

    public void startObserving();

    public void stopObserving();

    public void addTerminationCallback(PodTerminationCallback callback);

    public void removeTerminationCallback(PodTerminationCallback callback);

    public void addObservedPod(String podId);

    public void removedObservedPod(String cpodId);

    public List<String> getObservedCPod();
}
