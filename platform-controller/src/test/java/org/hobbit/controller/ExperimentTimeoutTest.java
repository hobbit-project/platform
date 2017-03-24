package org.hobbit.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.docker.ContainerManager;
import org.hobbit.controller.docker.ContainerStateObserver;
import org.hobbit.controller.docker.ContainerTerminationCallback;
import org.hobbit.controller.docker.ImageManager;
import org.hobbit.controller.queue.InMemoryQueue;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.ControllerStatus;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitErrors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;

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
    private static final String BENCHMARK_NAME = "benchmark";

    private ExperimentManager manager;
    private PlatformController controller;
    private Semaphore benchmarkControllerTerminated = new Semaphore(0);

    @Before
    public void init() {
        controller = new DummyPlatformController(benchmarkControllerTerminated);
        controller.queue.add(new ExperimentConfiguration(EXPERIMENT_ID, BENCHMARK_NAME, "{}", "systemUri"));
        manager = new ExperimentManager(controller, 1000, 1000);
        controller.expManager = manager;
        manager.setMaxExecutionTime(1000);
    }

    @Test /* (timeout = 10000) */
    public void test() throws Exception {
        benchmarkControllerTerminated.acquire();
        // Give the system some time to tidy up
        Thread.sleep(1000);
        // Check queue
        Assert.assertEquals(0, controller.queue.listAll().size());
        // Check status
        ControllerStatus status = new ControllerStatus();
        manager.addStatusInfo(status);
        Assert.assertNull(status.currentExperimentId);
        Assert.assertNull(status.currentBenchmarkUri);
        Model resultModel = ((DummyStorageServiceClient) controller.storage).insertedModel;
        Assert.assertTrue(
                resultModel.contains(resultModel.getResource("http://w3id.org/hobbit/experiments#" + EXPERIMENT_ID),
                        HOBBIT.terminatedWithError, HobbitErrors.ExperimentTookTooMuchTime));
    }

    @After
    public void close() {
        IOUtils.closeQuietly(manager);
    }

    private static class DummyPlatformController extends PlatformController {

        public DummyPlatformController(Semaphore benchmarkControllerTerminated) {
            imageManager = new DummyImageManager();
            containerManager = new DummyContainerManager(benchmarkControllerTerminated, this);
            queue = new InMemoryQueue();
            storage = new DummyStorageServiceClient();
        }

        @Override
        public void init() throws Exception {
            // do not init the super class
        }

        @Override
        public void analyzeExperiment(String uri) throws IOException {
            // nothing to do
        }

        @Override
        public void notifyTermination(String containerId, int exitCode) {
            expManager.notifyTermination(containerId, exitCode);
        }
    }

    private static class DummyImageManager implements ImageManager {

        @Override
        public List<BenchmarkMetaData> getBenchmarks() {
            return new ArrayList<>(0);
        }

        @Override
        public List<SystemMetaData> getSystemsOfUser(String userName) {
            return new ArrayList<>(0);
        }

        @Override
        public List<SystemMetaData> getSystemsForBenchmark(String benchmarkUri) {
            return new ArrayList<>(0);
        }

        @Override
        public List<SystemMetaData> getSystemsForBenchmark(Model benchmarkModel) {
            return new ArrayList<>(0);
        }

        @Override
        public Model getBenchmarkModel(String benchmarkUri) {
            return ModelFactory.createDefaultModel();
        }

        @Override
        public Model getSystemModel(String systemUri) {
            return ModelFactory.createDefaultModel();
        }

        @Override
        public String getBenchmarkImageName(String benchmarkUri) {
            return benchmarkUri;
        }

        @Override
        public String getSystemImageName(String systemUri) {
            return systemUri;
        }

    }

    private static class DummyContainerManager implements ContainerManager {

        private Semaphore benchmarkControllerTerminated;
        private ContainerTerminationCallback terminationCallback;

        public DummyContainerManager(Semaphore benchmarkControllerTerminated,
                ContainerTerminationCallback terminationCallback) {
            this.benchmarkControllerTerminated = benchmarkControllerTerminated;
            this.terminationCallback = terminationCallback;
        }

        @Override
        public String startContainer(String imageName) {
            return imageName;
        }

        @Override
        public String startContainer(String imageName, String[] command) {
            return imageName;
        }

        @Override
        public String startContainer(String imageName, String type, String parent) {
            return imageName;
        }

        @Override
        public String startContainer(String imageName, String containerType, String parentId, String[] command) {
            return imageName;
        }

        @Override
        public String startContainer(String imageName, String containerType, String parentId, String[] env,
                String[] command) {
            return imageName;
        }

        @Override
        public void stopContainer(String containerId) {
            // Check whether the benchmark controller has been terminated
            if (containerId.equals(BENCHMARK_NAME)) {
                // Release the mutex for the main method
                benchmarkControllerTerminated.release();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    terminationCallback.notifyTermination(containerId, 137);
                }
            }).start();
        }

        @Override
        public void removeContainer(String containerId) {
        }

        @Override
        public void stopParentAndChildren(String parentId) {
            stopContainer(parentId);
        }

        @Override
        public void removeParentAndChildren(String parentId) {
        }

        @Override
        public ContainerInfo getContainerInfo(String containerId) {
            return null;
        }

        @Override
        public List<Container> getContainers() {
            return new ArrayList<>(0);
        }

        @Override
        public String getContainerId(String name) {
            return name;
        }

        @Override
        public String getContainerName(String containerId) {
            return containerId;
        }

        @Override
        public void addContainerObserver(ContainerStateObserver containerObserver) {
        }

    }

    private static class DummyStorageServiceClient extends StorageServiceClient {

        public Model insertedModel;

        public DummyStorageServiceClient() {
            super(null);
        }

        @Override
        public boolean sendInsertQuery(Model model, String graphURI) {
            insertedModel = model;
            return true;
        }

    }
}
