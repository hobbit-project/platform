package org.hobbit.controller.kubernetes.fabric8;


import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import org.hobbit.controller.data.SetupHardwareInformation;
import org.hobbit.core.data.usage.ResourceUsageInformation;
public interface K8sResourceInformationCollector {

    public ResourceUsageInformation getSystemUsageInformation();

    public ResourceUsageInformation getUsageInformation(DeploymentList deployments);

    public SetupHardwareInformation getHardwareInformation();


}
