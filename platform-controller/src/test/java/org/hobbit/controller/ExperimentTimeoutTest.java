package org.hobbit.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.hobbit.controller.docker.ContainerManager;
import org.hobbit.controller.docker.ContainerStateObserver;
import org.hobbit.controller.docker.ContainerTerminationCallback;
import org.hobbit.controller.docker.ImageManager;
import org.hobbit.controller.queue.InMemoryQueue;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.core.data.status.ControllerStatus;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitErrors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.messages.ContainerStats;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.Task.Criteria;

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
    private static final String SYSTEM_URI = "systemUri";

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
        controller.queue.add(new ExperimentConfiguration(EXPERIMENT_ID, BENCHMARK_NAME, "{}", SYSTEM_URI));
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

    private static class DummyPlatformController extends PlatformController {

        public DummyPlatformController(Semaphore benchmarkControllerTerminated) {
            imageManager = new DummyImageManager();
            containerManager = new DummyContainerManager(benchmarkControllerTerminated, this);
            queue = new InMemoryQueue();
            storage = new DummyStorageServiceClient();
            try {
                clusterManager = new ClusterManagerImpl();
            } catch (DockerCertificateException e) {
                e.printStackTrace();
            }
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
        
        @Override
        protected void sendToCmdQueue(String address, byte command, byte[] data, BasicProperties props)
                throws IOException {
            // nothing to do
//            receiveCommand(command, data, address, null);
        }
    }

    private static class DummyImageManager implements ImageManager {

        @Override
        public List<BenchmarkMetaData> getBenchmarks() {
            List<BenchmarkMetaData> result = new ArrayList<>();
            BenchmarkMetaData meta = new BenchmarkMetaData();
            meta.uri = BENCHMARK_NAME;
            meta.name = BENCHMARK_NAME;
            meta.mainImage = BENCHMARK_NAME;
            meta.usedImages = new HashSet<>();
            meta.usedImages.add("benchmarkImage1");
            meta.usedImages.add("benchmarkImage2");
            meta.rdfModel = ModelFactory.createDefaultModel();
            result.add(meta);
            return result;
        }

        @Override
        public List<SystemMetaData> getSystems() {
            List<SystemMetaData> result = new ArrayList<>();
            SystemMetaData meta = new SystemMetaData();
            meta.uri = SYSTEM_URI;
            meta.name = meta.uri;
            meta.mainImage = "SystemImage";
            meta.usedImages = new HashSet<>();
            meta.usedImages.add("SystemImage1");
            meta.usedImages.add("SystemImage2");
            meta.rdfModel = ModelFactory.createDefaultModel();
            result.add(meta);
            meta = new SystemMetaData();
            meta.uri = "wrong_" + SYSTEM_URI;
            meta.name = meta.uri;
            meta.mainImage = "wrong_SystemImage";
            meta.usedImages = new HashSet<>();
            meta.usedImages.add("wrong_SystemImage1");
            meta.usedImages.add("wrong_SystemImage2");
            meta.rdfModel = ModelFactory.createDefaultModel();
            result.add(meta);
            return result;
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
        public String startContainer(String imageName, String containerType, String parentId, String[] env,
                                     String[] command, String experimentId) {
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
            stopContainer(parentId);
        }

        @Override
        public Task getContainerInfo(String containerId) {
            return null;
        }

        @Override
        public List<Task> getContainers(Criteria criteria) {
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

        @Override
        public void pullImage(String imageName) {
            System.out.print("Pulling Image (fake) ");
            System.out.print(imageName);
            System.out.println("...");
        }

        @Override
        public ContainerStats getStats(String containerId) {
            return null;
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
