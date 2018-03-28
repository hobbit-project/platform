package org.hobbit.controller.docker;

import static org.junit.Assert.assertNotNull;

import org.hobbit.core.Constants;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class ResourceInformationCollectorTest extends ContainerManagerBasedTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void test() throws Exception {
        environmentVariables.set(ResourceInformationCollector.PROMETHEUS_HOST_KEY, "localhost");
        environmentVariables.set(ResourceInformationCollector.PROMETHEUS_PORT_KEY,
                ResourceInformationCollector.PROMETHEUS_PORT_DEFAULT);

        String containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null,
                sleepCommand);
        assertNotNull(containerId);
        tasks.add(containerId);

        Thread.sleep(20000);

        ResourceInformationCollector collector = new ResourceInformationCollector(manager);
        ResourceUsageInformation usage = collector.getSystemUsageInformation();

        Assert.assertNotNull(usage);

        System.out.println("Got usage information " + usage.toString());

        Assert.assertNotNull(usage.getCpuStats());
        Assert.assertTrue(usage.getCpuStats().getTotalUsage() > 0);
        Assert.assertNotNull(usage.getMemoryStats());
        Assert.assertTrue(usage.getMemoryStats().getUsageSum() > 0);
        Assert.assertNotNull(usage.getDiskStats());
        Assert.assertTrue(usage.getDiskStats().getFsSizeSum() > 0);
    }
}
