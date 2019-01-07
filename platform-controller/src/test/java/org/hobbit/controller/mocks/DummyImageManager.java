package org.hobbit.controller.mocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.apache.jena.rdf.model.ModelFactory;
import org.hobbit.controller.docker.ImageManager;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;

public class DummyImageManager implements ImageManager {

    public static final String BENCHMARK_NAME = "benchmark";
    public static final String SYSTEM_URI = "systemUri";

    @Override
    public List<BenchmarkMetaData> getBenchmarks() {
        List<BenchmarkMetaData> result = new ArrayList<>();
        BenchmarkMetaData meta = new BenchmarkMetaData();
        meta.uri = BENCHMARK_NAME;
        meta.name = BENCHMARK_NAME;
        meta.mainImage = BENCHMARK_NAME;
        meta.usedImages = new HashSet<>();
        meta.usedImages.add("benchmarkImage1");
        meta.usedImages.add("benchmarkImage2");
        meta.rdfModel = ModelFactory.createDefaultModel();
        result.add(meta);
        return result;
    }

    @Override
    public List<SystemMetaData> getSystems() {
        List<SystemMetaData> result = new ArrayList<>();
        SystemMetaData meta = new SystemMetaData();
        meta.uri = SYSTEM_URI;
        meta.name = meta.uri;
        meta.mainImage = "SystemImage";
        meta.usedImages = new HashSet<>();
        meta.usedImages.add("SystemImage1");
        meta.usedImages.add("SystemImage2");
        meta.rdfModel = ModelFactory.createDefaultModel();
        result.add(meta);
        meta = new SystemMetaData();
        meta.uri = "wrong_" + SYSTEM_URI;
        meta.name = meta.uri;
        meta.mainImage = "wrong_SystemImage";
        meta.usedImages = new HashSet<>();
        meta.usedImages.add("wrong_SystemImage1");
        meta.usedImages.add("wrong_SystemImage2");
        meta.rdfModel = ModelFactory.createDefaultModel();
        result.add(meta);
        return result;
    }
}
