package org.hobbit.controller.docker;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;

/**
 * Interface of a class managing the images of benchmarks and systems.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface ImageManager {

    /**
     * Retrieves a list of all known benchmarks
     * 
     * @return the meta data of all known benchmarks
     */
    public List<BenchmarkMetaData> getBenchmarks();

    /**
     * Retrieves a list of systems that have been defined by the given user.
     * 
     * @param userName
     *            the name of the user
     * @return a list of system meta data
     */
    public List<SystemMetaData> getSystemsOfUser(String userName);

    /**
     * Retrieves a list of systems that are compatible to the given benchmark.
     * 
     * @param benchmarkUri
     *            the URI of the chosen benchmark
     * @return a list of system meta data
     */
    public List<SystemMetaData> getSystemsForBenchmark(String benchmarkUri);

    /**
     * Retrieves a list of systems that are compatible to the given benchmark.
     * 
     * @param benchmarkModel
     *            the RDF model of the chosen benchmark
     * @return a list of system meta data
     */
    public List<SystemMetaData> getSystemsForBenchmark(Model benchmarkModel);

    /**
     * Retrieves the RDF model of the benchmark with the given URI
     * 
     * @param benchmarkUri
     *            the URI of the chosen benchmark
     * @return the RDF model of the chosen benchmark
     */
    public Model getBenchmarkModel(String benchmarkUri);

    /**
     * Retrieves the RDF model of the system with the given URI
     * 
     * @param systemUri
     *            the URI of the chosen system
     * @return the RDF model of the chosen system
     */
    public Model getSystemModel(String systemUri);

    /**
     * Retrieves the Docker image name of the benchmark with the given URI
     * 
     * @param benchmarkUri
     *            the URI of the chosen benchmark
     * @return the Docker image name of the chosen benchmark
     */
    public String getBenchmarkImageName(String benchmarkUri);

    /**
     * Retrieves the Docker image name of the system with the given URI
     * 
     * @param systemUri
     *            the URI of the chosen system
     * @return the Docker image name of the chosen system
     */
    public String getSystemImageName(String systemUri);
}
