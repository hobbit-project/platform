package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class K8sUtility {


    private static KubernetesClient k8sClient = null;

    protected K8sUtility() {
        // Exists only to defeat instantiation.
    }

    public static KubernetesClient getK8sClient(){
        if(k8sClient == null) {
            k8sClient = new DefaultKubernetesClient();
        }
        return k8sClient;
    }

    public static String defaultNamespace(String namespace){
        if (namespace == null)
            namespace = "default";
        return namespace;
    }
}
