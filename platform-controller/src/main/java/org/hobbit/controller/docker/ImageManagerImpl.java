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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.Charsets;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.hobbit.controller.gitlab.GitlabControllerImpl;
import org.hobbit.controller.gitlab.Project;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Created by Timofey Ermilov on 22/09/16.
 */
public class ImageManagerImpl implements ImageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageManagerImpl.class);

    // gitlab access controller
    private GitlabControllerImpl gitlab;

    public ImageManagerImpl() {
        // instantiate gitlab
        gitlab = new GitlabControllerImpl();
    }

    public void runWhenGitlabIsReady(Runnable r) {
        gitlab.runAfterFirstFetch(r);
    }

    private Model stringToModel(String modelString) {
        // convert string to model
        Model m = ModelFactory.createDefaultModel();
        InputStream stream = new ByteArrayInputStream(modelString.getBytes(Charsets.UTF_8));
        m.read(stream, null, "TTL");
        return m;
    }

    public BenchmarkMetaData modelToBenchmarkMetaData(String modelString) throws Exception {
        return modelToBenchmarkMetaData(stringToModel(modelString));
    }

    public BenchmarkMetaData modelToBenchmarkMetaData(Model model) throws Exception {
        BenchmarkMetaData result = new BenchmarkMetaData();

        // find benchmark subject
        Resource benchmark = getResource(model, HOBBIT.Benchmark);
        // set URI
        result.benchmarkUri = benchmark.getURI();
        // find name
        result.benchmarkName = getName(model, benchmark);
        // find description
        result.benchmarkDescription = getDescription(model, benchmark);
        // find APIs
        result.implementedApis = getAPIs(model, benchmark, true);
        // find used images
        result.usedImages = getUsedImages(model, benchmark);

        return result;
    }

    public List<SystemMetaData> modelToSystemMetaData(String modelString) throws Exception {
        // execute default method on new model
        return modelToSystemMetaData(stringToModel(modelString));
    }

    public List<SystemMetaData> modelToSystemMetaData(Model model) throws Exception {
        List<SystemMetaData> results = new ArrayList<>();

        // find all system subjects
        List<Resource> systems = getResources(model, HOBBIT.SystemInstance);
        for (Resource system : systems) {
            SystemMetaData result = new SystemMetaData();
            // set URI
            result.systemUri = system.getURI();
            // find name
            result.systemName = getName(model, system);
            // find description
            result.systemDescription = getDescription(model, system);
            // find image name
            result.system_image_name = getImage(model, system);
            // find APIs
            result.implementedApis = getAPIs(model, system, false);
            // find used images
            result.usedImages = getUsedImages(model, system);
            // append to results
            results.add(result);
        }

        return results;
    }

    @Override
    public List<BenchmarkMetaData> getBenchmarks() {
        List<BenchmarkMetaData> results = new ArrayList<>();

        List<Project> projects = gitlab.getAllProjects();
        for (Project p : projects) {
            if (p.benchmarkMetadata != null) {
                try {
                    BenchmarkMetaData bench = modelToBenchmarkMetaData(p.benchmarkMetadata);
                    results.add(bench);
                } catch (Exception e) {
                    LOGGER.error("Error parsing benchmark metadata of project \"" + p.name + "\".", e);
                }
            }
        }

        return results;
    }

    public List<SystemMetaData> getSystems() {
        List<SystemMetaData> results = new ArrayList<>();

        List<Project> projects = gitlab.getAllProjects();
        for (Project p : projects) {
            if (p.systemMetadata != null) {
                try {
                    List<SystemMetaData> meta = modelToSystemMetaData(p.systemMetadata);
                    results.addAll(meta);
                } catch (Exception e) {
                    LOGGER.error("Error parsing system metadata of project \"" + p.name + "\".", e);
                }
            }
        }

        return results;
    }

    @Override
    public List<SystemMetaData> getSystemsForBenchmark(String benchmarkUri) {
        List<SystemMetaData> results = new ArrayList<>();
        List<SystemMetaData> systems = getSystems();
        List<BenchmarkMetaData> benchmarks = getBenchmarks();

        // then first find input benchmark
        BenchmarkMetaData benchmark = null;
        for (BenchmarkMetaData b : benchmarks) {
            if (b.benchmarkUri.equals(benchmarkUri)) {
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
            int intersectSize = Sets.intersection(benchmark.implementedApis, s.implementedApis).size();
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
        List<Project> projects = gitlab.getAllProjects();
        for (Project p : projects) {
            if (p.benchmarkMetadata != null) {
                try {
                    BenchmarkMetaData meta = modelToBenchmarkMetaData(p.benchmarkMetadata);
                    if (meta.benchmarkUri.equals(benchmarkUri)) {
                        return stringToModel(p.benchmarkMetadata);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error parsing benchmark metadata:", e);
                }
            }
        }

        return null;
    }

    @Override
    public Model getSystemModel(String systemUri) {
        List<Project> projects = gitlab.getAllProjects();
        for (Project p : projects) {
            if (p.systemMetadata != null) {
                try {
                    Model model = stringToModel(p.systemMetadata);
                    List<SystemMetaData> metas = modelToSystemMetaData(model);
                    for (SystemMetaData meta : metas) {
                        if (meta.systemUri.equals(systemUri)) {
                            // We have to remove all other systems that have not
                            // been requested
                            removeOtherSystems(model, systemUri);
                            return model;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error parsing system metadata:", e);
                }
            }
        }
        return null;
    }

    /**
     * Removes all other systems from the given mdoel which do not have the
     * given system URI. Removing a system means that all triples are removed
     * that have a system URI as subject. A system URI is a URI of a resource
     * {@code s} for which a triple {@code s rdf:type hobbit:SystemInstance} can
     * be found in the given model.
     * 
     * @param model
     *            the model from which the systems will be removed
     * @param systemUri
     *            the URI of the only system which is not removed from the
     *            system
     */
    protected void removeOtherSystems(Model model, String systemUri) {
        List<Resource> systems = RdfHelper.getSubjectResources(model, RDF.type, HOBBIT.SystemInstance);
        for (Resource system : systems) {
            if (!system.getURI().equals(systemUri)) {
                model.remove(model.listStatements(system, null, (RDFNode) null));
            }
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

    protected Resource getResource(Model model, Resource resourceType) throws Exception {
        Objects.requireNonNull(model, "The given RDF model is null");

        // find benchmark subject
        Resource benchmark = null;
        ResIterator subjects = model.listSubjectsWithProperty(RDF.type, resourceType);
        if (subjects.hasNext()) {
            benchmark = subjects.next();
        }

        // check if benchmark was actually found
        Objects.requireNonNull(model, "Benchmark not found!");

        return benchmark;
    }

    protected List<Resource> getResources(Model model, Resource resourceType) throws Exception {
        Objects.requireNonNull(model, "The given RDF model is null");

        List<Resource> results = new ArrayList<>();

        // find benchmark subject
        Resource res = null;
        ResIterator subjects = model.listSubjectsWithProperty(RDF.type, resourceType);
        while (subjects.hasNext()) {
            res = subjects.next();
            if (res != null) {
                results.add(res);
            }
        }

        // check if benchmark was actually found
        if (results.size() == 0) {
            throw new Exception("No resources found!");
        }

        return results;
    }

    protected String getBenchmarkUri(Model model) {
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

    protected String getImageName(Model model, String subjUri) {
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

    protected Set<String> getAPIs(Model model, Resource resource, boolean isBenchmark) {
        Set<String> apis = new HashSet<>();
        NodeIterator apisList = model.listObjectsOfProperty(resource,
                isBenchmark ? HOBBIT.hasAPI : HOBBIT.implementsAPI);
        while (apisList.hasNext()) {
            RDFNode n = apisList.next();
            apis.add(n.toString());
        }
        return apis;
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

    protected String getName(Model model, Resource resource) {
        return model.getProperty(resource, RDFS.label).getString();
    }

    protected String getDescription(Model model, Resource resource) {
        return model.getProperty(resource, RDFS.comment).getString();
    }

    protected String getImage(Model model, Resource resource) {
        return model.getProperty(resource, HOBBIT.imageName).getString();
    }

    @Override
    public List<SystemMetaData> getSystemsOfUser(String userName) {
        return gitlab.getAllProjects().stream()
                // get all projects with required user
                .filter(p -> p.user.equals(userName))
                // ... which have system information
                .filter(p -> p.systemMetadata != null)
                // map them to SystemMetaData
                .flatMap(p -> {
                    try {
                        return modelToSystemMetaData(p.systemMetadata).stream();
                    } catch (Exception e) {
                        LOGGER.error("Error parsing system metadata:", e);
                        return null;
                    }
                })
                // filter out failed conversions
                .filter(p -> p != null).collect(Collectors.toList());
    }
}
