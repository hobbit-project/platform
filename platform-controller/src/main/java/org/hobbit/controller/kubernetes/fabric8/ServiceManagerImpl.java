package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ServiceManagerImpl implements ServiceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);
    private KubernetesClient k8sClient;

    public ServiceManagerImpl() {
        this.k8sClient = K8sUtility.getK8sClient();
    }

    @Override
    public Service getService(String yaml_file) {
        try {
            Service service = k8sClient.services().load(new FileInputStream(yaml_file)).get();
            return service;
        } catch (FileNotFoundException e) {
            LOGGER.debug(e.getMessage());
            return null;
            // e.printStackTrace();
        }

    }

    @Override
    public Service getService(String namespace, String name) {
        namespace = K8sUtility.defaultNamespace(namespace);
        Service service = k8sClient.services().inNamespace(namespace).withName(name).get();

        return service;
    }

    @Override
    public Service createService(String serviceName, String protocol, String portName,
                                 int port, int targetPort, String type, String IP,
                                 String namespace) {

        Service service = new ServiceBuilder()
            .withNewMetadata()
            .withName(serviceName)
            .endMetadata()
            .withNewSpec()
            .addNewPort()
            .withName(portName)
            .withProtocol(protocol)
            .withPort(port)
            .withTargetPort(new IntOrString(targetPort))
            .endPort()
            .withType(type)
            .endSpec()
            .withNewStatus()
            .withNewLoadBalancer()
            .addNewIngress()
            .withIp(IP)
            .endIngress()
            .endLoadBalancer()
            .endStatus()
            .build();

        service = k8sClient.services().inNamespace(k8sClient.getNamespace()).create(service);
        return service;
    }

    @Override
    public ServiceList getServices() {
        ServiceList services = k8sClient.services().inAnyNamespace().list();
        return services;
    }

    @Override
    public ServiceList getServices(String namespace, String label1, String label2) {
        namespace = K8sUtility.defaultNamespace(namespace);
        ServiceList services = k8sClient.services().inNamespace(namespace).withLabel(label1, label2).list();
        return services;
    }

    @Override
    public Boolean deleteService(String namespace, String name) {
        Boolean isDeleted = k8sClient.services().inNamespace(namespace).withName(name).delete();
        return isDeleted;
    }


}
