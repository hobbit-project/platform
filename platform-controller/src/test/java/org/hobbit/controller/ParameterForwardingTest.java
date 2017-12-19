package org.hobbit.controller;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.docker.ImageManager;
import org.hobbit.core.Constants;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.utils.test.ModelComparisonHelper;
import org.hobbit.vocab.HOBBIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This is a whitebox test for
 * {@link ExperimentManager#getSerializedSystemParams(ExperimentConfiguration, ImageManager)}.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class ParameterForwardingTest implements ImageManager {

    private String benchmarkUri = "http://w3id.org/gerbil/hobbit/vocab#GerbilBenchmark";
    private String systemUri = "http://example.org/DummySystem";
    private Model benchmarkModel;
    private Model benchmarkParamModel;
    private Model expectedResult;
    private Model systemModel;

    @Before
    public void prepare() {
        InputStream input = ParameterForwardingTest.class.getClassLoader()
                .getResourceAsStream("org/hobbit/controller/benchmark.ttl");
        Assert.assertNotNull(input);
        benchmarkModel = ModelFactory.createDefaultModel();
        benchmarkModel.read(input, "", "TTL");
        IOUtils.closeQuietly(input);

        Property forwardedParameter = benchmarkModel
                .getProperty("http://w3id.org/gerbil/hobbit/vocab#hasExperimentType");
        // FIXME The param class should be replaced by a constant resource in the HOBBIT
        // vocab
        benchmarkModel.add(forwardedParameter, RDF.type,
                benchmarkModel.getResource(HOBBIT.getURI() + "ForwardedParameter"));

        input = ParameterForwardingTest.class.getClassLoader()
                .getResourceAsStream("org/hobbit/controller/exampleExperiment.ttl");
        Assert.assertNotNull(input);
        benchmarkParamModel = ModelFactory.createDefaultModel();
        benchmarkParamModel.read(input, "", "TTL");
        IOUtils.closeQuietly(input);
        System.out.println(benchmarkParamModel.toString());
        NodeIterator iterator = benchmarkParamModel.listObjectsOfProperty(
                benchmarkParamModel.getResource(Constants.NEW_EXPERIMENT_URI), forwardedParameter);
        Assert.assertTrue(iterator.hasNext());
        RDFNode forwardedNode = iterator.next();

        input = ParameterForwardingTest.class.getClassLoader()
                .getResourceAsStream("org/hobbit/controller/dummySystem.ttl");
        Assert.assertNotNull(input);
        expectedResult = ModelFactory.createDefaultModel();
        expectedResult.read(input, "", "TTL");
        IOUtils.closeQuietly(input);
        expectedResult.add(expectedResult.getResource(systemUri), forwardedParameter, forwardedNode);

        input = ParameterForwardingTest.class.getClassLoader()
                .getResourceAsStream("org/hobbit/controller/dummySystem.ttl");
        Assert.assertNotNull(input);
        systemModel = ModelFactory.createDefaultModel();
        systemModel.read(input, "", "TTL");
        IOUtils.closeQuietly(input);
    }

    @Test
    public void testParameterForwarding() {
        ExperimentConfiguration config = new ExperimentConfiguration("123", benchmarkUri,
                RabbitMQUtils.writeModel2String(benchmarkParamModel), systemUri);
        String serializedSystemModel = ExperimentManager.getSerializedSystemParams(config, this);
        Model result = RabbitMQUtils.readModel(serializedSystemModel);

        // Compare the models
        String expectedModelString = expectedResult.toString();
        String resultModelString = result.toString();
        // Check the precision and recall
        Set<Statement> missingStatements = ModelComparisonHelper.getMissingStatements(expectedResult, result);
        Set<Statement> unexpectedStatements = ModelComparisonHelper.getMissingStatements(result, expectedResult);

        StringBuilder builder = new StringBuilder();
        if (unexpectedStatements.size() != 0) {
            builder.append("The result contains the unexpected statements " + unexpectedStatements.toString()
                    + ". expected model:\n" + expectedModelString + "\nresult model:\n" + resultModelString + "\n");
        }
        if (missingStatements.size() != 0) {
            builder.append("The result does not contain the expected statements " + missingStatements.toString()
                    + ". expected model:\n" + expectedModelString + "\nresult model:\n" + resultModelString + "\n");
        }

        Assert.assertTrue(builder.toString(), missingStatements.size() == 0 && unexpectedStatements.size() == 0);
    }

    @Override
    public List<BenchmarkMetaData> getBenchmarks() {
        throw new NotImplementedException();
    }

    @Override
    public List<SystemMetaData> getSystemsOfUser(String userName) {
        throw new NotImplementedException();
    }

    @Override
    public List<SystemMetaData> getSystemsForBenchmark(String benchmarkUri) {
        throw new NotImplementedException();
    }

    @Override
    public List<SystemMetaData> getSystemsForBenchmark(Model benchmarkModel) {
        throw new NotImplementedException();
    }

    @Override
    public Model getBenchmarkModel(String benchmarkUri) {
        if (this.benchmarkUri.equals(benchmarkUri)) {
            return benchmarkModel;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public Model getSystemModel(String systemUri) {
        if (this.systemUri.equals(systemUri)) {
            return systemModel;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public String getBenchmarkImageName(String benchmarkUri) {
        throw new NotImplementedException();
    }

    @Override
    public String getSystemImageName(String systemUri) {
        throw new NotImplementedException();
    }

    @Override
    public BenchmarkMetaData modelToBenchmarkMetaData(Model model) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public List<SystemMetaData> modelToSystemMetaData(Model model) throws Exception {
        throw new NotImplementedException();
    }
}
