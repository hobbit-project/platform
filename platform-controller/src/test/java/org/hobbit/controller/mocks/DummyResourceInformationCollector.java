package org.hobbit.controller.mocks;

import org.hobbit.controller.data.SetupHardwareInformation;
import org.hobbit.controller.orchestration.ResourceInformationCollector;
import org.hobbit.core.data.usage.ResourceUsageInformation;

public class DummyResourceInformationCollector implements ResourceInformationCollector {

    @Override
    public ResourceUsageInformation getSystemUsageInformation() {
        return null;
    };

    @Override
    public ResourceUsageInformation getUsageInformation() {
        return null;
    };

    @Override
    public SetupHardwareInformation getHardwareInformation() {
        return null;
    };

}
