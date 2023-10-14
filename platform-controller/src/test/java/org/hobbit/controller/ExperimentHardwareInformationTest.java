package org.hobbit.controller;

import java.util.List;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.DOAP;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.docker.ResourceInformationCollectorImpl;
import org.hobbit.controller.mocks.DummyImageManager;
import org.hobbit.controller.mocks.DummyPlatformController;
import org.hobbit.controller.mocks.DummyStorageServiceClient;
import org.hobbit.utils.config.HobbitConfiguration;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.MEXCORE;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple test that uses a dummy {@link PlatformController} to simulate an
 * experiment that does not terminate and needs to be terminated by the
 * {@link ExperimentManager}.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ExperimentHardwareInformationTest extends DockerBasedTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentHardwareInformationTest.class);

    private static final String EXPERIMENT_ID = "hardwareInformationExperiment";

    private ExperimentManager manager;
    private PlatformController controller;

    @Before
    public void init() throws Exception {
        controller = new DummyPlatformController();
        controller.resInfoCollector = new ResourceInformationCollectorImpl(controller.containerManager);
        controller.queue.add(new ExperimentConfiguration(EXPERIMENT_ID, DummyImageManager.BENCHMARK_NAME, "{}", DummyImageManager.SYSTEM_URI));
        HobbitConfiguration configuration = new HobbitConfiguration();
        manager = new ExperimentManager(controller, configuration, 1000L, 1000L);
        controller.expManager = manager;
    }

    @Test
    public void test() throws Exception {
        LOGGER.info("Waiting for experiment to start...");
        Thread.sleep(2000);

        manager.handleExperimentTermination();
        Model resultModel = ((DummyStorageServiceClient) controller.storage).insertedModel;
        Assert.assertNotNull("Result model", resultModel);

        Resource experiment = resultModel.getResource("http://w3id.org/hobbit/experiments#" + EXPERIMENT_ID);

        List<RDFNode> clusters = resultModel.listObjectsOfProperty(experiment, HOBBIT.wasCarriedOutOn).toList();
        Assert.assertEquals("Number of hobbit:wasCarriedOutOn objects", 1, clusters.size());

        Resource cluster = clusters.get(0).asResource();
        Assert.assertTrue("hobbit:wasCarriedOutOn's object is a hobbit:Hardware",
                resultModel.contains(cluster, RDF.type, HOBBIT.Hardware));

        int nodesInSwarm = dockerClient.info().swarm().nodes();
        Assert.assertFalse("There should be at least one swarm node", nodesInSwarm == 0);

        List<RDFNode> nodes = resultModel.listObjectsOfProperty(cluster, HOBBIT.comprises).toList();
        Assert.assertEquals("Number of hobbit:comprises objects", nodesInSwarm, nodes.size());

        Resource node = nodes.get(0).asResource();
        Assert.assertTrue("hobbit:comprises's object is a mexcore:HardwareConfiguration",
                resultModel.contains(node, RDF.type, MEXCORE.HardwareConfiguration));
        Assert.assertTrue("mexcore:HardwareConfiguration has rdfs:label property",
                resultModel.contains(node, RDFS.label));
        Assert.assertTrue("mexcore:HardwareConfiguration has mexcore:cpu property",
                resultModel.contains(node, MEXCORE.cpu));
        Assert.assertTrue("mexcore:HardwareConfiguration has mexcore:memory property",
                resultModel.contains(node, MEXCORE.memory));
        Assert.assertTrue("mexcore:HardwareConfiguration has doap:os property",
                resultModel.contains(node, DOAP.os));
    }

    @After
    public void close() {
        IOUtils.closeQuietly(manager);
    }
}
