package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.apps.DeploymentListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import junit.framework.TestCase;
import org.hobbit.controller.orchestration.ClusterManager;
import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;


public class K8sClusterManagerImplTest extends TestCase {

    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    private KubernetesClient k8sClient;

    ClusterManager clusterManager = null;

    @Override
    public void setUp(){
        k8sClient = server.getClient();

        clusterManager = new K8sClusterManagerImpl();

        server.expect().withPath("/api/v1/nodes/node1").andReturn(200, new PodBuilder().build()).once();
        server.expect().withPath("/api/v1/nodes/node2").andReturn(200, new PodBuilder().build()).once();
        server.expect().withPath("/api/v1/nodes/node3").andReturn(200, new PodBuilder().build()).once();

    }

    @Test
    public void testGetClusterInfo() {
    }

    @Test
    public void testGetNumberOfNodes() {
        NodeList nodeList = k8sClient.nodes().list();
        long numberOfNodes = clusterManager.getNumberOfNodes();
        assertEquals(3, numberOfNodes);
    }
    @Test
    public void testIsClusterHealthy() {
        boolean isHealthy = clusterManager.isClusterHealthy();
        assertTrue(isHealthy);
    }
    @Test
    public void testSetTaskHistoryLimit() {
        Integer taskRevisionHistory = 10;
        server.expect().withPath("/apis/apps/v1/namespaces/test/deployments").andReturn(200, new DeploymentListBuilder().build()).once();
        clusterManager.setTaskHistoryLimit(taskRevisionHistory);
        DeploymentList deployments = k8sClient.apps().deployments().list();

        Iterator it = deployments.getItems().iterator();
        Deployment first = (Deployment) it.next();
        assertEquals(taskRevisionHistory, first.getSpec().getRevisionHistoryLimit());
    }

}
