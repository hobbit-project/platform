/**
 * This file is part of gui-serverbackend.
 *
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rabbitmq;

import java.io.InputStream;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.hobbit.utils.test.ModelComparisonHelper;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractRdfHelperTest {

    /**
     * Name of the resource from which the store content is loaded.
     */
    private String storeContentResource;
    /**
     * Name of the resource from which the expected result is loaded.
     */
    private String expectedResultResource;

    public AbstractRdfHelperTest(String storeContentResource, String expectedResultResource) {
        this.storeContentResource = storeContentResource;
        this.expectedResultResource = expectedResultResource;
    }

    @Test
    public void test() {
        // load the models
        Model originalModel = loadModel(storeContentResource);
        // load expected result
        Model expectedResult = loadModel(expectedResultResource);

        // execte query
        Model result = performTransformation(originalModel);

        // Compare the models
        String expectedModelString = expectedResult.toString();
        String resultModelString = result.toString();
        // Check the recall
        Set<Statement> statements = ModelComparisonHelper.getMissingStatements(expectedResult, result);
        Assert.assertTrue("The result does not contain the expected statements " + statements.toString()
                + ". expected model:\n" + expectedModelString + "\nresult model:\n" + resultModelString,
                statements.size() == 0);
        // Check the precision
        statements = ModelComparisonHelper.getMissingStatements(result, expectedResult);
        Assert.assertTrue("The result contains the unexpected statements " + statements.toString()
                + ". expected model:\n" + expectedModelString + "\nresult model:\n" + resultModelString,
                statements.size() == 0);
    }

    protected abstract Model performTransformation(Model model);

    protected static Model loadModel(String resourceName) {
        Model model = ModelFactory.createDefaultModel();
        InputStream is = AbstractRdfHelperTest.class.getClassLoader().getResourceAsStream(resourceName);
        Assert.assertNotNull(is);
        try {
            RDFDataMgr.read(model, is, Lang.TTL);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return model;
    }
}
