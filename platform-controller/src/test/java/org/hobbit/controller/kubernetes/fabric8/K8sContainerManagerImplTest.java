package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class K8sContainerManagerImplTest extends K8sContainerManagerImpl {
    K8sContainerManagerImpl podmanager = null;

    @Before
    public void setUp() throws Exception {
        podmanager = new K8sContainerManagerImpl();
    }

    @Test
    public void testContainsVersionTag() {
    }

    @Test
    public void testCreateNewPod() {
    }

    @Test
    public void testGetPod() {
        final Pod pod = podmanager.getPod("/Users/shantanupathak/platform/platform-controller/src/main/java/org/hobbit/controller/kubernetes/fabric8/DummyPod.yaml");
        assertNotNull(pod);
    }

    @Test
    public void testGetPods() {
        final PodList podLst = podmanager.getPods();
        assertNotNull(podLst);
        assertEquals(0, podLst.getItems().size());
    }

    @Test
    public void testGetPods1() {
        final PodList podLst = podmanager.getPods("nginx");
        System.out.println(podLst);
        assertEquals("nginx" , podLst);
    }

    @Test
    public void testGetPods2() {
    }

    @Test
    public void testGetPod1() {
    }

    @Test
    public void testCreatePod() {
    }

    @Test
    public void testCreateOrReplacePod() {
    }

    @Test
    public void testEditPodAddLabel() {
    }

    @Test
    public void testGetPodLog() {
    }

    @Test
    public void testWatchPod() {
    }

    @Test
    public void testDeletePod() {
    }

    @Test
    public void testDeletePods() {
    }

    @Test
    public void testUploadToPod() {
    }

    @Test
    public void testReadFromPod() {
    }
}
