package de.usu.research.hobbit.gui.rabbitmq;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
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
        StmtIterator iterator;
        Statement s;
        String expectedModelString = expectedResult.toString();
        String resultModelString = result.toString();
        // Check the recall
        iterator = expectedResult.listStatements();
        while (iterator.hasNext()) {
            s = iterator.next();
            Assert.assertTrue(
                    "The result does not contain the expected statement " + s.toString() + ". expected model:\n"
                            + expectedModelString + "\nresult model:\n" + resultModelString,
                    modelContainsStatement(result, s));
        }
        // Check the precision
        iterator = result.listStatements();
        while (iterator.hasNext()) {
            s = iterator.next();
            Assert.assertTrue(
                    "The result contains the unexpected statement " + s.toString() + ". expected model:\n"
                            + expectedModelString + "\nresult model:\n" + resultModelString,
                    modelContainsStatement(result, s));
        }
    }

    private boolean modelContainsStatement(Model result, Statement s) {
        Resource subject = s.getSubject();
        RDFNode object = s.getObject();
        if (subject.isAnon()) {
            if (object.isAnon()) {
                return result.contains(null, s.getPredicate(), (RDFNode) null);
            } else {
                return result.contains(null, s.getPredicate(), object);
            }
        } else {
            if (object.isAnon()) {
                return result.contains(subject, s.getPredicate(), (RDFNode) null);
            } else {
                return result.contains(subject, s.getPredicate(), object);
            }
        }
    }

    protected abstract Model performTransformation(Model model);

    private Model loadModel(String resourceName) {
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
