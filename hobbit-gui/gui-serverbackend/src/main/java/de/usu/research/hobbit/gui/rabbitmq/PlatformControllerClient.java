package de.usu.research.hobbit.gui.rabbitmq;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.Constants;
import org.hobbit.core.FrontEndApiCommands;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.ConfiguredExperiment;
import org.hobbit.core.data.ControllerStatus;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.RabbitRpcClient;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.ShutdownSignalException;

import de.usu.research.hobbit.gui.rest.BenchmarkBean;
import de.usu.research.hobbit.gui.rest.ConfigurationParamValueBean;
import de.usu.research.hobbit.gui.rest.Datatype;
import de.usu.research.hobbit.gui.rest.SubmitModelBean;
import de.usu.research.hobbit.gui.rest.SystemBean;

/**
 * Managing the connection to the HOBBIT RabbitMQ instance (partly based on
 * org.hobbit.controller.test.RequestBenchmarkDetails and
 * org.hobbit.controller.test.RequestBenchmarks)
 * 
 * @author Roman Korf
 *
 */
public class PlatformControllerClient implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformControllerClient.class);

    public static PlatformControllerClient create(Connection connection) {
        PlatformControllerClient platformClient = null;
        try {
            RabbitRpcClient client = RabbitRpcClient.create(connection, Constants.FRONT_END_2_CONTROLLER_QUEUE_NAME);
            platformClient = new PlatformControllerClient(client);
        } catch (IOException e) {
            LOGGER.error("Exception while trying to create RabbitRpcClient. Returning null.", e);
            e.printStackTrace();
        }
        return platformClient;
    }

    private RabbitRpcClient client;

    protected PlatformControllerClient(RabbitRpcClient client) {
        this.client = client;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.usu.research.hobbit.gui.rabbitmq.IRabbitMQConnection#close()
     */
    @Override
    public void close() throws IOException {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Retrieves the benchmarks registered at the HOBBIT PlatformController
     * 
     * @return A list of benchmarks
     * @throws IOException
     * @throws InterruptedException
     * @throws ConsumerCancelledException
     * @throws ShutdownSignalException
     * @throws Exception
     *             If something goes wrong with the request
     */
    public List<BenchmarkBean> requestBenchmarks() throws IOException, ShutdownSignalException,
            ConsumerCancelledException, InterruptedException {
        LOGGER.info("Sending request...");
        byte[] data = client.request(new byte[] { FrontEndApiCommands.LIST_AVAILABLE_BENCHMARKS });
        if (data == null) {
            throw new IOException("Didn't got a response.");
        }

        LOGGER.info("Parsing response...");
        // parse the response
        String jsonString = RabbitMQUtils.readString(data);
        Gson gson = new Gson();
        Collection<BenchmarkMetaData> benchmarks = gson.fromJson(jsonString,
                new TypeToken<Collection<BenchmarkMetaData>>() {
                }.getType());

        LOGGER.info("Preparing response for GUI...");
        // Create output
        List<BenchmarkBean> benchmarkBeans = new ArrayList<BenchmarkBean>();

        for (BenchmarkMetaData benchmark : benchmarks) {
            benchmarkBeans.add(new BenchmarkBean(benchmark.benchmarkUri, benchmark.benchmarkName,
                    benchmark.benchmarkDescription));
        }

        LOGGER.debug(Arrays.toString(benchmarkBeans.toArray()));
        LOGGER.info("Sending response to GUI...");

        return benchmarkBeans;
    }

    /**
     * Retrieves the benchmark details from the HOBBIT PlatformControler
     * 
     * @param benchmarkUri
     * @return
     * @throws GUIBackendException
     * @throws IOException
     * @throws InterruptedException
     * @throws ConsumerCancelledException
     * @throws ShutdownSignalException
     * @throws Exception
     */
    public BenchmarkBean requestBenchmarkDetails(String benchmarkUri) throws GUIBackendException, IOException,
            ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        LOGGER.info("Sending request...");
        // Map<String, String> env = System.getenv();
        if (benchmarkUri == null) {
            String msg = "Benchmark URI is null. Aborting.";
            LOGGER.error(msg);
            throw new GUIBackendException(msg);
        }
        LOGGER.info("Sending request...");
        byte[] data = client.request(RabbitMQUtils.writeByteArrays(
                new byte[] { FrontEndApiCommands.GET_BENCHMARK_DETAILS },
                new byte[][] { RabbitMQUtils.writeString(benchmarkUri) }, null));
        if (data == null) {
            throw new IOException("Didn't got a response.");
        }

        LOGGER.info("Parsing response...");
        // parse the response
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Model benchmarkModel = RabbitMQUtils.readModel(buffer);
        String jsonString = RabbitMQUtils.readString(buffer);
        Gson gson = new Gson();
        Collection<SystemMetaData> systems = gson.fromJson(jsonString, new TypeToken<Collection<SystemMetaData>>() {
        }.getType());

        BenchmarkBean benchmarkDetails = RdfModelHelper.createBenchmarkBean(benchmarkModel);
        if (benchmarkDetails == null) {
            throw new IOException("Error while parsing benchmark model.");
        }

        // Parse Benchmark System Details
        LOGGER.info("Adding systems for GUI...");
        benchmarkDetails.setSystems(new ArrayList<>());
        for (SystemMetaData system : systems) {
            benchmarkDetails.getSystems().add(
                    new SystemBean(system.systemUri, system.systemName, system.systemDescription));
        }

        LOGGER.info("Sending response to GUI...");
        return benchmarkDetails;
    }

    public String submitBenchmark(SubmitModelBean benchmarkConf) throws GUIBackendException, IOException,
            ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        String benchmarkUri = benchmarkConf.getBenchmark();
        String systemUri = benchmarkConf.getSystem();

        if (benchmarkUri == null) {
            String msg = "Benchmark URI is null. Aborting.";
            LOGGER.error(msg);
            throw new GUIBackendException(msg);
        }

        if (systemUri == null) {
            String msg = "System URI is null. Aborting.";
            LOGGER.error(msg);
            throw new GUIBackendException(msg);
        }

        LOGGER.info("Creating model...");
        Model model = ModelFactory.createDefaultModel();

        String benchmarkInstanceId = Constants.NEW_EXPERIMENT_URI;
        Resource benchmarkInstanceResource = model.createResource(benchmarkInstanceId);
        model.add(benchmarkInstanceResource, RDF.type, HOBBIT.Experiment);
        model.add(benchmarkInstanceResource, HOBBIT.involvesBenchmark, model.createResource(benchmarkUri));
        model.add(benchmarkInstanceResource, HOBBIT.involvesSystemInstance, model.createResource(systemUri));

        model = addParameters(model, benchmarkInstanceResource, benchmarkConf.getConfigurationParams());

        byte[] data = RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.ADD_EXPERIMENT_CONFIGURATION },
                new byte[][] { RabbitMQUtils.writeString(benchmarkUri), RabbitMQUtils.writeString(systemUri),
                        RabbitMQUtils.writeModel(model) }, null);

        LOGGER.info("Sending request...");
        data = client.request(data);
        if (data == null) {
            throw new IOException("Didn't got a response.");
        }

        String id = RabbitMQUtils.readString(data);
        // parse the response
        LOGGER.info("Response: " + id);

        return id;
    }

    private static Model addParameters(Model model, Resource benchmarkInstanceResource,
            List<ConfigurationParamValueBean> list) {
        for (ConfigurationParamValueBean paramValue : list) {
            String uri = paramValue.getId();
            String datatype = Datatype.getValue(paramValue.getDatatype());
            String value = paramValue.getValue();
            String range = paramValue.getRange();

            if (range == null) {
                model.add(benchmarkInstanceResource, model.createProperty(uri),
                        model.createTypedLiteral(value, expandedXsdId(datatype)));
            } else {
                model.add(benchmarkInstanceResource, model.createProperty(uri), model.createResource(value));
            }
        }

        StringWriter writer = new StringWriter();
        model.write(writer, "Turtle");

        return model;
    }

    public String requestStatus() throws IOException, ShutdownSignalException, ConsumerCancelledException,
            InterruptedException {
        LOGGER.info("Sending request...");
        byte[] data = client.request(new byte[] { FrontEndApiCommands.LIST_CURRENT_STATUS });
        if (data == null) {
            throw new IOException("Didn't got a response.");
        }
        // parse the response
        String response = RabbitMQUtils.readString(data);
        Gson gson = new Gson();
        ControllerStatus status = gson.fromJson(response, ControllerStatus.class);
        // print results
        StringBuilder builder = new StringBuilder();
        builder.append("currentExperiment:\n\texperiment id: ");
        builder.append(status.currentExperimentId);
        builder.append("\n\tbenchmark URI: ");
        builder.append(status.currentBenchmarkUri);
        builder.append("\n\tbenchmark name: ");
        builder.append(status.currentBenchmarkName);
        builder.append("\n\tsystem Uri: ");
        builder.append(status.currentSystemUri);
        builder.append("\n\tstatus: ");
        builder.append(status.currentStatus);
        if (status.queue != null) {
            builder.append("\n\nqueue:\n");
            for (ConfiguredExperiment exp : status.queue) {
                builder.append("\n\tbenchmark name: ");
                builder.append(exp.benchmarkName);
                builder.append("\n\tsystem name: ");
                builder.append(exp.systemName);
                builder.append("\n");
            }
        }
        String statusStr = builder.toString();
        LOGGER.info(statusStr);
        return statusStr;
    }

    public static void main(String[] argv) throws Exception {
        try {
            final RabbitMQConnection connection = RabbitMQConnectionSingleton.getConnection();
            Thread[] threads = new Thread[5];
            final CountDownLatch latch = new CountDownLatch(threads.length);
            for (int j = 0; j < threads.length; j++) {
                threads[j] = new Thread() {
                    public void run() {
                        try {
                            for (int i = 0; i < 3; i++) {
                                long start = System.currentTimeMillis();

                                PlatformControllerClient client = PlatformControllerClient.create(connection
                                        .getConnection());

                                try {
                                    // LOGGER.info("Status");
                                    // connectionMgr.requestStatus();

                                    LOGGER.info("Request Benchmarks");
                                    List<BenchmarkBean> benchmarks = client.requestBenchmarks();
                                    LOGGER.info("Request BenchmarkDetails");
                                    client.requestBenchmarkDetails(benchmarks.get(0).id);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                long diff = System.currentTimeMillis() - start;
                                LOGGER.info("time: " + diff);
                            }
                        } finally {
                            latch.countDown();
                        }
                    }
                };
            }
            for (int j = 0; j < threads.length; j++) {
                threads[j].start();
            }
            latch.await();
        } finally {
            RabbitMQConnectionSingleton.shutdown();
        }
    }

    private static String expandedXsdId(String id) {
        String prefix = id.substring(0, id.indexOf(":"));
        return id.replace(prefix + ":", "http://www.w3.org/2001/XMLSchema#");
    }

}
