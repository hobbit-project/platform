package org.hobbit.controller.docker;

import static org.junit.Assert.assertNotNull;

import org.hobbit.core.Constants;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.junit.Assert;
import org.junit.Test;

public class ResourceInformationCollectorTest extends ContainerManagerBasedTest {

    @Test
    public void test() throws Exception {
        String containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null,
                sleepCommand);
        assertNotNull(containerId);
        containers.add(containerId);

        ResourceInformationCollector collector = new ResourceInformationCollector(dockerClient, manager);
        ResourceUsageInformation usage = collector.getSystemUsageInformation();

        Assert.assertNotNull(usage);

        Assert.assertNotNull(usage.getCpuStats());
        Assert.assertTrue(usage.getCpuStats().getTotalUsage() > 0);
        Assert.assertNotNull(usage.getMemoryStats());
        Assert.assertTrue(usage.getMemoryStats().getUsageSum() > 0);
        Assert.assertNotNull(usage.getDiskStats());
        Assert.assertTrue(usage.getDiskStats().getFsSizeSum() > 0);
    }
}
