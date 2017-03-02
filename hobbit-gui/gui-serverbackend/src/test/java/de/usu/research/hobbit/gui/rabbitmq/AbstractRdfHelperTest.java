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
