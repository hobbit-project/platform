package org.hobbit.controller.kubernetes;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class K8sContainerManagerImplTest {

    @Rule
    KubernetesServer server = new KubernetesServer();

    @Rule
    K8sContainerManagerImpl containerManager = new K8sContainerManagerImpl();

    @BeforeEach
    public void setUp(){
        server.before();
    }

    @Test
    public void startContainer(){

    }


}
