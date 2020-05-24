package org.hobbit.controller.kubernetes;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1PodList;

public interface K8sClusterManager {

    public V1PodList getPodsInfo() throws ApiException;

    public int getNumberOfNodes();
}
