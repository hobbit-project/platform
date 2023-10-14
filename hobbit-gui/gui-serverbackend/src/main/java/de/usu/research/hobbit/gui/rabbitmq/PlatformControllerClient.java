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
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.hobbit.core.Constants;
import org.hobbit.core.FrontEndApiCommands;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.core.data.status.ControllerStatus;
import org.hobbit.core.data.status.QueuedExperiment;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.RabbitRpcClient;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitExperiments;
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
import de.usu.research.hobbit.gui.rest.beans.HardwareConstraintBean;
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
     * @throws ShutdownSignalException    If something goes wrong with the request
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
        BenchmarkBean bean;
        for (BenchmarkMetaData benchmark : benchmarks) {
            bean = new BenchmarkBean(benchmark.uri, benchmark.name, benchmark.description);
            if ((benchmark.defError != null) && (!benchmark.defError.isEmpty())) {
                bean.setErrorMessage(benchmark.defError);
            }
            benchmarkBeans.add(bean);
        }

        LOGGER.debug(Arrays.toString(benchmarkBeans.toArray()));
        LOGGER.info("Sending response to GUI...");

        return benchmarkBeans;
    }

    /**
     * Retrieves the benchmark details from the HOBBIT PlatformControler
     *
     * @param benchmarkUri the URI of the benchmark for which the details should be
     *                     retrieved
     * @param user         information about the requesting user which will be used
     *                     to filter the systems that can be used with the requested
     *                     benchmark.
     * @return
     * @throws GUIBackendException
     * @throws IOException
     * @throws InterruptedException
     * @throws ConsumerCancelledException
     * @throws ShutdownSignalException
     */
    public BenchmarkBean requestBenchmarkDetails(String benchmarkUri, UserInfoBean user) throws GUIBackendException,
            IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        // Map<String, String> env = System.getenv();
        if (benchmarkUri == null) {
            String msg = "Benchmark URI is null. Aborting.";
            LOGGER.error(msg);
            throw new GUIBackendException(msg);
        }

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
        List<SystemBean> systemBeans = new ArrayList<>();
        try {
            // parse the response
            ByteBuffer buffer = ByteBuffer.wrap(data);
            benchmarkModel = RabbitMQUtils.readModel(buffer);
            String jsonString = RabbitMQUtils.readString(buffer);
            systemBeans = getSystemBeansFromJson(jsonString);
        } catch (Exception e) {
            throw new IOException("Error while parsing benchmark model.", e);
        }

        BenchmarkBean benchmarkDetails = RdfModelHelper.createBenchmarkBean(benchmarkModel);
        if (benchmarkDetails == null) {
            throw new IOException("Error while parsing benchmark model.");
        }
        benchmarkDetails.setSystems(systemBeans);

        return benchmarkDetails;
    }

    /**
     * Sends the given benchmark configuration to the platform controller where an
     * experiment will be started with the chosen system and benchmark
     * configuration.
     *
     * @param benchmarkConf the benchmark configuration with which an experiment
     *                      should be started
     * @param userName      the name of the user who submitted the benchmark
     *                      configuration
     * @return The ID of the created experiment
     * @throws GUIBackendException If the given benchmark configuration is not valid
     * @throws IOException         If there is a problem during the receiving of the
     *                             response
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

        model.add(HobbitExperiments.New, RDF.type, HOBBIT.Experiment);
        model.add(HobbitExperiments.New, HOBBIT.involvesBenchmark, model.createResource(benchmarkUri));
        model.add(HobbitExperiments.New, HOBBIT.involvesSystemInstance, model.createResource(systemUri));

        try {
            model = addParameters(model, HobbitExperiments.New, benchmarkConf.getConfigurationParams());
        } catch (Exception e) {
            LOGGER.error("Got an exception while processing the parameters.", e);
            throw new GUIBackendException("Please check your parameter definitions.");
        }

        try {
            addHardwareConstraint(model, HobbitExperiments.New, benchmarkConf.getMaxHardwareConstraints());
        } catch (Exception e) {
            LOGGER.error("Got an exception while processing the hardware constraints.", e);
            throw new GUIBackendException("Please check your parameter definitions.");
        }

        byte[] data = RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.ADD_EXPERIMENT_CONFIGURATION },
                new byte[][] { RabbitMQUtils.writeString(benchmarkUri), RabbitMQUtils.writeString(systemUri),
                        RabbitMQUtils.writeModel(model), RabbitMQUtils.writeString(userName) },
                null);

        LOGGER.info("Sending request...");
        data = client.request(data);
        if (data == null) {
            throw new IOException("Didn't get a response.");
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
     * @param model              the RDF model to which the parameter should be
     *                           added
     * @param experimentInstance the resource of the new experiment inside the given
     *                           RDF model
     * @param list               the list of parameters that should be added
     * @return the updated model
     */
    protected static Model addParameters(Model model, Resource experimentInstance,
            List<ConfigurationParamValueBean> list) {
        for (ConfigurationParamValueBean paramValue : list) {
            String uri = paramValue.getId();
            String datatype = Datatype.getValue(paramValue.getDatatype());
            String value = paramValue.getValue();
            String range = paramValue.getRange();

            if (range == null) {
                model.add(experimentInstance, model.createProperty(uri),
                        model.createTypedLiteral(value, expandedXsdId(datatype)));
            } else {
                if (range.startsWith(XSD.NS)) {
                    model.add(experimentInstance, model.createProperty(uri), model.createTypedLiteral(value, range));
                } else {
                    model.add(experimentInstance, model.createProperty(uri), model.createResource(value));
                }
            }
        }
        return model;
    }

    /**
     * Add hardware constraints defined in the given bean to the given model.
     * 
     * @param model              the RDF model to which the constraints should be
     *                           added
     * @param experimentInstance the resource of the new experiment for which the
     *                           constraint should hold inside the given RDF model
     * @param hardwareConstraint the constraint (or {@code null} if such a
     *                           constraint doesn't exist)
     * @return the given, updated model
     */
    protected static Model addHardwareConstraint(Model model, Resource experimentInstance,
            HardwareConstraintBean hardwareConstraint) {
        if ((hardwareConstraint != null) && (hardwareConstraint.isValidConstraint())) {
            Resource constraintsResource = model.createResource(hardwareConstraint.getIri());
            model.add(constraintsResource, RDF.type, HOBBIT.Hardware);
            String label = hardwareConstraint.getLabel();
            if ((label != null) && (!label.isEmpty())) {
                model.addLiteral(constraintsResource, RDFS.label, model.createLiteral(label));
            }
            int cpuCount = hardwareConstraint.getCpuCount();
            if (cpuCount > 0) {
                model.addLiteral(constraintsResource, HOBBIT.hasCPUTypeCount, cpuCount);
            }
            long memory = hardwareConstraint.getMemory();
            if (memory > 0) {
                model.addLiteral(constraintsResource, HOBBIT.hasMemory, memory);
            }
            model.add(experimentInstance, HOBBIT.maxHardware, constraintsResource);
        }
        return model;
    }

    /**
     * Requests the status of the controller.
     *
     * @return the status of the controller
     * @throws IOException If no response has been received
     */
    public ControllerStatus requestStatus(String userName) throws IOException {
        byte[] data = client
                .request(RabbitMQUtils.writeByteArrays(new byte[] { FrontEndApiCommands.LIST_CURRENT_STATUS },
                        new byte[][] { RabbitMQUtils.writeString(userName) }, null));
        if (data == null) {
            throw new IOException("Didn't get a response.");
        }
        // parse the response
        String response = RabbitMQUtils.readString(data);
        System.out.println(response);
        ControllerStatus status = gson.fromJson(response, ControllerStatus.class);
        if (status.queuedExperiments == null)
            status.queuedExperiments = new QueuedExperiment[0];
        return status;
    }

    /**
     * Closes the challenge with the given URI.
     *
     * @param challengeUri the URI of the challenge that should be closed
     * @throws IOException If the controller does not responses
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
     * @param userMail the mail address of the user for which the systems should be
     *                 requested
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
        List<SystemBean> systemBeans = getSystemBeansFromJson(RabbitMQUtils.readString(response));
        return systemBeans;
    }

    protected List<SystemBean> getSystemBeansFromJson(String json) {
        Collection<SystemMetaData> systems = gson.fromJson(json, new TypeToken<Collection<SystemMetaData>>() {
        }.getType());
        List<SystemBean> systemBeans = new ArrayList<>();
        transformSysMetaDataToBean(systems, systemBeans);
        return systemBeans;
    }

    protected void transformSysMetaDataToBean(Collection<SystemMetaData> systems, List<SystemBean> systemBeans) {
        SystemBean bean;
        if (systems != null) {
            for (SystemMetaData system : systems) {
                bean = new SystemBean(system.uri, system.name, system.description);
                if ((system.defError != null) && (!system.defError.isEmpty())) {
                    bean.setErrorMessage(system.defError);
                }
                systemBeans.add(bean);
            }
        }
    }

    /**
     * Requests the deletion of the experiment with the given experiment id with the
     * access rights of the given user.
     *
     * @param id       id of the experiment that should be removed
     * @param userName name of the user requesting the deletion
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
     * @param experimentId      the id of the experiment that should be terminated.
     * @param preferredUsername the name of the user who wants to terminate the
     *                          experiment
     * @return {@code true} if the termination was successful, else {@code false}
     * @throws IOException If communication problems arise.
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
