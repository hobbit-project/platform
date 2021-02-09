package org.hobbit.controller.orchestration;

import org.hobbit.controller.data.SetupHardwareInformation;
import org.hobbit.core.data.usage.ResourceUsageInformation;

/**
 * A class that can collect resource usage information.
 *
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public interface ResourceInformationCollector {

    ResourceUsageInformation getSystemUsageInformation();

    ResourceUsageInformation getUsageInformation();

    SetupHardwareInformation getHardwareInformation();
}
