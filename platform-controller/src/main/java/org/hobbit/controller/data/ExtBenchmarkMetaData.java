package org.hobbit.controller.data;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.data.BenchmarkMetaData;

/**
 * An extended metadata class for benchmarks including the RDF model of the
 * benchmark.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ExtBenchmarkMetaData extends BenchmarkMetaData {

    public Model model;
}
