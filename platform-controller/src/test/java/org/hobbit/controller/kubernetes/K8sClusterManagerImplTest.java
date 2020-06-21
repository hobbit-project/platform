package org.hobbit.controller.kubernetes;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1PodList;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class K8sClusterManagerImplTest {

    K8sClusterManagerImpl clusterManager;

    @Before
    public void setUp() {
        try {
            System.out.print("Into the Setup");
            //ApiClient k8sclient = ClientBuilder.standard().build();
            //Configuration.setDefaultApiClient(k8sclient);
            clusterManager = new K8sClusterManagerImpl();
        } catch (IOException | ApiException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetPodsInfo() {
        try {
            System.out.print("Into the testGetPodsInfo");
            final V1PodList info = clusterManager.getPodsInfo();
            assertNotNull(info);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGetNumberOfNodes() {
        System.out.print("Into the testGetNumberOfNodes");
        long numberOfNodes = clusterManager.getNumberOfNodes();
        assertEquals(1, numberOfNodes);
    }

    @Test
    public void testIsClusterHealthy() {
        System.out.print("Into the testIsClusterHealthy");
        boolean isHealthy = clusterManager.isClusterHealthy();
        assertTrue(isHealthy);
    }

}
