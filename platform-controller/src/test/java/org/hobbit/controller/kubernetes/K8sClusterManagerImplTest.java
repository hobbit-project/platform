package org.hobbit.controller.kubernetes;


import io.fabric8.kubernetes.api.model.NodeListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.hobbit.controller.orchestration.objects.ClusterInfo;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class K8sClusterManagerImplTest {

    @Rule
    KubernetesServer server = new KubernetesServer();

    @Rule
    K8sClusterManagerImpl clusterManager = new K8sClusterManagerImpl();

    KubernetesClient client = null;

    @BeforeEach
    public void setUp(){
        server.before();
        server.expect().withPath("/api/v1/nodes").andReturn(200, new NodeListBuilder().addNewItem().withNewMetadata()
            .addToLabels("org.hobbit.workergroup","system").endMetadata().and()
            .addNewItem().and()
            .addNewItem().and()
            .build()).once();
        clusterManager.setK8sClient(server.getClient());
        client = clusterManager.getK8sClient();

    }

    @Test
    void getClusterInfo() {
        final ClusterInfo info = clusterManager.getClusterInfo();
        assertNotNull(info);
    }

    @Test
    void getNumberOfNodes() {
        long numberOfNodes = clusterManager.getNumberOfNodes();
        assertEquals(3, numberOfNodes);
    }

    @Test
    void getNumberOfNodesWithLabel() {
        // System.out.println(client.nodes().withLabelIn("org.hobbit.workergroup","system" ).list().getItems());
        long numberOfNodes = clusterManager.getNumberOfNodes("org.hobbit.workergroup=system");
        assertEquals(0, numberOfNodes);
    }

    @Test
    void isClusterHealthy() {
        boolean isHealthy = clusterManager.isClusterHealthy();
        assertTrue(isHealthy);
    }

    @Test
    void setTaskHistoryLimit() {
        server.expect().withPath("/apis/apps/v1/deployments").andReturn(200, new DeploymentListBuilder()
            .addNewItem().editOrNewSpec().withRevisionHistoryLimit(5).endSpec().and()
            .addNewItem().editOrNewSpec().withRevisionHistoryLimit(5).endSpec().and()
            .addNewItem().editOrNewSpec().withRevisionHistoryLimit(5).endSpec().and()
            .build()).once();

        clusterManager.setTaskHistoryLimit(0);
        Integer taskHistoryLimit = clusterManager.getTaskHistoryLimit();
        assertEquals(0, (long) taskHistoryLimit);
        //set back to default
        clusterManager.setTaskHistoryLimit(5);
    }

}
