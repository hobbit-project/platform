package org.hobbit.controller.kubernetes;

import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.KubernetesClient;


public class NamespaceManagerImpl implements NamespaceManager {

    private KubernetesClient k8sClient;

    public NamespaceManagerImpl() {
        this.k8sClient = K8sUtility.getK8sClient();;
    }

    @Override
    public NamespaceList getNamespaces() {
        NamespaceList namespaceList = k8sClient.namespaces().withLabel(APP_CATEGORY_KEY, APP_CATEGORY_VALUE).list();
        return namespaceList;
    }

    @Override
    public Boolean deleteNamespaceResources() {
        Boolean isDeleted = k8sClient.namespaces().withLabel(APP_CATEGORY_KEY, APP_CATEGORY_VALUE).delete();
        return isDeleted;
    }


}
