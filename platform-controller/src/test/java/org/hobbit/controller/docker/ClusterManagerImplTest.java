package org.hobbit.controller.docker;

import com.spotify.docker.client.messages.Info;
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
        final Info info = clusterManager.getClusterInfo();
        assertNotNull(info);
    }

    @Test
    public void getNumberOfNodes() throws Exception {
        Integer numberOfNodes = clusterManager.getNumberOfNodes();
        assertTrue(numberOfNodes == 1);
    }

    @Test
    public void isClusterHealthy() throws Exception {
        boolean isHealthy = clusterManager.isClusterHealthy();
        assertTrue(isHealthy);
    }


}