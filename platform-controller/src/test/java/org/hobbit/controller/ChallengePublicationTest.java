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
package org.hobbit.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.hobbit.controller.analyze.ExperimentAnalyzer;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.queue.ExperimentQueue;
import org.hobbit.core.Constants;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.utils.test.ModelComparisonHelper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChallengePublicationTest {

    @Test
    @SuppressWarnings("resource")
    public void test() {
        // Load data
        DummyStorage storage = new DummyStorage("org/hobbit/controller/UnpublishedChallengeConfigGraph.ttl",
                "org/hobbit/controller/UnpublishedChallengePrivateGraph.ttl", null);
        DummyStorage expectedStorage = new DummyStorage("org/hobbit/controller/PublishedChallengeConfigGraph.ttl",
                "org/hobbit/controller/PublishedChallengePrivateGraph.ttl",
                "org/hobbit/controller/PublishedChallengePublicGraph.ttl");
        ExperimentQueue queue = new DummyQueue();
        ExperimentAnalyzer analyzer = new DummyAnalyzer();

        PlatformController.republishChallenges(storage, queue, analyzer);
        // make sure that the data has been published correctly
        compareModels(expectedStorage.getDataset().getNamedModel(Constants.PUBLIC_RESULT_GRAPH_URI),
                storage.getDataset().getNamedModel(Constants.PUBLIC_RESULT_GRAPH_URI));
        // make sure that the data has been removed from the other graphs
        compareModels(expectedStorage.getDataset().getNamedModel(Constants.CHALLENGE_DEFINITION_GRAPH_URI),
                storage.getDataset().getNamedModel(Constants.CHALLENGE_DEFINITION_GRAPH_URI));
        compareModels(expectedStorage.getDataset().getNamedModel(Constants.PRIVATE_RESULT_GRAPH_URI),
                storage.getDataset().getNamedModel(Constants.PRIVATE_RESULT_GRAPH_URI));
    }

    protected static void compareModels(Model expectedResult, Model result) {
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

    protected static final class DummyStorage extends StorageServiceClient {

        private static final Logger LOGGER = LoggerFactory.getLogger(DummyStorage.class);

        private Dataset dataset;

        public DummyStorage(String configGraphFile, String privateGraphFile, String publicGraphFile) {
            super(null);
            dataset = DatasetFactory.create();
            Model model = configGraphFile != null ? loadModel(configGraphFile) : ModelFactory.createDefaultModel();
            dataset.addNamedModel(Constants.CHALLENGE_DEFINITION_GRAPH_URI, model);
            model = privateGraphFile != null ? loadModel(privateGraphFile) : ModelFactory.createDefaultModel();
            dataset.addNamedModel(Constants.PRIVATE_RESULT_GRAPH_URI, model);
            model = publicGraphFile != null ? loadModel(publicGraphFile) : ModelFactory.createDefaultModel();
            dataset.addNamedModel(Constants.PUBLIC_RESULT_GRAPH_URI, model);
        }

        @Override
        public boolean sendAskQuery(String query) throws Exception {
            throw new IllegalArgumentException("The method is not implemented");
            // if (query == null) {
            // throw new IllegalArgumentException("The given query is null.");
            // }
            // byte[] response =
            // rpcClient.request(RabbitMQUtils.writeString(query));
            // if (response != null) {
            // try {
            // return Boolean.parseBoolean(RabbitMQUtils.readString(response));
            // } catch (Exception e) {
            // throw new Exception("Couldn't parse boolean value from response.
            // Returning false.", e);
            // }
            // }
            // throw new Exception("Couldn't get response for query.");
        }

        @Override
        public Model sendConstructQuery(String query) {
            if (query == null) {
                LOGGER.error("The given query is null. Returning null.");
                return null;
            }
            QueryExecution qe = QueryExecutionFactory.create(query, dataset);
            return qe.execConstruct();
        }

        @Override
        public Model sendDescribeQuery(String query) {
            throw new IllegalArgumentException("The method is not implemented");
            // if (query == null) {
            // LOGGER.error("The given query is null. Returning null.");
            // return null;
            // }
            // byte[] response =
            // rpcClient.request(RabbitMQUtils.writeString(query));
            // if (response != null) {
            // try {
            // return RabbitMQUtils.readModel(response);
            // } catch (Exception e) {
            // LOGGER.error("The response couldn't be parsed. Returning null.",
            // e);
            // }
            // }
            // return null;
        }

        @Override
        public ResultSet sendSelectQuery(String query) {
            if (query == null) {
                LOGGER.error("The given query is null. Returning null.");
                return null;
            }
            QueryExecution qe = QueryExecutionFactory.create(query, dataset);
            return qe.execSelect();
        }

        @Override
        public boolean sendUpdateQuery(String query) {
            if (query == null) {
                LOGGER.error("Can not send an update query that is null. Returning false.");
                return false;
            }
            UpdateRequest update = UpdateFactory.create(query);
            UpdateProcessor up = UpdateExecutionFactory.create(update, dataset);
            up.execute();
            // DatasetGraph dg = up.getDatasetGraph();
            // return
            // ModelFactory.createModelForGraph(dg.getGraph(NodeFactory.createURI(graphUri)));
            return true;
        }

        @Override
        public boolean sendInsertQuery(Model model, String graphURI) {
            if (model == null) {
                LOGGER.error("Can not store a model that is null. Returning false.");
                return false;
            }
            if (graphURI == null) {
                LOGGER.error("Can not store a model without a graph URI. Returning false.");
                return false;
            }
            if (dataset.containsNamedModel(graphURI)) {
                dataset.getNamedModel(graphURI).add(model);
            } else {
                dataset.addNamedModel(graphURI, model);
            }
            return true;
        }

        @Override
        public void close() throws IOException {
        }

        public Dataset getDataset() {
            return dataset;
        }
    }

    protected static final class DummyQueue implements ExperimentQueue {

        @Override
        public ExperimentConfiguration getNextExperiment() {
            return null;
        }

        @Override
        public void add(ExperimentConfiguration experiment) {
        }

        @Override
        public void remove(ExperimentConfiguration experiment) {
        }

        @Override
        public List<ExperimentConfiguration> listAll() {
            return new ArrayList<>(0);
        }
    }

    protected static final class DummyAnalyzer implements ExperimentAnalyzer {

        protected List<String> uris = new ArrayList<>();

        @Override
        public void analyzeExperiment(String uri) throws IOException {
            uris.add(uri);
        }

        public List<String> getUris() {
            return uris;
        }
    }

    protected static Model loadModel(String resourceName) {
        Model model = ModelFactory.createDefaultModel();
        InputStream is = ChallengePublicationTest.class.getClassLoader().getResourceAsStream(resourceName);
        Assert.assertNotNull(resourceName + " couldn't be loaded.", is);
        try {
            RDFDataMgr.read(model, is, Lang.TTL);
        } catch (Exception e) {
            throw new IllegalStateException(resourceName + " couldn't be loaded.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return model;
    }
}
