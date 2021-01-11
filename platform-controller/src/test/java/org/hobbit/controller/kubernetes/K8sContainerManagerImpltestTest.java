package org.hobbit.controller.kubernetes;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.hobbit.controller.docker.ContainerManagerImpl;
import org.hobbit.core.Constants;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class K8sContainerManagerImplTest {

    @Rule
    KubernetesServer server = new KubernetesServer();

    @Rule
    K8sContainerManagerImpl containerManager = new K8sContainerManagerImpl();

    KubernetesClient client = null;

    String imageName = "perl";
    String[] cmd = {"perl", "-Mbignum=bpi", "-wle", "print bpi(2000)"};
    @BeforeEach
    public void setUp(){
        server.before();
        containerManager.setK8sClient(server.getClient());
        client = containerManager.getK8sClient();
    }

    @Test
    public void startContainer(){
        String parentId = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null,
            cmd);
        assertNotNull(parentId);
        String containerId = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null,
            cmd);
        assertNotNull(containerId);

        final Job job = client.batch().jobs().withName(containerId).get();
        assertNotNull(job, "Service inspection response from Kubernetes");
        assertNotEquals(containerId, job.getMetadata().getUid());

        assertEquals(job.getMetadata().getLabels().get(ContainerManagerImpl.LABEL_TYPE), Constants.CONTAINER_TYPE_SYSTEM,
            "Type label of created swarm service" );
        assertEquals(parentId, job.getMetadata().getLabels().get(ContainerManagerImpl.LABEL_PARENT),
            "Parent label of created swarm service" );

        JobList tasks = client.batch().jobs().list();
        assertEquals(1, tasks.getItems().size(), "Amount of tasks of created kubernetes jobs");
        for (Job task : tasks.getItems()) {
            assertEquals( 1, task.getStatus().getSucceeded());
            Iterator iter = job.getSpec().getTemplate().getSpec().getContainers().iterator();
            Container container =(Container) iter.next();
            assertTrue(Arrays.equals(cmd, container.getArgs().toArray()), "Command of created container spec is sleepCommand");
        }
    }


}
