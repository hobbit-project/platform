package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.NodeStatus;

public interface K8sClusterManager {

    public NodeStatus getClusterNodeInfo(String nodeName);

    public long getNumberOfNodes();

    public long getNumberOfNodes(String label);

    public boolean isClusterHealthy();

    public long getExpectedNumberOfNodes();


}
