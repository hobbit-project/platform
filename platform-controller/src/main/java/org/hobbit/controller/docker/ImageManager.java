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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

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
     * Retrieves a list of all known benchmarks
     *
     * @return the meta data of all known benchmarks
     */
    public List<SystemMetaData> getSystems();

    /**
     * Retrieves a list of systems that have been defined by the given user. Note
     * that the default implementation does not filter the systems.
     *
     * @param email
     *            the e-mail of the user
     * @return a list of system meta data
     */
    public default List<SystemMetaData> getSystemsOfUser(String email) {
        return getSystems();
    }

    /**
     * Retrieves a list of systems that are compatible to the given benchmark.
     *
     * @param benchmarkUri
     *            the URI of the chosen benchmark
     * @return a list of system meta data
     */
    @SuppressWarnings("unchecked")
    public default List<SystemMetaData> getSystemsForBenchmark(String benchmarkUri) {
        // First find input benchmark
        BenchmarkMetaData benchmark = getBenchmark(benchmarkUri);

        // if no benchmark found - return empty results
        if (benchmark == null) {
            LoggerFactory.getLogger(ImageManager.class).error("Input benchmark not found, returning empty results.");
            return Collections.EMPTY_LIST;
        }
        return getSystems().parallelStream()
                .filter(s -> (Sets.intersection(benchmark.definedApis, s.implementedApis).size() > 0))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of systems that are compatible to the given benchmark.
     *
     * @param benchmarkModel
     *            the RDF model of the chosen benchmark
     * @return a list of system meta data
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public default List<SystemMetaData> getSystemsForBenchmark(Model benchmarkModel) {
        List<Resource> benchmarks = RdfHelper.getSubjectResources(benchmarkModel, RDF.type, HOBBIT.Benchmark);
        if ((benchmarks == null) || (benchmarks.size() == 0)) {
            return Collections.EMPTY_LIST;
        } else {
            return getSystemsForBenchmark(benchmarks.get(0).getURI());
        }
    }

    /**
     * Retrieves the RDF model of the benchmark with the given URI
     *
     * @param benchmarkUri
     *            the URI of the chosen benchmark
     * @return the RDF model of the chosen benchmark
     */
    public default BenchmarkMetaData getBenchmark(String benchmarkUri) {
        if (benchmarkUri == null) {
            return null;
        }
        return getBenchmarks().parallelStream().filter(b -> benchmarkUri.equals(b.uri)).findAny().orElse(null);
    }

    /**
     * Retrieves the RDF model of the system with the given URI
     *
     * @param systemUri
     *            the URI of the chosen system
     * @return the RDF model of the chosen system
     */
    public default SystemMetaData getSystem(String systemUri) {
        if (systemUri == null) {
            return null;
        }
        return getSystems().parallelStream().filter(s -> systemUri.equals(s.uri)).findAny().orElse(null);
    }

    /**
     * Retrieves the RDF model of the benchmark with the given URI
     *
     * @param benchmarkUri
     *            the URI of the chosen benchmark
     * @return the RDF model of the chosen benchmark
     */
    public default Model getBenchmarkModel(String benchmarkUri) {
        BenchmarkMetaData metaData = getBenchmark(benchmarkUri);
        if (metaData != null) {
            return metaData.rdfModel;
        } else {
            return null;
        }
    }

    /**
     * Retrieves the RDF model of the system with the given URI
     *
     * @param systemUri
     *            the URI of the chosen system
     * @return the RDF model of the chosen system
     */
    public default Model getSystemModel(String systemUri) {
        SystemMetaData metaData = getSystem(systemUri);
        if (metaData != null) {
            return metaData.rdfModel;
        } else {
            return null;
        }
    }

    /**
     * Retrieves the Docker image name of the benchmark with the given URI
     *
     * @param benchmarkUri
     *            the URI of the chosen benchmark
     * @return the Docker image name of the chosen benchmark
     */
    @Deprecated
    public default String getBenchmarkImageName(String benchmarkUri) {
        BenchmarkMetaData benchmark = getBenchmark(benchmarkUri);
        if (benchmark != null) {
            return benchmark.mainImage;
        } else {
            return null;
        }
    }

    /**
     * Retrieves the Docker image name of the system with the given URI
     *
     * @param systemUri
     *            the URI of the chosen system
     * @return the Docker image name of the chosen system
     */
    @Deprecated
    public default String getSystemImageName(String systemUri) {
        SystemMetaData system = getSystem(systemUri);
        if (system != null) {
            return system.mainImage;
        } else {
            return null;
        }
    }
}
