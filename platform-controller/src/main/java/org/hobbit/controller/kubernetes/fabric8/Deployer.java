package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;

import java.util.List;

public interface Deployer {

    Deployment loadDeployment(String yaml_file);

    Deployment loadDeployment(String name, String namespace);

    Deployment createDeployment(String name, String deployLabel1, String deployLabel2,
                                String container, String image, String specLabel1, String specLabel2,
                                String namespace, int replicaCount);

    Deployment createOrReplace(Deployment deployObj, String namespace);


    //Deployment createOrReplace(Deployment deployObj, String namespace, int replicas, List<String> labels, List<String> specLabels);

}
