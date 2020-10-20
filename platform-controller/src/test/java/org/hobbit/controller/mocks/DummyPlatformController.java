package org.hobbit.controller.mocks;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.hobbit.controller.PlatformController;
import org.hobbit.controller.queue.InMemoryQueue;

public class DummyPlatformController extends PlatformController {

    public DummyPlatformController() {
        this(new Semaphore(0));
    }

    public DummyPlatformController(Semaphore benchmarkControllerTerminated) {
        imageManager = new DummyImageManager();
        containerManager = new DummyContainerManager(benchmarkControllerTerminated, this);
        resInfoCollector = new DummyResourceInformationCollector();
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
    public void notifyTermination(String containerId, long exitCode) {
        expManager.notifyTermination(containerId, exitCode);
    }

    @Override
    protected void sendToCmdQueue(String address, byte command, byte[] data, BasicProperties props)
            throws IOException {
        // nothing to do
        // receiveCommand(command, data, address, null);
    }
}
