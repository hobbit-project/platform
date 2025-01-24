package org.hobbit.controller.kubernetes;

import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Info;
import org.hobbit.controller.interfaces.ClusterManager;

public class ClusterManagerImpl implements ClusterManager {
    @Override
    public Info getClusterInfo() throws DockerException, InterruptedException {
        return null;
    }

    @Override
    public long getNumberOfNodes() throws DockerException, InterruptedException {
        return 0;
    }

    @Override
    public long getNumberOfNodes(String label) throws DockerException, InterruptedException {
        return 0;
    }

    @Override
    public boolean isClusterHealthy() throws DockerException, InterruptedException {
        return false;
    }

    @Override
    public long getExpectedNumberOfNodes() {
        return 0;
    }

    @Override
    public void setTaskHistoryLimit(Integer taskHistoryLimit) throws DockerException, InterruptedException {

    }

    @Override
    public int getTaskHistoryLimit() throws DockerException, InterruptedException {
        return 0;
    }
}
