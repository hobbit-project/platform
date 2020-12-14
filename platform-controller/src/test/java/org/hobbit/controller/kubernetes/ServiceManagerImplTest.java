package org.hobbit.controller.kubernetes;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ServiceManagerImplTest {

    @Rule;
    public KubernetesServer server = new KubernetesServer(true, true);

    @Rule
    ServiceManagerImpl ServiceManager = new ServiceManagerImpl();
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);
    private KubernetesClient k8sClient;



    @BeforeEach
    void prepareService() {

        Service  service = new ServiceBuilder()
            .withNewMetadata()
            .withName("httpbin")
            .withLabels(Collections.singletonMap("app", "httpbin"))
            .endMetadata()
            .withNewSpec()
            .addNewPort()
            .withName("http")
            .withPort(5511)
            .withTargetPort(new IntOrString(8080))
            .endPort()
            .addToSelector("deploymentconfig", "httpbin")
            .endSpec()
            .build();
    }



    @Test
    void getService() {
        Service svc = k8sClient.services().load(getClass().getResourceAsStream("/test-service.yml")).get();
        assertNotNull(svc);
        assertEquals("httpbin", svc.getMetadata().getName());
    }


    @Test
    void testGetService() {
        Service responseSvc = k8sClient.services().inNamespace("test").create(Service);
        assertNotNull(responseSvc);
        assertEquals("httpbin", responseSvc.getMetadata().getName());

    }

    @Test
    void createService() {

        Service service1 = new ServiceBuilder().withNewMetadata().withName("svc1").and().withNewSpec().and().build();
        Service service2 = new ServiceBuilder().withNewMetadata().withName("svc2").addToLabels("foo", "bar").and().withNewSpec().and().build();
        Service service3 = new ServiceBuilder().withNewMetadata().withName("svc3").addToLabels("foo", "bar").and().withNewSpec().and().build();

        k8sClient.services().inNamespace("ns1").create(service1);
        k8sClient.services().inNamespace("ns2").create(service2);
        k8sClient.services().inNamespace("ns1").create(service3);

    }


    @Test
    ServiceList getServices() {
        //When
        ServiceList services = k8sClient.services().inAnyNamespace().list();
        return services;


        //Then
        ServiceList service = k8sClient.services().list();
        assertNotNull(service);
        assertEquals(0, size());

    }

    private void size() {
    }

    @Test
    ServiceList testGetServices() {
        //When
        namespace = K8sUtility.defaultNamespace(namespace);
        ServiceList services = k8sClient.services().inNamespace(namespace).withLabel(label1, label2).list();
        return services;

        //Then
        ServiceList   = k8sClient.services().inAnyNamespace().withLabels(Collections.singletonMap("label1", "label2")).list();
        assertNotNull(services);
        assertEquals(2, ServiceList.size());
    }



    @Test
    void deleteService() {
       //When
        Boolean isDeleted = k8sClient.services().inNamespace(namespace).withName(name).delete();

        //Then
        assertTrue(isDeleted);
    }

}
