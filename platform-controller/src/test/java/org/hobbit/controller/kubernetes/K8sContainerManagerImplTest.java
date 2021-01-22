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

import static org.junit.Assert.assertNotNull;
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
    public void setUp() {
        server.before();
        containerManager.setK8sClient(server.getClient());
        client = containerManager.getK8sClient();
    }

    @Test
    public void startContainer() {
        String parentId = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null,
            cmd);
        assertNotNull(parentId);
        String containerId = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null,
            cmd);
        assertNotNull(containerId);

        final Job job = client.batch().jobs().withName(containerId).get();
        assertNotNull("Service inspection response from Kubernetes",job);
        assertNotEquals(containerId, job.getMetadata().getUid());

        assertEquals(job.getMetadata().getLabels().get(ContainerManagerImpl.LABEL_TYPE), Constants.CONTAINER_TYPE_SYSTEM,
            "Type label of created swarm service");
        assertEquals(parentId, job.getMetadata().getLabels().get(ContainerManagerImpl.LABEL_PARENT),
            "Parent label of created swarm service");

        JobList tasks = client.batch().jobs().list();
        assertEquals(1, tasks.getItems().size(), "Amount of tasks of created kubernetes jobs");
        for (Job task : tasks.getItems()) {
            assertEquals(1, task.getStatus().getSucceeded());
            Iterator iter = job.getSpec().getTemplate().getSpec().getContainers().iterator();
            Container container = (Container) iter.next();
            assertTrue(Arrays.equals(cmd, container.getArgs().toArray()), "Command of created container spec is sleepCommand");
        }
    }

    @Test
    public void startContainerWithoutCommand() {
        String containerId = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null);
        assertNotNull(containerId);
        JobList tasks = client.batch().jobs().list();
        assertEquals(1, tasks.getItems().size(), "Amount of tasks of created kubernetes jobs");
        for (Job task : tasks.getItems()) {
            Iterator iter = task.getSpec().getTemplate().getSpec().getContainers().iterator();
            Container container = (Container) iter.next();
            assertNull(container.getCommand());
        }
    }

    @Test
    public void removeContainer() {
        // start new test container
        String testContainer = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null, cmd);
        assertNotNull(testContainer);
        containerManager.removeContainer(testContainer);
        JobList tasks = client.batch().jobs().list();
        assertEquals(0, tasks.getItems().size(), "number of running jobs");
    }

    @Test
    public void removeParentAndChildren() {
        // start new test container
        String topParent = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null, cmd);
        assertNotNull(topParent);
        String child1 = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, topParent, cmd);
        assertNotNull(child1);

        String subParent = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, topParent,
            cmd);
        assertNotNull(subParent);
        String subChild = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, subParent,
            cmd);
        assertNotNull(subChild);
        String unrelatedParent = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null, cmd);
        assertNotNull(unrelatedParent);
        String unrelatedChild = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, unrelatedParent,
            cmd);
        assertNotNull(unrelatedChild);

        JobList tasks = client.batch().jobs().list();
        tasks.getItems().forEach(task -> {
            assertEquals(1, task.getStatus().getActive());
        });
    }

    @Test
    public void getContainerInfo(){
        // start new test container
        String testContainer = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null, cmd);
        assertNotNull(testContainer);
        Job infoFromManager =containerManager.getContainerInfo(testContainer);
        Job containerInfo = client.batch().jobs().inNamespace("default").withName(testContainer).get();
        containerManager.removeContainer(testContainer);
        assertEquals(infoFromManager.getMetadata().getUid(), containerInfo.getMetadata().getUid());
    }

    @Test
    public void getContainerIdAndName() {
        String containerId = containerManager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null, cmd);
        assertNotNull(containerId);
        String containerName = containerManager.getContainerName(containerId);
        assertEquals(containerId, containerManager.getContainerId(containerName));
    }




}

