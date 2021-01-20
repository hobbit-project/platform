package org.hobbit.controller.kubernetes;

import io.fabric8.kubernetes.api.model.batch.CronJob;
import io.fabric8.kubernetes.api.model.batch.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.AssertFalse.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class K8sContainerManagerImplTest {

    @Rule
    KubernetesServer server = new KubernetesServer();

    @Rule
    K8sContainerManagerImpl containerManager = new K8sContainerManagerImpl();
    private KubernetesClient k8sClient;


    @BeforeEach
    public void setUp(){
        server.before();
    }

    @Test
    public JobBuilder startContainer(){

        return new JobBuilder()
            .withApiVersion("batch/v1")
            .withNewMetadata()
            .withName("job1")
            .withUid("3Dc4c8746c-94fd-47a7-ac01-11047c0323b4")
            .withLabels(Collections.singletonMap("label1", "maximum-length-of-63-characters"))
            .withAnnotations(Collections.singletonMap("annotation1", "some-very-long-annotation"))
            .endMetadata()
            .withNewSpec()
            .withNewTemplate()
            .withNewSpec()
            .addNewContainer()
            .withName("pi")
            .withImage("perl")
            .withArgs("perl", "-Mbignum=bpi", "-wle", "print bpi(2000)")
            .endContainer()
            .withRestartPolicy("Never")
            .endSpec()
            .endTemplate()
            .endSpec();
    }
}


    @Test
    void testStartContainer() {
    }

    @Test
    void testStartContainer1() {

        KubernetesClient k8sClient = server.getClient();

        CronJob cronjob = k8sClient.batch().cronjobs().withName(name).get();
        assertNotNull(1,CronJob);


    }

    @Test
    void testStartContainer2() {

        CronJob cronjob = k8sClient.batch().cronjobs().withName(name).get();
        assertNull(2,CronJob);

    }

    @Test
    void testStartContainer3() {

        CronJob  cronjob = k8sClient.batch().cronjobs().withName(name).get();
        assertNull(3,CronJob);

        }

    @Test
    void testStartContainer4() {

        CronJob cronjob = k8sClient.batch().cronjobs().withName(name).get();
        assertNull(4,CronJob);

        }

    @Test
    void testStartContainer5() {

        CronJob  cronjob = k8sClient.batch().cronjobs().withName(name).get();
        assertEquals(5, CronJob);
    }



    @Test
    void testStartContainer6() {

        CronJob cronjob = k8sClient.batch().cronjobs().withName(name).get();
        assertNull(6, CronJob);


        }

    @Test
    void testStartContainer7() {

        CronJob cronjob = k8sClient.batch().cronjobs().withName(name).get();
        assertNull(7, CronJob);


        }

    @Test
    void testStartContainer8() {

        CronJob cronjob = k8sClient.batch().cronjobs().withName(name).get();
        assertNull(8, CronJob);


    }

    @Test
    void stopContainer() {
    }

    @Test
    void removeContainer() {

        CronJob cronjob1 = new CronJobBuilder().withNewMetadata()
        .withNamespace("test")
        .withName("cronjob1")
        .withResourceVersion("1")
        .endMetadata()
        .withNewSpec()
        .endSpec()
        .withNewStatus()
        .endStatus()
        .build();

        Boolean deleted = k8sClient.batch().cronjobs().inAnyNamespace().delete(cronjob3);
        assertFalse(deleted);
    }

    @Test
    void stopParentAndChildren() {





    }



    @Test
    void removeParentAndChildren() {


        Boolean deleted = k8sClient.batch().cronjobs().inAnyNamespace().delete(cronjob1, cronjob2);
        assertTrue(deleted);

        deleted = k8sClient.batch().cronjobs().inAnyNamespace().delete(cronjob3);
        assertFalse(deleted);
    }


    }

    @Test
    void getContainerExitCode() {
    }

    @Test
    void getContainerInfo() {
    }

    @Test
    void getContainers() {
    }

    @Test
    void getContainerId() {
        server.expect().withPath("/apis/batch/v1beta1/namespaces/test/cronjobs?labelSelector=" + Utils.toUrlEncoded("key1=value1,key2=value2")).andReturn(200, new CronJobListBuilder().addNewItem().and()
        .build()).once();

        CronJobList cronJobList = client.batch().cronjobs()
        .withLabel("key1", "value1")

        assertNotNull(cronJobList);
        assertEquals(0, cronJobList.getItems().size());


        }

    @Test
    void getContainerName() {
    }

    @Test
    void addContainerObserver() {
    }

    @Test
    void pullImage() {

        kubernetesClient client = server.getClient();
        assertNotNull(client.batch().cronjobs().load(getClass().getResourceAsStream("/test-cronjob.yml")).get());
        }

    }

    @Test
    void getStats() {
    }

    @Test
    void containsVersionTag() {
    }
}
