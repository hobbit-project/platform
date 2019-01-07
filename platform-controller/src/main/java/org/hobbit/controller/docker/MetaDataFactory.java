package org.hobbit.controller.docker;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.ImageMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;

public class MetaDataFactory {

    public static List<BenchmarkMetaData> modelToBenchmarkMetaData(Model model) {
        return modelToBenchmarkMetaData(model, null, null);
    }

    public static List<BenchmarkMetaData> modelToBenchmarkMetaData(Model model, String source, Date date) {
        List<BenchmarkMetaData> results = new ArrayList<>();
        // find all benchmark subjects
        List<Resource> benchmarks = RdfHelper.getSubjectResources(model, RDF.type, HOBBIT.Benchmark);
        for (Resource benchmark : benchmarks) {
            BenchmarkMetaData result = new BenchmarkMetaData();
            buildMetaData(benchmark, result, getModelWithUniqueBenchmark(model, benchmark.getURI()), source, date);
            // find APIs
            result.definedApis = new HashSet<>(RdfHelper.getStringValues(model, benchmark, HOBBIT.hasAPI));
            // append to results
            results.add(result);
        }
        return results;
    }

    public static List<SystemMetaData> modelToSystemMetaData(Model model) {
        return modelToSystemMetaData(model, null, null);
    }

    public static List<SystemMetaData> modelToSystemMetaData(Model model, String source, Date date) {
        List<SystemMetaData> results = new ArrayList<>();
        // find all system subjects
        List<Resource> systems = RdfHelper.getSubjectResources(model, RDF.type, HOBBIT.SystemInstance);
        for (Resource system : systems) {
            SystemMetaData result = new SystemMetaData();
            buildMetaData(system, result, getModelWithUniqueSystem(model, system.getURI()), source, date);
            // find APIs
            result.implementedApis = new HashSet<>(RdfHelper.getStringValues(model, system, HOBBIT.implementsAPI));
            // append to results
            results.add(result);
        }
        return results;
    }

    protected static void buildMetaData(Resource imageResource, ImageMetaData metadata, Model model, String source,
            Date date) {
        // set URI
        metadata.uri = imageResource.getURI();
        // find name
        metadata.name = RdfHelper.getLabel(model, imageResource);
        // find description
        metadata.description = RdfHelper.getDescription(model, imageResource);
        // find image name
        metadata.mainImage = RdfHelper.getStringValue(model, imageResource, HOBBIT.imageName);
        // find used images
        metadata.usedImages = new HashSet<>(RdfHelper.getStringValues(model, imageResource, HOBBIT.usesImage));
        metadata.date = date;
        metadata.source = source;
        metadata.rdfModel = model;
    }

    public static Model ttlStringToModel(String modelString, String lang) {
        // convert string to model
        Model m = ModelFactory.createDefaultModel();
        m.read(new StringReader(modelString), null, lang);
        return m;
    }

    public static Model byteArrayToModel(byte data[], String lang) {
        Model m = ModelFactory.createDefaultModel();
        m.read(new ByteArrayInputStream(data), null, lang);
        return m;
    }

    /**
     * Creates a new model that contains all triples of the old model except the
     * definitions of other hobbit:Benchmark elements than the benchmark with the
     * given URI.
     * 
     * @param model
     *            the model from which all triples will be copied
     * @param benchmarkUri
     *            the URI of the only benchmark which is not removed from the copied
     *            model
     * @return the copied model
     */
    public static Model getModelWithUniqueBenchmark(Model model, String benchmarkUri) {
        return getModelWithUniqueResource(model, benchmarkUri, HOBBIT.Benchmark);
    }

    /**
     * Creates a new model that contains all triples of the old model except the
     * definitions of other {@code hobbit:SystemInstance} elements than the resource
     * with the given URI. Removing a system means that all triples are removed that
     * have a system URI as subject. A system URI is a URI of a resource {@code s}
     * for which a triple {@code s rdf:type hobbit:SystemInstance} can be found in
     * the given model.
     * 
     * @param model
     *            the model from which all triples will be copied
     * @param systemUri
     *            the URI of the only system which is not removed from the copied
     *            model
     * @return the copied model
     */
    public static Model getModelWithUniqueSystem(Model model, String systemUri) {
        return getModelWithUniqueResource(model, systemUri, HOBBIT.SystemInstance);
    }

    protected static Model getModelWithUniqueResource(Model model, String uri, Resource type) {
        if (model == null) {
            return ModelFactory.createDefaultModel();
        }
        List<Resource> resources = RdfHelper.getSubjectResources(model, RDF.type, type);
        if (resources.size() <= 1) {
            return model;
        }
        Model newModel = ModelFactory.createDefaultModel();
        newModel.add(model);
        for (Resource r : resources) {
            if (!r.getURI().equals(uri)) {
                newModel.removeAll(r, null, (RDFNode) null);
            }
        }
        return newModel;
    }

}
