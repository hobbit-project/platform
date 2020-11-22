package org.hobbit.controller.ochestration;

import org.hobbit.controller.ochestration.objects.ClusterInfo;

public interface ClusterManager {

    ClusterInfo getClusterInfo();

    long getNumberOfNodes();

    long getNumberOfNodes(String label);

    boolean isClusterHealthy();

    long getExpectedNumberOfNodes();

    void setTaskHistoryLimit(Integer taskHistoryLimit);

    int getTaskHistoryLimit();
}
