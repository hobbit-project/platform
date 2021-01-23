package org.hobbit.controller.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.hobbit.controller.data.SetupHardwareInformation;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.hobbit.controller.orchestration.ContainerManager;
import org.hobbit.controller.orchestration.ResourceInformationCollector;
import org.apache.jena.sparql.vocabulary.DOAP;
import org.hobbit.core.Constants;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.MEXCORE;
import static org.junit.jupiter.api.Assertions.*;

import static org.junit.Assert.assertNotNull;

class K8sResourceInformationCollectorImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8sResourceInformationCollectorImplTest.class);

    KubernetesClient client = K8sUtility.getK8sClient();

    String imageName = "perl";
    String[] cmd = {"perl", "-Mbignum=bpi", "-wle", "print bpi(2000)"};

    protected static final String busyboxImageName = "busybox:latest";
    protected static final String[] sleepCommand = { "sleep", "60s" };

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    ContainerManager manager = new K8sContainerManagerImpl();

    @BeforeEach
    public void setUp() {
        environmentVariables.set(
            K8sResourceInformationCollectorImpl.PROMETHEUS_HOST_KEY,
            K8sResourceInformationCollectorImpl.PROMETHEUS_HOST_DEFAULT);
        environmentVariables.set(
            K8sResourceInformationCollectorImpl.PROMETHEUS_PORT_KEY,
            K8sResourceInformationCollectorImpl.PROMETHEUS_PORT_DEFAULT);
    }


    @Test
    void getSystemUsageInformation() throws  Exception {
        String containerId = manager.startContainer(imageName,
            Constants.CONTAINER_TYPE_SYSTEM, null, cmd);
        assertNotNull("Container ID", containerId);

        LOGGER.info("Waiting...");
        Thread.sleep(10000);

        ResourceInformationCollector collector = new K8sResourceInformationCollectorImpl(manager);
        LOGGER.info("Requesting system usage information...");
        ResourceUsageInformation usage = collector.getSystemUsageInformation();

        assertNotNull("System usage information", usage);
        LOGGER.info("Got {}", usage);

        assertNotNull("CPU stats", usage.getCpuStats());
        assertTrue(usage.getCpuStats().getTotalUsage() > 0);
        assertNotNull("Memory stats", usage.getMemoryStats());
        assertTrue(usage.getMemoryStats().getUsageSum() > 0);
        assertNotNull("Disk stats", usage.getDiskStats());
        assertTrue(usage.getDiskStats().getFsSizeSum() > 0);

        // Generate a second container
        LOGGER.info("Creating second container...");
        containerId = manager.startContainer(imageName,
            Constants.CONTAINER_TYPE_SYSTEM, null, cmd);
        assertNotNull("Container ID", containerId);

        LOGGER.info("Waiting...");
        Thread.sleep(10000);

        LOGGER.info("Requesting system usage information...");
        ResourceUsageInformation usage2 = collector.getSystemUsageInformation();
        assertNotNull("System usage information", usage2);
        LOGGER.info("Got {}", usage2);

        assertNotNull("CPU stats", usage2.getCpuStats());
        assertTrue(usage2.getCpuStats().getTotalUsage() > 0);
        assertTrue(usage.getCpuStats().getTotalUsage()
            <= usage2.getCpuStats().getTotalUsage());
        assertNotNull("Memory stats", usage2.getMemoryStats());
        assertTrue(usage.getMemoryStats().getUsageSum()
            <= usage2.getMemoryStats().getUsageSum());
        assertNotNull("Disk stats", usage2.getDiskStats());
        assertTrue(usage.getDiskStats().getFsSizeSum()
            <= usage2.getDiskStats().getFsSizeSum());
    }

    @Test
    void testIncreasingFsSize() throws Exception {
        ResourceInformationCollector collector = new K8sResourceInformationCollectorImpl(manager);
        final String[] command = { "sh", "-c",
            "sleep 20s ; dd if=/dev/zero of=file.txt count=16024 bs=1048576 ; sleep 60s" };
        LOGGER.info("Creating container...");
        String containerId = manager.startContainer(busyboxImageName,
            Constants.CONTAINER_TYPE_SYSTEM, null, command);
        assertNotNull("Container ID", containerId);
        LOGGER.info("Waiting for the container {} to start...", containerId);
        Thread.sleep(10000);

        LOGGER.info("Requesting system usage information...");
        ResourceUsageInformation usage = collector.getSystemUsageInformation();

        assertNotNull("System usage information", usage);
        LOGGER.info("Got {}", usage);

        assertNotNull("CPU stats", usage.getCpuStats());
        /* FIXME cpu usage */
        assertTrue(usage.getCpuStats().getTotalUsage() > 0, "CPU usage > 0");
        assertNotNull("Memory stats", usage.getMemoryStats());
        assertTrue( usage.getMemoryStats().getUsageSum() > 0,"Memory usage > 0");
        assertNotNull("Disk stats", usage.getDiskStats());
        assertTrue( usage.getDiskStats().getFsSizeSum() > 0, "Disk fs size > 0");

        LOGGER.info("Waiting for the container {} to generate its file...",
            containerId);
        Thread.sleep(30000);

        LOGGER.info("Requesting system usage information...");
        ResourceUsageInformation usage2 = collector.getSystemUsageInformation();
        assertNotNull("System usage information", usage2);
        LOGGER.info("Got {}", usage2);

        assertNotNull("CPU stats", usage2.getCpuStats());
        assertTrue( usage2.getCpuStats().getTotalUsage() > 0, "CPU usage (after generating the file) > 0");
        // assertTrue("We expected that the CPU time used to generate the file
        // would increase the overall CPU time",
        // usage.getCpuStats().getTotalUsage() < usage2.getCpuStats().getTotalUsage());
        assertNotNull("Memory stats", usage2.getMemoryStats());
        // assertTrue(usage.getMemoryStats().getUsageSum() <
        // usage2.getMemoryStats().getUsageSum());
        assertNotNull("Disk stats", usage2.getDiskStats());
        // assertTrue("We expected that the Fssize would be increased when
        // generating a huge file",
        // usage.getDiskStats().getFsSizeSum() < usage2.getDiskStats().getFsSizeSum());
        assertTrue((usage.getMemoryStats().getUsageSum()
                + usage.getDiskStats().getFsSizeSum())
                < (usage2.getMemoryStats().getUsageSum()
                + usage2.getDiskStats().getFsSizeSum()), "We expected that the consumed memory would be increased when generating a huge file");
    }

    @Test
    void getHardwareInformation() throws Exception{
        LOGGER.info("Requesting hardware information...");
        ResourceInformationCollector collector = new K8sResourceInformationCollectorImpl(manager);
        SetupHardwareInformation setupInfo = collector.getHardwareInformation();
        assertNotNull("Hardware information", setupInfo);
        LOGGER.info("Got {}", setupInfo);
        Model m = ModelFactory.createDefaultModel();
        setupInfo.addToModel(m);

        int nodesInSwarm = client.nodes().list().getItems().size();

        assertEquals(nodesInSwarm,
            m.listObjectsOfProperty(HOBBIT.comprises).toList().size(),"Number of hobbit:comprises properties is equal to the number of nodes in the swarm");

        assertEquals(nodesInSwarm,
            m.listSubjectsWithProperty(RDF.type, MEXCORE.HardwareConfiguration).toList().size(),"Number of mexcore:HardwareConfiguration resources is equal to the number of nodes in the swarm");

        assertEquals(nodesInSwarm,
            m.listObjectsOfProperty(RDFS.label).toList().size(), "Number of rdfs:label properties is equal to the number of nodes in the swarm");

        assertEquals(nodesInSwarm,
            m.listObjectsOfProperty(MEXCORE.cpu).toList().size(), "Number of mexcore:cpu properties is equal to the number of nodes in the swarm");

        assertEquals(nodesInSwarm,
            m.listObjectsOfProperty(MEXCORE.memory).toList().size(), "Number of mexcore:memory properties is equal to the number of nodes in the swarm");

        assertEquals(nodesInSwarm,
            m.listObjectsOfProperty(DOAP.os).toList().size(), "Number of doap:os properties is equal to the number of nodes in the swarm");

        assertEquals(1 + 6 * nodesInSwarm,
            m.listStatements().toList().size(), "Total number of statements in hardware information model");
    }
}
