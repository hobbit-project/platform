package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class K8sUtility {
    // current placeholder dummy URL to be changed
    public static final String MASTER_URL = "https://192.168.42.20:8443/";

    public static Config kubeConfig = new ConfigBuilder()
        .withMasterUrl(K8sUtility.MASTER_URL)
        .build();

    public static KubernetesClient kubeClient =new DefaultKubernetesClient(kubeConfig);

    public static String defaultNamespace(String namespace){
        if (namespace == null)
            namespace = "default";
        return namespace;
    }
}
