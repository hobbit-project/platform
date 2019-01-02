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
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.hobbit.analysis.AnalysisComponent.DataProcessor;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.*;
import static org.junit.Assert.*;
import weka.core.Instances;

public class DataProcessorTest {

    private DataProcessor dp;

    @Before
    public void setUp() throws Exception {
        dp = new AnalysisComponent.DataProcessor();
        dp.getParametersFromRdfModel(Arrays.asList(
            loadModel("org/hobbit/analysis/goodParams.ttl"),
            loadModel("org/hobbit/analysis/goodKpis.ttl")
        ));
    }

    @Test
    public void testCorrelationDataset() throws Exception {
        Instances dataset = dp.getInstancesDatasetForCorrelation();
        assertNotNull("Correlation dataset", dataset);
        assertEquals("Number of attributes", 5, dataset.numAttributes());
        assertEquals("Number of instances", 3, dataset.numInstances());
        assertThat(dataset.stream().map(Object::toString).toArray(),
            arrayContainingInAnyOrder(
                "1,11,1,1,1",
                "2,12,11,2,10",
                "3,13,22,3,100"));
    }

    private static Model loadModel(String resourceName) {
        Model model = ModelFactory.createDefaultModel();
        InputStream is = DataProcessorTest.class.getClassLoader().getResourceAsStream(resourceName);
        assertNotNull("Input stream for " + resourceName, is);
        try {
            RDFDataMgr.read(model, is, Lang.TTL);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return model;
    }

}
