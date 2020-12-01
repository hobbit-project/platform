package org.hobbit.controller.orchestration;

import org.hobbit.controller.data.SetupHardwareInformation;
import org.hobbit.core.data.usage.ResourceUsageInformation;

public interface ResourceInformationCollector {

    ResourceUsageInformation getSystemUsageInformation();

    ResourceUsageInformation getUsageInformation();

    SetupHardwareInformation getHardwareInformation();
}
