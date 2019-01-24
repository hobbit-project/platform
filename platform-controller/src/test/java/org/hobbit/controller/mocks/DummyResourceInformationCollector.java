package org.hobbit.controller.mocks;

import com.spotify.docker.client.messages.swarm.Service;
import org.hobbit.controller.data.SetupHardwareInformation;
import org.hobbit.controller.docker.ResourceInformationCollector;
import org.hobbit.core.data.usage.ResourceUsageInformation;

public class DummyResourceInformationCollector implements ResourceInformationCollector {

    @Override
    public ResourceUsageInformation getSystemUsageInformation() {
        return null;
    };

    @Override
    public ResourceUsageInformation getUsageInformation(Service.Criteria criteria) {
        return null;
    };

    @Override
    public SetupHardwareInformation getHardwareInformation() {
        return null;
    };

}
