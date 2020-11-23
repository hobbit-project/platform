package org.hobbit.controller.orchestration;

import org.hobbit.controller.orchestration.objects.ClusterInfo;

public interface ClusterManager {

    ClusterInfo getClusterInfo();

    long getNumberOfNodes();

    long getNumberOfNodes(String label);

    boolean isClusterHealthy();

    long getExpectedNumberOfNodes();

    void setTaskHistoryLimit(Integer taskHistoryLimit);

    int getTaskHistoryLimit();
}
