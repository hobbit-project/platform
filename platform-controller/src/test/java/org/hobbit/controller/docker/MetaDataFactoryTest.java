package org.hobbit.controller.docker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.lf5.util.StreamUtils;
import org.hobbit.controller.ParameterForwardingTest;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.ImageMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.junit.Assert;
import org.junit.Test;

public class MetaDataFactoryTest {

    @Test
    public void testModelToBenchmarkMetaData() throws IOException {
        BenchmarkMetaData expectedMetaData = new BenchmarkMetaData();
        expectedMetaData.uri = "http://example.org/GerbilBenchmark";
        expectedMetaData.name = "GERBIL Benchmark";
        expectedMetaData.description = "Example of a HOBBIT T3.2 benchmark based on GERBIL";
        expectedMetaData.definedApis = new HashSet<>();
        expectedMetaData.definedApis.add("http://example.org/GerbilApi");
        expectedMetaData.mainImage = "hobbit/gerbil_controller";
        expectedMetaData.usedImages = new HashSet<>();
        expectedMetaData.usedImages.add("hobbit/gerbil_datagen");
        expectedMetaData.usedImages.add("hobbit/gerbil_taskgen");
        expectedMetaData.date = new Date(10);
        expectedMetaData.source = "test";

        InputStream input = ParameterForwardingTest.class.getClassLoader()
                .getResourceAsStream("org/hobbit/controller/benchmark.ttl");
        Assert.assertNotNull(input);
        byte modelDat[] = StreamUtils.getBytes(input);
        IOUtils.closeQuietly(input);

        expectedMetaData.rdfModel = MetaDataFactory.byteArrayToModel(modelDat, "TTL");

        List<BenchmarkMetaData> results = MetaDataFactory.modelToBenchmarkMetaData(expectedMetaData.rdfModel,
                expectedMetaData.source, expectedMetaData.date);
        Assert.assertEquals(1, results.size());
        compareMetaData(expectedMetaData, results.get(0));
        String[] expectedApis = expectedMetaData.definedApis.toArray(new String[expectedMetaData.definedApis.size()]);
        Arrays.sort(expectedApis);
        String[] actualApis = results.get(0).definedApis.toArray(new String[0]);
        Arrays.sort(actualApis);
        Assert.assertArrayEquals(expectedApis, actualApis);
    }

    @Test
    public void testModelToSystemMetaData() throws IOException {
        SystemMetaData expectedMetaData = new SystemMetaData();
        expectedMetaData.uri = "http://example.org/DummySystem";
        expectedMetaData.name = "Dummy System";
        expectedMetaData.description = "Example of a HOBBIT T3.2 system that is compatible to the GERBIL benchmark";
        expectedMetaData.implementedApis = new HashSet<>();
        expectedMetaData.implementedApis.add("http://example.org/GerbilApi");
        expectedMetaData.mainImage = "gerbil_dummy_system";
        expectedMetaData.usedImages = new HashSet<>();
        expectedMetaData.usedImages.add("gerbil_dummy_system_part2");
        expectedMetaData.date = new Date(10);
        expectedMetaData.source = "test";

        InputStream input = ParameterForwardingTest.class.getClassLoader()
                .getResourceAsStream("org/hobbit/controller/dummySystem.ttl");
        Assert.assertNotNull(input);
        byte modelDat[] = StreamUtils.getBytes(input);
        IOUtils.closeQuietly(input);

        expectedMetaData.rdfModel = MetaDataFactory.byteArrayToModel(modelDat, "TTL");

        List<SystemMetaData> results = MetaDataFactory.modelToSystemMetaData(expectedMetaData.rdfModel,
                expectedMetaData.source, expectedMetaData.date);
        Assert.assertEquals(1, results.size());
        compareMetaData(expectedMetaData, results.get(0));
        String[] expectedApis = expectedMetaData.implementedApis
                .toArray(new String[expectedMetaData.implementedApis.size()]);
        Arrays.sort(expectedApis);
        String[] actualApis = results.get(0).implementedApis.toArray(new String[0]);
        Arrays.sort(actualApis);
        Assert.assertArrayEquals(expectedApis, actualApis);
    }

    public static void compareMetaData(ImageMetaData expected, ImageMetaData actual) {
        Assert.assertEquals(expected.uri, actual.uri);
        Assert.assertEquals(expected.name, actual.name);
        Assert.assertEquals(expected.description, actual.description);
        Assert.assertEquals(expected.mainImage, actual.mainImage);
        Assert.assertEquals(expected.rdfModel, actual.rdfModel);
        Assert.assertEquals(expected.source, actual.source);
        Assert.assertEquals(expected.date, actual.date);
        Assert.assertEquals(expected.defError, actual.defError);
        String[] expectedImages = expected.usedImages.toArray(new String[expected.usedImages.size()]);
        Arrays.sort(expectedImages);
        String[] actualImages = actual.usedImages.toArray(new String[actual.usedImages.size()]);
        Arrays.sort(actualImages);
        Assert.assertArrayEquals(expectedImages, actualImages);
    }
}
