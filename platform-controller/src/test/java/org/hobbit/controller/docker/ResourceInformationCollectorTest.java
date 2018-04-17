package org.hobbit.controller.docker;

import static org.junit.Assert.assertNotNull;

import org.hobbit.core.Constants;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceInformationCollectorTest extends ContainerManagerBasedTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceInformationCollectorTest.class);

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

        Thread.sleep(10000);

        ResourceInformationCollector collector = new ResourceInformationCollector(manager);
        ResourceUsageInformation usage = collector.getSystemUsageInformation();

        Assert.assertNotNull(usage);
        LOGGER.info("Got usage information {}", usage);

        Assert.assertNotNull(usage.getCpuStats());
        Assert.assertTrue(usage.getCpuStats().getTotalUsage() > 0);
        Assert.assertNotNull(usage.getMemoryStats());
        Assert.assertTrue(usage.getMemoryStats().getUsageSum() > 0);
        Assert.assertNotNull(usage.getDiskStats());
        Assert.assertTrue(usage.getDiskStats().getFsSizeSum() > 0);

        // Generate a second container
        containerId = manager.startContainer(busyboxImageName, Constants.CONTAINER_TYPE_SYSTEM, null, sleepCommand);
        assertNotNull(containerId);
        tasks.add(containerId);

        Thread.sleep(10000);

        ResourceUsageInformation usage2 = collector.getSystemUsageInformation();
        Assert.assertNotNull(usage2);
        LOGGER.info("Got usage information {}", usage2);

        Assert.assertNotNull(usage2.getCpuStats());
        Assert.assertTrue(usage2.getCpuStats().getTotalUsage() > 0);
        Assert.assertTrue(usage.getCpuStats().getTotalUsage() <= usage2.getCpuStats().getTotalUsage());
        Assert.assertNotNull(usage2.getMemoryStats());
        Assert.assertTrue(usage.getMemoryStats().getUsageSum() <= usage2.getMemoryStats().getUsageSum());
        Assert.assertNotNull(usage2.getDiskStats());
        Assert.assertTrue(usage.getDiskStats().getFsSizeSum() <= usage2.getDiskStats().getFsSizeSum());
    }   // "dd if=/dev/zero of=file.txt count=1024 bs=1048576"
}
