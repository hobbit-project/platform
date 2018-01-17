/**
 * This file is part of gui-serverbackend.
 *
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import org.apache.jena.vocabulary.XSD;
import org.hobbit.core.Constants;
import org.hobbit.core.FrontEndApiCommands;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.core.data.status.ControllerStatus;
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

import de.usu.research.hobbit.gui.rest.Datatype;
import de.usu.research.hobbit.gui.rest.beans.BenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.ConfigurationParamValueBean;
import de.usu.research.hobbit.gui.rest.beans.SubmitModelBean;
import de.usu.research.hobbit.gui.rest.beans.SystemBean;
import de.usu.research.hobbit.gui.rest.beans.UserInfoBean;

/**
 * Managing the connection to the HOBBIT RabbitMQ instance (partly based on
 * org.hobbit.controller.test.RequestBenchmarkDetails and
 * org.hobbit.controller.test.RequestBenchmarks)
 *
 * @author Roman Korf
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class PlatformControllerClient implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformControllerClient.class);

    public static PlatformControllerClient create(Connection connection) {
        if (connection == null) {
            LOGGER.error("Got no RabbitMQ Connection object. Returning null.");
            return null;
        }
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
    private Gson gson = new Gson();

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
     *             If something goes wrong with the request
     */
    public List<BenchmarkBean> requestBenchmarks()
            throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        LOGGER.info("Sending request...");
        byte[] data = client.request(new byte[] { FrontEndApiCommands.LIST_AVAILABLE_BENCHMARKS });
        if (data == null) {
            throw new IOException("Didn't got a response.");
        }

        LOGGER.info("Parsing response...");
        // parse the response
        String jsonString = RabbitMQUtils.readString(data);
        Collection<BenchmarkMetaData> benchmarks = gson.fromJson(jsonString,
                new TypeToken<Collection<BenchmarkMetaData>>() {
                }.getType());

        LOGGER.info("Preparing response for GUI...");
        // Create output
        List<BenchmarkBean> benchmarkBeans = new ArrayList<BenchmarkBean>();

        for (BenchmarkMetaData benchmark : benchmarks) {
            benchmarkBeans.add(new BenchmarkBean(benchmark.uri, benchmark.name, benchmark.description));
        }

        LOGGER.debug(Arrays.toString(benchmarkBeans.toArray()));
        LOGGER.info("Sending response to GUI...");

        return benchmarkBeans;
    }

    /**
     * Retrieves the benchmark details from the HOBBIT PlatformControler
     *
     * @param benchmarkUri
     *            the URI of the benchmark for which the details should be retrieved
     * @param user
     *            information about the requesting user which will be used to filter
     *            the systems that can be used with the requested benchmark.
     * @return
     * @throws GUIBackendException
     * @throws IOException
     * @throws InterruptedException
     * @throws ConsumerCancelledException
     * @throws ShutdownSignalException
     */
    public BenchmarkBean requestBenchmarkDetails(String benchmarkUri, UserInfoBean user) throws GUIBackendException,
            IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        LOGGER.info("Sending request...");
        // Map<String, String> env = System.getenv();
        if (benchmarkUri == null) {
            String msg = "Benchmark URI is null. Aborting.";
            LOGGER.error(msg);
            throw new GUIBackendException(msg);
        }
        LOGGER.info("Sending request...");

        byte[] data = null;
        if (user != null) {
            data = client.request(RabbitMQUtils.writeByteArrays(
                    new byte[] { FrontEndApiCommands.GET_BENCHMARK_DETAILS }, new byte[][] {
                            RabbitMQUtils.writeString(benchmarkUri), RabbitMQUtils.writeString(user.getEmail()) },
                    null));
        } else {
            data = client
                    .request(RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.GET_BENCHMARK_DETAILS },
                            new byte[][] { RabbitMQUtils.writeString(benchmarkUri) }, null));
        }
        if (data == null) {
            throw new IOException("Didn't got a response.");
        }

        Model benchmarkModel = null;
        Collection<SystemMetaData> systems = null;
        try {
            LOGGER.info("Parsing response...");
            // parse the response
            ByteBuffer buffer = ByteBuffer.wrap(data);
            benchmarkModel = RabbitMQUtils.readModel(buffer);
            String jsonString = RabbitMQUtils.readString(buffer);
            systems = gson.fromJson(jsonString, new TypeToken<Collection<SystemMetaData>>() {
            }.getType());
        } catch (Exception e) {
            throw new IOException("Error while parsing benchmark model.", e);
        }

        BenchmarkBean benchmarkDetails = RdfModelHelper.createBenchmarkBean(benchmarkModel);
        if (benchmarkDetails == null) {
            throw new IOException("Error while parsing benchmark model.");
        }

        // Parse Benchmark System Details
        LOGGER.info("Adding systems for GUI...");
        benchmarkDetails.setSystems(new ArrayList<>());
        if (systems != null) {
            for (SystemMetaData system : systems) {
                benchmarkDetails.getSystems().add(new SystemBean(system.uri, system.name, system.description));
            }
        }

        LOGGER.info("Sending response to GUI...");
        return benchmarkDetails;
    }

    /**
     * Sends the given benchmark configuration to the platform controller where an
     * experiment will be started with the chosen system and benchmark
     * configuration.
     * 
     * @param benchmarkConf
     *            the benchmark configuration with which an experiment should be
     *            started
     * @param userName
     *            the name of the user who submitted the benchmark configuration
     * @return The ID of the created experiment
     * @throws GUIBackendException
     *             If the given benchmark configuration is not valid
     * @throws IOException
     *             If there is a problem during the receiving of the response
     */
    public String submitBenchmark(SubmitModelBean benchmarkConf, String userName)
            throws GUIBackendException, IOException {
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

        try {
            model = addParameters(model, benchmarkInstanceResource, benchmarkConf.getConfigurationParams());
        } catch (Exception e) {
            LOGGER.error("Got an exception while processing the parameters.", e);
            throw new GUIBackendException("Please check your parameter definitions.");
        }

        byte[] data = RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.ADD_EXPERIMENT_CONFIGURATION },
                new byte[][] { RabbitMQUtils.writeString(benchmarkUri), RabbitMQUtils.writeString(systemUri),
                        RabbitMQUtils.writeModel(model), RabbitMQUtils.writeString(userName) },
                null);

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

    /**
     * Adds the given list of parameters to the given RDF model by creating triples
     * using the given benchmark resource.
     * 
     * @param model
     *            the RDF model to which the parameter should be added
     * @param benchmarkInstanceResource
     *            the resource of the benchmark inside the given RDF model
     * @param list
     *            the list of parameters that should be added
     * @return the updated model
     */
    protected static Model addParameters(Model model, Resource benchmarkInstanceResource,
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
                if (range.startsWith(XSD.NS)) {
                    model.add(benchmarkInstanceResource, model.createProperty(uri),
                            model.createTypedLiteral(value, range));
                } else {
                    model.add(benchmarkInstanceResource, model.createProperty(uri), model.createResource(value));
                }
            }
        }

        StringWriter writer = new StringWriter();
        model.write(writer, "Turtle");

        return model;
    }

    /**
     * Requests the status of the controller.
     * 
     * @return the status of the controller
     * @throws IOException
     *             If no response has been received
     */
    public ControllerStatus requestStatus() throws IOException {
        byte[] data = client.request(new byte[] { FrontEndApiCommands.LIST_CURRENT_STATUS });
        if (data == null) {
            throw new IOException("Didn't get a response.");
        }
        // parse the response
        String response = RabbitMQUtils.readString(data);
        System.out.println(response);
        ControllerStatus status = gson.fromJson(response, ControllerStatus.class);
        return status;
    }

    /**
     * Closes the challenge with the given URI.
     * 
     * @param challengeUri
     *            the URI of the challenge that should be closed
     * @throws IOException
     *             If the controller does not responses
     */
    public void closeChallenge(String challengeUri) throws IOException {
        LOGGER.info("Sending request...");
        byte[] res = client.request(RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.CLOSE_CHALLENGE },
                new byte[][] { RabbitMQUtils.writeString(challengeUri) }, null));
        if (res == null) {
            throw new IOException("Didn't get a response when trying to close the challenge");
        }
        String result = RabbitMQUtils.readString(res);
        LOGGER.info("Challenge " + challengeUri + " closed " + result);
    }

    /**
     * Requests the systems for the user with the given user mail address. Returns
     * an empty list if an error occurs.
     * 
     * @param userMail
     *            the mail address of the user for which the systems should be
     *            requested
     * @return the systems for the given user or an empty list if an error occurred
     */
    public List<SystemBean> requestSystemsOfUser(String userMail) {
        byte[] response = client
                .request(RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.GET_SYSTEMS_OF_USER },
                        new byte[][] { RabbitMQUtils.writeString(userMail) }, null));
        if (response == null) {
            LOGGER.info("Couldn't get the systems for user {}. Returning empty list.");
            return new ArrayList<>(0);
        }
        Collection<SystemMetaData> systems = gson.fromJson(RabbitMQUtils.readString(response),
                new TypeToken<Collection<SystemMetaData>>() {
                }.getType());
        List<SystemBean> systemBeans = new ArrayList<>();
        if (systems != null) {
            for (SystemMetaData system : systems) {
                systemBeans.add(new SystemBean(system.uri, system.name, system.description));
            }
        }
        return systemBeans;
    }

    /**
     * Requests the deletion of the experiment with the given experiment id with the
     * access rights of the given user.
     * 
     * @param id
     *            id of the experiment that should be removed
     * @param userName
     *            name of the user requesting the deletion
     * @return {@code true} if the deletion was successful, else {@code false}
     */
    public boolean requestExperimentDeletion(String id, String userName) {
        byte[] response = client
                .request(RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.REMOVE_EXPERIMENT },
                        new byte[][] { RabbitMQUtils.writeString(id), RabbitMQUtils.writeString(userName) }, null));
        if ((response == null) || (response.length == 0)) {
            LOGGER.info("Couldn't request the deletion of {} for {}. Returning false.", id, userName);
            return false;
        } else {
            return response[0] != 0;
        }
    }

    /*
     * Only for testing purposes.
     */
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

                                PlatformControllerClient client = PlatformControllerClient
                                        .create(connection.getConnection());

                                try {
                                    // LOGGER.info("Status");
                                    // connectionMgr.requestStatus();

                                    // LOGGER.info("Request Benchmarks");
                                    // List<BenchmarkBean> benchmarks =
                                    // client.requestBenchmarks();
                                    // LOGGER.info("Request BenchmarkDetails");
                                    // client.requestBenchmarkDetails(benchmarks.get(0).id);
                                    LOGGER.info("Request Systems for user DefaultHobbitUser");
                                    List<SystemBean> systems = client.requestSystemsOfUser("DefaultHobbitUser");
                                    for (SystemBean system : systems) {
                                        LOGGER.info("Found: {}, {}", system.getId(), system.getName());
                                    }
                                    LOGGER.info("Request Systems for user GerbilBenchmark");
                                    systems = client.requestSystemsOfUser("GerbilBenchmark");
                                    LOGGER.info("Found: {} systems", systems.size());
                                    for (SystemBean system : systems) {
                                        LOGGER.info("Found: {}, {}", system.getId(), system.getName());
                                    }
                                    LOGGER.info("Request Systems for user gerbil@informatik.uni-leipzig.de");
                                    systems = client.requestSystemsOfUser("gerbil@informatik.uni-leipzig.de");
                                    LOGGER.info("Found: {} systems", systems.size());
                                    for (SystemBean system : systems) {
                                        LOGGER.info("Found: {}, {}", system.getId(), system.getName());
                                    }
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
        if (!id.startsWith("http:")) {
            String prefix = id.substring(0, id.indexOf(":"));
            return id.replace(prefix + ":", XSD.NS);
        } else {
            return id;
        }
    }

    /**
     * Sends a request to the platform controller to terminate the experiment with
     * the given ID using the access rights of the given user.
     * 
     * @param experimentId
     *            the id of the experiment that should be terminated.
     * @param preferredUsername
     *            the name of the user who wants to terminate the experiment
     * @return {@code true} if the termination was successful, else {@code false}
     * @throws IOException
     *             If communication problems arise.
     */
    public boolean terminateExperiment(String experimentId, String preferredUsername) throws IOException {
        byte[] res = client.request(RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.REMOVE_EXPERIMENT },
                new byte[][] { RabbitMQUtils.writeString(experimentId), RabbitMQUtils.writeString(preferredUsername) },
                null));
        if (res == null) {
            throw new IOException("Didn't get a response when trying to terminate the challenge");
        }
        // If the result is not empty and the first byte is not 0, the removal was
        // successful
        return (res.length > 0) && (res[0] > 0);
    }

}
