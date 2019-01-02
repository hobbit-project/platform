/**
 * This file is part of analysis-component.
 *
 * analysis-component is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * analysis-component is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with analysis-component.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.analysis;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.analysis.AnalysisComponent.DataProcessor;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitExperiments;
import weka.core.Instances;

public class CorrelationTest {

    private DataProcessor dp;
    private AnalysisComponent.AnalysisModel analysisModel;

    @Before
    public void setUp() throws Exception {
        dp = new AnalysisComponent.DataProcessor();
        dp.getParametersFromRdfModel(Arrays.asList(
            loadModel("org/hobbit/analysis/goodParams.ttl"),
            loadModel("org/hobbit/analysis/goodKpis.ttl")
        ));

        Instances correlationDataset = dp.getInstancesDatasetForCorrelation();
        assertNotNull("Correlation dataset", correlationDataset);
        analysisModel = new AnalysisComponent.AnalysisModel(
                null,
                null,
                null,
                correlationDataset,
                ModelFactory.createDefaultModel(),
                HobbitExperiments.New.getURI());
    }

    @Test
    public void testCorrelation() throws Exception {
        analysisModel.computeCorrelation(
                "http://example.com/exampleBenchmark",
                "http://example.com/exampleSystemV1");

        List<Statement> resultsets = analysisModel.correlationModel.listStatements(null, RDF.type, HOBBIT.AnalysisResultset).toList();
        assertEquals("Number of hobbit:AnalysisResultset resources",
                1, resultsets.size());

        List<Statement> results = analysisModel.correlationModel.listStatements(null, RDF.type, HOBBIT.AnalysisResult).toList();
        assertEquals("Number of hobbit:AnalysisResult resources",
                6, results.size());
    }

    private static Model loadModel(String resourceName) {
        Model model = ModelFactory.createDefaultModel();
        InputStream is = CorrelationTest.class.getClassLoader().getResourceAsStream(resourceName);
        assertNotNull("Input stream for " + resourceName, is);
        try {
            RDFDataMgr.read(model, is, Lang.TTL);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return model;
    }

}
