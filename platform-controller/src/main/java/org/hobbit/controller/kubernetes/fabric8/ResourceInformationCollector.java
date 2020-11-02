package org.hobbit.controller.kubernetes.fabric8;


import org.hobbit.controller.data.SetupHardwareInformation;
import org.hobbit.core.data.usage.ResourceUsageInformation;
public interface ResourceInformationCollector {

    public ResourceUsageInformation getSystemUsageInformation();

    public ResourceUsageInformation getUsageInformation( );

    public SetupHardwareInformation getHardwareInformation();


}
