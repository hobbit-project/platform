package org.hobbit.controller.docker;

import com.spotify.docker.client.messages.Info;
import org.hobbit.controller.ochestration.objects.ClusterInfo;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClusterManagerImplTest {

    ClusterManagerImpl clusterManager = null;

    @Before
    public void setUp() throws Exception {
        clusterManager = new ClusterManagerImpl();
    }

    @Test
    public void getClusterInfo() throws Exception {
        final ClusterInfo info = clusterManager.getClusterInfo();
        assertNotNull(info);
    }

    @Test
    public void getNumberOfNodes() throws Exception {
        long numberOfNodes = clusterManager.getNumberOfNodes();
        assertEquals(1, numberOfNodes);
    }

    @Test
    public void getNumberOfBenchmarkNodes() throws Exception {
        long numberOfNodes = clusterManager.getNumberOfNodes("org.hobbit.workergroup=benchmark");
        assertEquals(0, numberOfNodes);
    }

    @Test
    public void getNumberOfSystemNodes() throws Exception {
        long numberOfNodes = clusterManager.getNumberOfNodes("org.hobbit.workergroup=system");
        assertEquals(0, numberOfNodes);
    }

    @Test
    public void isClusterHealthy() throws Exception {
        boolean isHealthy = clusterManager.isClusterHealthy();
        assertTrue(isHealthy);
    }

    @Test
    public void setTaskHistoryLimit() throws Exception {
        clusterManager.setTaskHistoryLimit(0);
        Integer taskHistoryLimit = clusterManager.getTaskHistoryLimit();
        assertEquals(0, (long) taskHistoryLimit);
        //set back to default
        clusterManager.setTaskHistoryLimit(5);
    }


}
