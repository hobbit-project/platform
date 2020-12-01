package org.hobbit.controller.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.dsl.LogWatch;

import java.io.File;
import java.util.List;

public interface PodsManager {

    /**
     * Label that denotes pod type
     */
    public static final String LABEL_TYPE = "org.hobbit.type";
    /**
     * Label that denotes pod parent
     */
    public static final String LABEL_PARENT = "org.hobbit.parent";


    Pod getPod(String yaml_path);

    PodList getPods(String namespace);

    ServiceList getPods();

    ServiceList getPods(String namespace, String label1,  String label2);

    Pod getPod(String namespace, String name);

    String getPodId(String name);

    public String startPod(String imageName, String containerType, String parentId, String[] env,
                                 String[] netAliases, String[] command, boolean pullImage);

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
