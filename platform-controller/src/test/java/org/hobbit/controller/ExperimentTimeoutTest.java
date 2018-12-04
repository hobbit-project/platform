package org.hobbit.controller;

import java.util.concurrent.Semaphore;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.mocks.DummyPlatformController;
import org.hobbit.controller.mocks.DummyImageManager;
import org.hobbit.controller.mocks.DummyStorageServiceClient;
import org.hobbit.core.data.status.ControllerStatus;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitErrors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

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

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void init() {
        // set max execution time to 1s
        System.setProperty("MAX_EXECUTION_TIME", "1000");
        controller = new DummyPlatformController(benchmarkControllerTerminated);
        controller.queue.add(new ExperimentConfiguration(EXPERIMENT_ID, DummyImageManager.BENCHMARK_NAME, "{}", DummyImageManager.SYSTEM_URI));
        manager = new ExperimentManager(controller, 1000, 1000);
        controller.expManager = manager;
    }

    @Test (timeout = 10000)
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
        Assert.assertTrue("Result model contains the error information about terminated experiment",
                resultModel.contains(resultModel.getResource("http://w3id.org/hobbit/experiments#" + EXPERIMENT_ID),
                        HOBBIT.terminatedWithError, HobbitErrors.ExperimentTookTooMuchTime));
    }

    @After
    public void close() {
        IOUtils.closeQuietly(manager);
    }
}
