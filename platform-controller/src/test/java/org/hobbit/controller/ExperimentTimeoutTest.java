package org.hobbit.controller;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.jena.rdf.model.Model;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.mocks.DummyImageManager;
import org.hobbit.controller.mocks.DummyPlatformController;
import org.hobbit.controller.mocks.DummyStorageServiceClient;
import org.hobbit.controller.utils.RabbitMQConnector;
import org.hobbit.core.Constants;
import org.hobbit.core.data.status.ControllerStatus;
import org.hobbit.utils.config.HobbitConfiguration;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitErrors;
import org.hobbit.vocab.HobbitExperiments;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * A simple test that uses a dummy {@link PlatformController} to simulate an
 * experiment that does not terminate and needs to be terminated by the
 * {@link ExperimentManager}.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ExperimentTimeoutTest {

    private static final String EXPERIMENT_ID = "123";

    private ExperimentManager manager;
    private PlatformController controller;
    private Semaphore benchmarkControllerTerminated = new Semaphore(0);

    @Before
    public void init() {
        // set max execution time to 1s
        Configuration config = new MapConfiguration(new HashMap<>());
        config.addProperty("MAX_EXECUTION_TIME", "1000");
        config.addProperty("HOBBIT_RABBIT_IMAGE", "rabbitmq:management");
        HobbitConfiguration configuration = new HobbitConfiguration();
        configuration.addConfiguration(config);
        configuration.addConfiguration(new EnvironmentConfiguration());

        controller = new DummyPlatformController(benchmarkControllerTerminated);
        controller.queue.add(new ExperimentConfiguration(EXPERIMENT_ID, DummyImageManager.BENCHMARK_NAME, "{}",
                DummyImageManager.SYSTEM_URI));
        manager = new ExperimentManager(controller, configuration, 1000, 1000) {
            // We have to override the creation of the RabbitMQ connector to the
            // experiment's RabbitMQ broker. Instead, we connect to the already running
            // RabbitMQ.
            protected void createRabbitMQ(ExperimentConfiguration config) throws Exception {
                RabbitMQConnector rabbitMQConnector = new RabbitMQConnector(controller,
                        this.hobbitConfig.getString(Constants.RABBIT_MQ_HOST_NAME_KEY));
                controller.setExpRabbitMQConnector(rabbitMQConnector);
                rabbitMQConnector.init();
            };
        };
        controller.expManager = manager;
    }

    @Test(timeout = 20000)
    public void test() throws Exception {
        benchmarkControllerTerminated.acquire();
        // Give the system some time to tidy up
        Thread.sleep(1000);
        // Check queue
        Assert.assertEquals("Queue size after experiment termination by timeout", 0, controller.queue.listAll().size());
        // Check status
        ControllerStatus status = new ControllerStatus();
        manager.addStatusInfo(status, "");
        Assert.assertNull("Status of the running experiment", status.experiment);
        Model resultModel = ((DummyStorageServiceClient) controller.storage).insertedModel;
        Assert.assertTrue("Result model contains the error information about the failed experiment.",
                resultModel.contains(HobbitExperiments.getExperiment(EXPERIMENT_ID), HOBBIT.terminatedWithError,
                        HobbitErrors.ExperimentTookTooMuchTime));
    }

    @After
    public void close() {
        IOUtils.closeQuietly(manager);
    }
}
