package org.hobbit.controller.kubernetes;


import io.fabric8.kubernetes.api.model.PodList;

import java.util.List;

public interface PodStateObserver {

    public void startObserving();

    public void stopObserving();

    /*

    public void addTerminationCallback(PodTerminationCallback callback);

    public void removeTerminationCallback(PodTerminationCallback callback);

    public void addObservedPod(String podId);

    public void removedObservedPod(String cpodId);

    */

    public PodList getObservedPods();
}
