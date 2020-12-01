package org.hobbit.controller.kubernetes;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources.NetworkAttachmentDefinition;

public class K8sUtility {


    private static KubernetesClient k8sClient = null;

    protected K8sUtility() {
        // Exists only to defeat instantiation.
    }

    public static KubernetesClient getK8sClient(){
        if(k8sClient == null) {
            k8sClient = new DefaultKubernetesClient();
        }
        KubernetesDeserializer.registerCustomKind("k8s.cni.cncf.io/v1", "NetworkAttachmentDefinition", NetworkAttachmentDefinition.class);
        return k8sClient;
    }

    public static String defaultNamespace(String namespace){
        if (namespace == null)
            namespace = "default";
        return namespace;
    }
}
