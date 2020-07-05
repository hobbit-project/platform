package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.dsl.LogWatch;

import java.io.File;
import java.util.List;

public interface PodsManager {

    // In Kubernetes we don't work with the containers directly, instead we use pods.
    // The pod houses a container. A pod is a single instance of an application.
    // It is the simplest object you can create in Kubernetes

    Pod getPod(String yaml_path);

    PodList getPods(String namespace);

    PodList getPods();

    PodList getPods(String namespace, String label1,  String label2);

    Pod getPod(String namespace, String name);

    void createPod(String name, String container_name, String image, int port, String namespace);

    void createOrReplacePod(String namespace, Pod pod);

    void editPodAddLabel(String namespace, String name, String label, String new_label);

    String getPodLog(String name, String namespace );

    LogWatch getStats(String podName, String namespace );

    Boolean deletePod(String namespace, String name);

    Boolean deletePods(String namespace, List<Pod> pods);

    void uploadToPod(String namespace, Pod pod, String filePath, File file);

    String readFromPod(String namespace, Pod pod, String path);


}
