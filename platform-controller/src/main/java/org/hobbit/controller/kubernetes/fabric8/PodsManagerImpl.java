package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

public class PodsManagerImpl implements PodsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);
    private KubernetesClient kubeClient;


    public PodsManagerImpl() {
        this.kubeClient = K8sUtility.kubeClient;
    }

    @Override
    public Pod getPod(String yaml_path){

        Pod pod = null;
        try {
            pod = kubeClient.pods().load(new FileInputStream(yaml_path)).get();
            return pod;
        } catch (FileNotFoundException e) {
            LOGGER.debug("Pod resource not available: "+ e.getMessage());
            //e.printStackTrace();
            return null;
        }
    }

    @Override
    public PodList getPods(String namespace) {
        namespace = K8sUtility.defaultNamespace(namespace);
        PodList podList = kubeClient.pods().inNamespace(namespace).list();

        return podList;
    }

    @Override
    public PodList getPods() {
        PodList podList = kubeClient.pods().inAnyNamespace().list();
        return podList;
    }

    @Override
    public PodList getPods(String namespace, String label1, String label2) {
        namespace = K8sUtility.defaultNamespace(namespace);

        PodList podList = kubeClient.pods().inNamespace(namespace)
                                    .withLabel(label1, label2)
                                    .list();
        return podList;
    }

    @Override
    public Pod getPod(String namespace, String name) {
        namespace = K8sUtility.defaultNamespace(namespace);
        Pod pod = kubeClient.pods().inNamespace(namespace).withName(name).get();
        return pod;
    }

    @Override
    public void createPod(String name, String container_name, String image, int port, String namespace) {
        namespace = K8sUtility.defaultNamespace(namespace);

        Pod aPod = new PodBuilder().withNewMetadata().withName(name).endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName(container_name)
            .withImage(image)
            .addNewPort().withContainerPort(port).endPort()
            .endContainer()
            .endSpec()
            .build();
        Pod createdPod = kubeClient.pods().inNamespace(namespace).create(aPod);
    }

    @Override
    public void createOrReplacePod(String namespace, Pod pod) {
        namespace = K8sUtility.defaultNamespace(namespace);
        kubeClient.pods().inNamespace(namespace).createOrReplace(pod);
    }

    @Override
    public void editPodAddLabel(String namespace, String name, String label, String new_label) {
        namespace = K8sUtility.defaultNamespace(namespace);
        kubeClient.pods().inNamespace(namespace).withName(name).edit()
            .editOrNewMetadata().addToLabels(new_label,label).endMetadata().done();
    }

    @Override
    public String getPodLog(String name, String namespace) {
        namespace = K8sUtility.defaultNamespace(namespace);
        String log = kubeClient.pods().inNamespace(namespace).withName(name).getLog();
        return log;
    }

    @Override
    public LogWatch watchPod(String name, String namespace) {
        namespace = K8sUtility.defaultNamespace(namespace);
        LogWatch watch = kubeClient.pods().inNamespace(namespace).withName(name).tailingLines(10).watchLog(System.out);
        return watch;
    }

    @Override
    public Boolean deletePod(String namespace, String name) {
        namespace = K8sUtility.defaultNamespace(namespace);
        Boolean isDeleted = kubeClient.pods().inNamespace(namespace).withName(name).delete();
        return isDeleted;
    }

    @Override
    public Boolean deletePods(String namespace, List<Pod> pods) {
        namespace = K8sUtility.defaultNamespace(namespace);
        Boolean isDeleted = kubeClient.pods().inNamespace(namespace).delete(pods);
        return isDeleted;
    }





}
