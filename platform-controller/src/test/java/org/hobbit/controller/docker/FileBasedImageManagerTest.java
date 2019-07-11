package org.hobbit.controller.docker;

import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.junit.Assert;
import org.junit.Test;

/**
 * A simple test which tests the ability of the {@link FileBasedImageManager}
 * class to load metadata from a file.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class FileBasedImageManagerTest {

    private static final String METADATA_DIRECTORY = "src/test/resources/org/hobbit/controller";

    @Test(timeout = 20000)
    public void testBenchmark() throws InterruptedException {
        FileBasedImageManager manager = new FileBasedImageManager(METADATA_DIRECTORY);
        while (manager.getBenchmarks().isEmpty()) {
            Thread.sleep(100);
        }
        BenchmarkMetaData benchmark = manager.getBenchmark("http://example.org/GerbilBenchmark");
        Assert.assertNotNull(benchmark);
        Assert.assertEquals("GERBIL Benchmark", benchmark.getName());
    }

    @Test(timeout = 20000)
    public void testSystem() throws InterruptedException {
        FileBasedImageManager manager = new FileBasedImageManager(METADATA_DIRECTORY);
        while (manager.getSystems().isEmpty()) {
            Thread.sleep(100);
        }
        SystemMetaData system = manager.getSystem("http://example.org/DummySystem");
        Assert.assertNotNull(system);
        Assert.assertEquals("Dummy System", system.getName());
    }
}
