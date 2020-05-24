package org.hobbit.controller.kubernetes;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1PodList;

public interface K8sClusterManager {

    //sujay
    public V1PodList getPodsInfo() throws ApiException;

    //sujay
    public long getNumberOfNodes();

    //Sam
    public int getNumberOfNodes(String label);


}
