package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.autoscaling.v1.Scale;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.apache.jena.assembler.JA.namespace;
import static org.junit.jupiter.api.Assertions.*;

class DeployerImplTest {

    @Rule
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);
    private KubernetesClient kubeClient;
    private String name;


    public void DeployerImpl() {
        this.kubeClient = K8sUtility.kubeClient;
    }
   // KubernetesClient kubeClient = K8sUtility.getK8sClient();
    public KubernetesServer server = new KubernetesServer();

    @Test
    void loadDeployment() {

        List<HasMetadata> list = kubeClient.load(getClass().getResourceAsStream("/valid-deployment-without-apiversion.json")).get();
        Deployment deployment = (Deployment) list.get(0);

        // Then
        assertNotNull(deployment);
        assertEquals("test", deployment.getMetadata().getName());
        assertEquals(1, deployment.getSpec().getReplicas());
        assertEquals(1, deployment.getSpec().getTemplate().getSpec().getContainers().size());
    }


    @Test
    void createDeployment() {

        namespace = K8sUtility.defaultNamespace(namespace);
        Deployment deployment = new DeploymentBuilder().withNewMetadata()
            .withName("deployment")
            .withNamespace(namespace)
            .addToLabels("label1", "label2")
            .endMetadata()
            .withNewSpec()
            .endSpec()
            .build();

        kubeClient.apps().deployments().inNamespace(namespace).create(deployObj);
    }

    @Test
    void createOrReplace() {
        Deployment deployment = new DeploymentBuilder().withNewMetadata().withName(name).withNamespace(namespace).build();
        KubernetesClient client = server.getClient();

        Deployment deployObj;
        Deployment result = client.apps().deployments().inNamespace(namespace).create(deployObj);
        assertNotNull(result);
        assertEquals("deployment1", result.getMetadata().getName());
    }


    @Test
    void testGetDeployments() {

        DeploymentList aDeploymentList = kubeClient.apps().deployments().inNamespace(namespace).list();
        assertNotNull(aDeploymentList);
        assertEquals(1, aDeploymentList.getItems().size());
    }

    @Test
    void scaleReplicas() {
    }

    @Test
    void deleteDeployment() {

        String namespace = K8sUtility.defaultNamespace(namespace);
        boolean bDeleted = kubeClient.apps().deployments().inNamespace(namespace).withName(name).delete();
        assertTrue(bDeleted);

    }

    @Test
    void scaleDeployment() {

        Scale scaleResponse  = kubeClient.apps().deployments().inNamespace(namespace).withName(name).scale(scale);
        assertEquals("bar", scaleResponse.getMetadata().getLabels().get("foo"));
    }

    @Test
    void getDeploymentLogs() {

        String namespace = K8sUtility.defaultNamespace(namespace);
        namespace = K8sUtility.defaultNamespace(namespace);

        // When
        String log = kubeClient.apps().deployments().inNamespace(namespace).withName(name).getLog();

        // Then
        assertNotNull(log);
        assertEquals("hello", log);
    }
}
