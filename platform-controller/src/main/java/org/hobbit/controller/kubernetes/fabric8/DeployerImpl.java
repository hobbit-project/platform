package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

public class DeployerImpl implements  Deployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);

    private KubernetesClient kubeClient;

    public DeployerImpl() {
        this.kubeClient = K8sUtility.kubeClient;
    }

    @Override
    public Deployment loadDeployment(String yaml_file) {
        try {
            Deployment deployment = kubeClient.apps().deployments().load(new FileInputStream(yaml_file)).get();
            return deployment;
        } catch (FileNotFoundException e) {
            LOGGER.debug(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Deployment loadDeployment(String name, String namespace) {
        namespace = K8sUtility.defaultNamespace(namespace);
        Deployment deployment = kubeClient.apps().deployments()
                                    .inNamespace(namespace)
                                    .withName(name).get();
        return deployment;
    }

    @Override
    public Deployment createDeployment(String name, String deployLabel1, String deployLabel2,
                                       String container, String image, String specLabel1, String specLabel2,
                                       String namespace, int replicaCount) {
        namespace = K8sUtility.defaultNamespace(namespace);
        Deployment deployment = new DeploymentBuilder()
            .withNewMetadata()
            .withName(name)
            .addToLabels(deployLabel1, deployLabel2)
            .endMetadata()
            .withNewSpec()
            .withReplicas(replicaCount)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels(specLabel1, specLabel2)
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName(container)
            .withImage(image)
            .withCommand("sleep","36000")
            .endContainer()
            .endSpec()
            .endTemplate()
            .withNewSelector()
            .addToMatchLabels(specLabel1,specLabel1)
            .endSelector()
            .endSpec()
            .build();

        kubeClient.apps().deployments().inNamespace(namespace).create(deployment);

        return deployment;
    }

    @Override
    public Deployment createOrReplace(Deployment deployObj, String namespace) {
        namespace = K8sUtility.defaultNamespace(namespace);
        Deployment deployment = kubeClient.apps().deployments().inNamespace(namespace).createOrReplace(deployObj);
        return deployment;
    }

    /*
    @Override
    public Deployment createOrReplace(Deployment deployObj, String namespace, int replicas, List<String> labels, List<String> specLabels) {
        return null;
    }
    */


}
