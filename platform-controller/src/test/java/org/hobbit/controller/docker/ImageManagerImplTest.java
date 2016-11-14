package org.hobbit.controller.docker;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Timofey Ermilov on 22/09/16.
 */
public class ImageManagerImplTest {
    private ImageManagerImpl imageManager;

    @Before
    public void initObserver() throws InterruptedException {
        imageManager = new ImageManagerImpl();
        // wait for gitlab projects fetch
        Thread.sleep(3000);
    }

    @Test
    public void modelToBenchmarkMetaDataTest() throws Exception {
        // Model m = imageManager.getBenchmarkModel("test");
        // BenchmarkMetaData data = imageManager.modelToBenchmarkMetaData(m);
        // assertEquals(data.benchmarkName, "GERBIL Benchmark");
        // assertEquals(data.benchmarkDescription, "Example of a HOBBIT T3.2
        // benchmark based on GERBIL");
        // assertEquals(data.benchmarkUri,
        // "http://example.org/GerbilBenchmark");
    }

    @Test
    public void getBenchmarks() {
        List<BenchmarkMetaData> bs = imageManager.getBenchmarks();
        Assert.assertTrue(bs.size() > 0);
        BenchmarkMetaData gerbilBench = null;
        for (BenchmarkMetaData b : bs) {
            if (b.benchmarkUri.equals("http://example.org/GerbilBenchmark")) {
                gerbilBench = b;
            }
        }
        Assert.assertNotNull(gerbilBench);
        // System.out.println(b.benchmarkUri);
        List<SystemMetaData> systs = imageManager.getSystemsForBenchmark(gerbilBench.benchmarkUri);
        Assert.assertTrue(systs.size() > 0);
        // System.out.println(Arrays.toString(systs.toArray()));
        Model benchmarkModel = imageManager.getBenchmarkModel("http://example.org/GerbilBenchmark");
        Assert.assertNotNull(benchmarkModel);
        // System.out.println(benchmarkModel);
        Model systemModel = imageManager.getSystemModel("http://example.org/DummySystem");
        // System.out.println(systemModel);
        Assert.assertNotNull(systemModel);
        // check for systems of user
        List<SystemMetaData> systems = imageManager.getSystemsOfUser("DefaultHobbitUser");
        Assert.assertNotNull(systems);
    }
}
