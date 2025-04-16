package org.hobbit.controller.containers;

import io.kubernetes.client.openapi.models.V1Pod;

public interface KubExtendedContainerManager extends ContainerManager {
    public V1Pod getPod(String podIP);
}
