/**
 * This file is part of platform-controller.
 *
 * platform-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * platform-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with platform-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.controller.docker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.hobbit.controller.data.ExtBenchmarkMetaData;
import org.hobbit.controller.data.ExtSystemMetaData;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class FileBasedImageManager implements ImageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageManagerImpl.class);

    private static final String DEFAULT_DEF_FOLDER = "metadata";

    private final String inputFolder;
    private Timer timer;
    private int repeatInterval = 60 * 1000; // every 1 min
    private Map<String, ExtBenchmarkMetaData> benchmarks = null;
    private Map<String, ExtSystemMetaData> systems = null;

    public FileBasedImageManager() {
        this(DEFAULT_DEF_FOLDER);
    }

    public FileBasedImageManager(String inputFolder) {
        this.inputFolder = inputFolder;
        startFetchingFiles();
    }

    // public void runWhenGitlabIsReady(Runnable r) {
    // gitlab.runAfterFirstFetch(r);
    // }

    protected void startFetchingFiles() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Map<String, ExtBenchmarkMetaData> newBenchmarks = new HashMap<>();
                Map<String, ExtSystemMetaData> newSystems = new HashMap<>();

                try {
                    // Get a list of *.ttl files
                    Collection<File> files = FileUtils.listFiles(new File(inputFolder), new String[] { "ttl" }, false);
                    // Go through the list and try to read the files
                    for (File f : files) {
                        readFile(f, newBenchmarks, newSystems);
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception while reading ", e);
                }

                if (benchmarks == null) {
                    // This is the first fetching of projects -> we might have
                    // to notify threads that are waiting for that
                    benchmarks = newBenchmarks;
                    systems = newSystems;
                    synchronized (this) {
                        this.notifyAll();
                    }
                } else {
                    // update cached version
                    benchmarks = newBenchmarks;
                    systems = newSystems;
                }
                // // indicate that projects were fetched
                // if (!projectsFetched) {
                // projectsFetched = true;
                // for (Runnable r : readyRunnable) {
                // r.run();
                // }
                // }
            }
        }, 0, repeatInterval);
    }

    protected void readFile(File f, Map<String, ExtBenchmarkMetaData> newBenchmarks,
            Map<String, ExtSystemMetaData> newSystems) {
        String fileContent = null;
        try {
            fileContent = FileUtils.readFileToString(f, Charsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Couldn't read {}. It will be ignored.", f.getAbsolutePath());
            return;
        }
        Model model = null;
        try {
            model = stringToModel(fileContent);
        } catch (Exception e) {
            LOGGER.error("Couldn't parse " + f.getAbsolutePath() + ". It will be ignored.", e);
            return;
        }
        List<Resource> benchmarkResources = RdfHelper.getSubjectResources(model, RDF.type, HOBBIT.Benchmark);
        List<Resource> systemResources = RdfHelper.getSubjectResources(model, RDF.type, HOBBIT.SystemInstance);
        // If the file contains at least one benchmark
        if (benchmarkResources.size() > 0) {
            // If it contains more than one benchmark
            if (benchmarkResources.size() > 1) {
                LOGGER.info(
                        "File {} contains more than one benchmark. This is not supported. The file will be ignored.",
                        f.getAbsolutePath());
                return;
                // If it contains one system beside the benchmark
            } else if (systemResources.size() > 0) {
                LOGGER.info(
                        "File {} contains a benchmark and one or more systems. This is not supported. The file will be ignored.",
                        f.getAbsolutePath());
            } else {
                try {
                    newBenchmarks.put(benchmarkResources.get(0).getURI(),
                            modelToBenchmarkMetaData(model, benchmarkResources.get(0)));
                } catch (Exception e) {
                    LOGGER.error("Couldn't process the model in " + f.getAbsolutePath() + ". It will be ignored.", e);
                    return;
                }
            }
        } else if (systemResources.size() > 0) {
            // If it contains more than one benchmark
            if (systemResources.size() > 1) {
                LOGGER.info("File {} contains more than one system. This is not supported. The file will be ignored.",
                        f.getAbsolutePath());
                return;
            } else {
                try {
                    List<SystemMetaData> metaData = modelToSystemMetaData(model);
                    for (SystemMetaData system : metaData) {
                        newSystems.put(system.uri, (ExtSystemMetaData) system);
                    }
                } catch (Exception e) {
                    LOGGER.error("Couldn't process the model in " + f.getAbsolutePath() + ". It will be ignored.", e);
                    return;
                }
            }
        }
    }

    protected static Model stringToModel(String modelString) {
        // convert string to model
        Model m = ModelFactory.createDefaultModel();
        InputStream stream = new ByteArrayInputStream(modelString.getBytes(Charsets.UTF_8));
        m.read(stream, null, "TTL");
        return m;
    }

    protected static ExtBenchmarkMetaData modelToBenchmarkMetaData(Model model, Resource benchmark) throws Exception {
        ExtBenchmarkMetaData result = new ExtBenchmarkMetaData();

        // set URI
        result.uri = benchmark.getURI();
        // find name
        result.name = getName(model, benchmark);
        // find description
        result.description = getDescription(model, benchmark);
        // find APIs
        result.definedApis = getAPIs(model, benchmark, true);

        result.model = model;

        return result;
    }

    public List<SystemMetaData> modelToSystemMetaData(String modelString) throws Exception {
        // execute default method on new model
        return modelToSystemMetaData(stringToModel(modelString));
    }

    public List<SystemMetaData> modelToSystemMetaData(Model model) throws Exception {
        List<SystemMetaData> results = new ArrayList<>();

        // find all system subjects
        List<Resource> systems = RdfHelper.getSubjectResources(model, RDF.type, HOBBIT.SystemInstance);
        for (Resource system : systems) {
            ExtSystemMetaData result = new ExtSystemMetaData();
            // set URI
            result.uri = system.getURI();
            // find name
            result.name = getName(model, system);
            // find description
            result.description = getDescription(model, system);
            // find image name
            result.mainImage = getImage(model, system);
            // find APIs
            result.implementedApis = getAPIs(model, system, false);
            // FIXME We should query the part of the model that is important for
            // the system instead of adding everything
            result.model = model;
            // find used images
            result.usedImages = getUsedImages(model, system);
            // append to results
            results.add(result);
        }

        return results;
    }

    @Override
    public List<BenchmarkMetaData> getBenchmarks() {
        return new ArrayList<>(benchmarks.values());
    }

    protected List<SystemMetaData> getSystems() {
        return new ArrayList<>(systems.values());
    }

    @Override
    public List<SystemMetaData> getSystemsForBenchmark(String benchmarkUri) {
        List<SystemMetaData> results = new ArrayList<>();
        List<SystemMetaData> systems = getSystems();
        List<BenchmarkMetaData> benchmarks = getBenchmarks();

        // then first find input benchmark
        BenchmarkMetaData benchmark = null;
        for (BenchmarkMetaData b : benchmarks) {
            if (b.uri.equals(benchmarkUri)) {
                benchmark = b;
                break;
            }
        }

        // if no benchmark found - return empty results
        if (benchmark == null) {
            LOGGER.error("Input benchmark not found, returning empty results.");
            return results;
        }

        // find all systems that have same api
        for (SystemMetaData s : systems) {
            int intersectSize = Sets.intersection(benchmark.definedApis, s.implementedApis).size();
            if (intersectSize > 0) {
                results.add(s);
            }
        }

        return results;
    }

    @Override
    public List<SystemMetaData> getSystemsForBenchmark(Model benchmarkModel) {
        String benchmarkUri = getBenchmarkUri(benchmarkModel);
        return getSystemsForBenchmark(benchmarkUri);
    }

    @Override
    public Model getBenchmarkModel(String benchmarkUri) {
        if (benchmarks.containsKey(benchmarkUri)) {
            return benchmarks.get(benchmarkUri).model;
        } else {
            return null;
        }
    }

    @Override
    public Model getSystemModel(String systemUri) {
        if (systems.containsKey(systemUri)) {
            return systems.get(systemUri).model;
        } else {
            return null;
        }
    }

    @Override
    public String getBenchmarkImageName(String benchmarkUri) {
        return getImageName(getBenchmarkModel(benchmarkUri), benchmarkUri);
    }

    @Override
    public String getSystemImageName(String systemUri) {
        return getImageName(getSystemModel(systemUri), systemUri);
    }

    protected static String getBenchmarkUri(Model model) {
        // find benchmark subject
        Resource benchmark = null;
        ResIterator subjects = model.listSubjectsWithProperty(RDF.type, HOBBIT.Benchmark);
        if (subjects.hasNext()) {
            benchmark = subjects.next();
        }

        // check if benchmark was actually found
        if (benchmark == null) {
            return null;
        }

        // set URI
        return benchmark.getURI();
    }

    protected static String getImageName(Model model, String subjUri) {
        if (model == null) {
            return null;
        }
        Resource subj = model.getResource(subjUri);
        NodeIterator iterator = model.listObjectsOfProperty(subj, HOBBIT.imageName);
        if (iterator.hasNext()) {
            RDFNode node = iterator.next();
            if (node.isLiteral()) {
                return node.asLiteral().getString();
            }
        }
        return null;
    }

    protected static Set<String> getAPIs(Model model, Resource resource, boolean isBenchmark) {
        Set<String> apis = new HashSet<>();
        NodeIterator apisList = model.listObjectsOfProperty(resource,
                isBenchmark ? HOBBIT.hasAPI : HOBBIT.implementsAPI);
        while (apisList.hasNext()) {
            RDFNode n = apisList.next();
            apis.add(n.toString());
        }
        return apis;
    }

    protected static String getName(Model model, Resource resource) {
        return model.getProperty(resource, RDFS.label).getString();
    }

    protected static String getDescription(Model model, Resource resource) {
        return model.getProperty(resource, RDFS.comment).getString();
    }

    protected static String getImage(Model model, Resource resource) {
        return model.getProperty(resource, HOBBIT.imageName).getString();
    }

    @Override
    public List<SystemMetaData> getSystemsOfUser(String userName) {
        return getSystems();
    }

    protected Set<String> getUsedImages(Model model, Resource resource) {
        Set<String> images = new HashSet<>();
        NodeIterator imagesList = model.listObjectsOfProperty(resource, HOBBIT.usesImage);
        while (imagesList.hasNext()) {
            RDFNode n = imagesList.next();
            images.add(n.toString());
        }
        return images;
    }

    @Override
    public BenchmarkMetaData modelToBenchmarkMetaData(Model model) throws Exception {
        BenchmarkMetaData result = new BenchmarkMetaData();

        // find benchmark subject
        List<Resource> benchmarks = RdfHelper.getSubjectResources(model, RDF.type, HOBBIT.Benchmark);
        if (benchmarks.size() == 0) {
            return null;
        }
        Resource benchmark = benchmarks.get(0);
        // set URI
        result.uri = benchmark.getURI();
        // find name
        result.name = getName(model, benchmark);
        // find description
        result.description = getDescription(model, benchmark);
        // find APIs
        result.definedApis = getAPIs(model, benchmark, true);
        // find used images
        result.usedImages = getUsedImages(model, benchmark);

        return result;
    }
}
