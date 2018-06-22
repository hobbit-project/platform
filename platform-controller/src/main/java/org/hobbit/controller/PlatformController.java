/**
 * This file is part of platform-controller.
 *
 * platform-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * platform-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with platform-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.controller;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.controller.analyze.ExperimentAnalyzer;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.docker.ClusterManager;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.hobbit.controller.docker.ContainerManager;
import org.hobbit.controller.docker.ContainerManagerImpl;
import org.hobbit.controller.docker.ContainerStateObserver;
import org.hobbit.controller.docker.ContainerStateObserverImpl;
import org.hobbit.controller.docker.ContainerTerminationCallback;
import org.hobbit.controller.docker.GitlabBasedImageManager;
import org.hobbit.controller.docker.ImageManager;
import org.hobbit.controller.docker.ResourceInformationCollector;
import org.hobbit.controller.front.FrontEndApiHandler;
import org.hobbit.controller.health.ClusterHealthChecker;
import org.hobbit.controller.health.ClusterHealthCheckerImpl;
import org.hobbit.controller.queue.ExperimentQueue;
import org.hobbit.controller.queue.ExperimentQueueImpl;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.FrontEndApiCommands;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.StartCommandData;
import org.hobbit.core.data.StopCommandData;
import org.hobbit.core.data.SystemMetaData;
import org.hobbit.core.data.status.ControllerStatus;
import org.hobbit.core.data.status.QueuedExperiment;
import org.hobbit.core.data.status.RunningExperiment;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.RabbitQueueFactoryImpl;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;

/**
 * This class implements the functionality of the central platform controller.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class PlatformController extends AbstractCommandReceivingComponent
        implements ContainerTerminationCallback, ExperimentAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformController.class);

    /**
     * The current version of the platform.
     */
    public static final String PLATFORM_VERSION = readVersion();

    private static final String DEPLOY_ENV = System.getProperty("DEPLOY_ENV", "production");
    private static final String DEPLOY_ENV_TESTING = "testing";
    private static final String DEPLOY_ENV_DEVELOP = "develop";
    private static final String CONTAINER_PARENT_CHECK_ENV_KEY = "CONTAINER_PARENT_CHECK";
    private static final boolean CONTAINER_PARENT_CHECK = System.getenv().containsKey(CONTAINER_PARENT_CHECK_ENV_KEY)
            ? System.getenv().get(CONTAINER_PARENT_CHECK_ENV_KEY) == "1"
            : true;
    private static final String RABBIT_MQ_EXPERIMENTS_HOST_NAME_KEY = "HOBBIT_RABBIT_EXPERIMENTS_HOST";

    // every 60 mins
    public static final long PUBLISH_CHALLENGES = 60 * 60 * 1000;

    /**
     * RabbitMQ channel between front end and platform controller.
     */
    protected Channel frontEnd2Controller;
    /**
     * The handler for requests coming from the front end.
     */
    protected FrontEndApiHandler frontEndApiHandler;
    /**
     * RabbitMQ channel between front end and platform controller.
     */
    protected Channel controller2Analysis;
    /**
     * A manager for Docker containers.
     */
    protected ContainerManager containerManager;
    /**
     * The observer of docker containers.
     */
    protected ContainerStateObserver containerObserver;
    /**
     * The queue containing experiments that are waiting for their execution.
     */
    protected ExperimentQueue queue;
    /**
     * Health checker used to make sure that the cluster has the preconfigured
     * hardware.
     */
    protected ClusterHealthChecker healthChecker = new ClusterHealthCheckerImpl();
    /**
     * A simple mutex that is used to wait for a termination signal for the
     * controller.
     */
    private Semaphore terminationMutex = new Semaphore(0);
    /**
     * Threadsafe JSON parser.
     */
    private Gson gson = new Gson();
    /**
     * Manager of benchmark and system images.
     */
    protected ImageManager imageManager;
    /**
     * Last experiment id that has been used.
     */
    private long lastIdTime = 0;

    protected StorageServiceClient storage;

    protected ExperimentManager expManager;

    protected ResourceInformationCollector resInfoCollector;

    protected ClusterManager clusterManager;

    /**
     * Timer used to trigger publishing of challenges
     */
    protected Timer challengePublishTimer;

    protected String rabbitMQExperimentsHostName;

    /**
     * Default constructor.
     */
    public PlatformController() {
    }

    /**
     * Constructor needed for testing.
     */
    public PlatformController(ExperimentManager expManager) {
        this.expManager = expManager;
        expManager.setController(this);
    }

    @Override
    public void init() throws Exception {
        // First initialize the super class
        super.init();
        LOGGER.debug("Platform controller initialization started.");
        if (System.getenv().containsKey(RABBIT_MQ_EXPERIMENTS_HOST_NAME_KEY)) {
            rabbitMQExperimentsHostName = System.getenv().get(RABBIT_MQ_EXPERIMENTS_HOST_NAME_KEY);
            if (!rabbitMQHostName.equals(rabbitMQExperimentsHostName)) {
                switchCmdToExpRabbit();
                LOGGER.info("Using {} as message broker for experiments.", rabbitMQExperimentsHostName);
            } else {
                LOGGER.warn(
                        "The message broker {} will be used for both - the platform internal communication as well as the experiment communication. It is suggested to have two separated message brokers.",
                        rabbitMQHostName);
            }
        } else {
            // the platform and its experiment are sharing a single RabbitMQ instance
            rabbitMQExperimentsHostName = rabbitMQHostName;
            LOGGER.warn(
                    "The message broker {} will be used for both - the platform internal communication as well as the experiment communication. It is suggested to have two separated message brokers.",
                    rabbitMQHostName);
        }

        // Set task history limit for swarm cluster to 0 (will remove all terminated
        // containers)
        // Only for prod mode
        clusterManager = new ClusterManagerImpl();
        if (DEPLOY_ENV.equals(DEPLOY_ENV_TESTING) || DEPLOY_ENV.equals(DEPLOY_ENV_DEVELOP)) {
            LOGGER.debug("Ignoring task history limit parameter. Will remain default (run 'docker info' for details).");
        } else {
            LOGGER.debug(
                    "Production mode. Setting task history limit to 0. All terminated containers will be removed.");
            clusterManager.setTaskHistoryLimit(0);
        }

        // create container manager
        containerManager = new ContainerManagerImpl();
        LOGGER.debug("Container manager initialized.");
        // Create container observer (polls status every 5s)
        containerObserver = new ContainerStateObserverImpl(containerManager, 5 * 1000);
        containerObserver.addTerminationCallback(this);
        // Tell the manager to add container to the observer
        containerManager.addContainerObserver(containerObserver);
        resInfoCollector = new ResourceInformationCollector(containerManager);

        containerObserver.startObserving();
        LOGGER.debug("Container observer initialized.");

        imageManager = new GitlabBasedImageManager();
        LOGGER.debug("Image manager initialized.");

        frontEnd2Controller = incomingDataQueueFactory.getConnection().createChannel();
        frontEndApiHandler = (new FrontEndApiHandler.Builder()).platformController(this)
                .queue(incomingDataQueueFactory, Constants.FRONT_END_2_CONTROLLER_QUEUE_NAME).build();

        controller2Analysis = cmdQueueFactory.getConnection().createChannel();
        controller2Analysis.queueDeclare(Constants.CONTROLLER_2_ANALYSIS_QUEUE_NAME, false, false, true, null);

        queue = new ExperimentQueueImpl();

        storage = StorageServiceClient.create(outgoingDataQueuefactory.getConnection());

        // the experiment manager should be the last module to create since it
        // directly starts to use the other modules
        if (expManager == null) {
            expManager = new ExperimentManager(this);
        }

        // schedule challenges re-publishing
        // TODO RC rename this timer
        challengePublishTimer = new Timer();
        PlatformController controller = this;
        challengePublishTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkRepeatableChallenges();
                republishChallenges(storage, queue, controller);
            }
        }, PUBLISH_CHALLENGES, PUBLISH_CHALLENGES);

        LOGGER.info("Platform controller initialized.");
    }

    /**
     * This method closes the command queue and its handling and reopens it on the
     * RabbitMQ broker which will be used for experiments.
     * 
     * @throws Exception
     */
    private void switchCmdToExpRabbit() throws Exception {
        // We have to close the existing command queue
        try {
            cmdChannel.close();
        } catch (Exception e) {
            LOGGER.warn("Exception while closing command queue. It will be ignored.", e);
        }
        IOUtils.closeQuietly(cmdQueueFactory);
        // temporarily create a new factory to the second broker but keep the reference
        // to the first broker (XXX this is a dirty workaround to make use of methods
        // like createConnection())
        ConnectionFactory tempFactory = connectionFactory;
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMQExperimentsHostName);
        connectionFactory.setAutomaticRecoveryEnabled(true);
        // attempt recovery every 10 seconds
        connectionFactory.setNetworkRecoveryInterval(10000);
        try {
            // override the existing command queue factory and the queue itself
            cmdQueueFactory = new RabbitQueueFactoryImpl(createConnection());
            cmdChannel = cmdQueueFactory.getConnection().createChannel();
            String queueName = cmdChannel.queueDeclare().getQueue();
            cmdChannel.exchangeDeclare(Constants.HOBBIT_COMMAND_EXCHANGE_NAME, "fanout", false, true, null);
            cmdChannel.queueBind(queueName, Constants.HOBBIT_COMMAND_EXCHANGE_NAME, "");

            Consumer consumer = new DefaultConsumer(cmdChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                        byte[] body) throws IOException {
                    try {
                        handleCmd(body, properties.getReplyTo());
                    } catch (Exception e) {
                        LOGGER.error("Exception while trying to handle incoming command.", e);
                    }
                }
            };
            cmdChannel.basicConsume(queueName, true, consumer);
        } finally {
            // set the factory back
            connectionFactory = tempFactory;
        }
    }

    /**
     * Handles incoming command request from the hobbit command queue.
     *
     * <p>
     * Commands handled by this method:
     * <ul>
     * <li>{@link Commands#DOCKER_CONTAINER_START}</li>
     * <li>{@link Commands#DOCKER_CONTAINER_STOP}</li>
     * </ul>
     *
     * @param command
     *            command to be executed
     * @param data
     *            byte-encoded supplementary json for the command
     *
     *            0 - start container 1 - stop container Data format for each
     *            command: Start container:
     */
    public void receiveCommand(byte command, byte[] data, String sessionId, String replyTo) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.info("received command: session={}, command={}, data={}", sessionId, Commands.toString(command),
                    data != null ? RabbitMQUtils.readString(data) : "null");
        } else {
            LOGGER.info("received command: session={}, command={}", sessionId, Commands.toString(command));
        }
        // This command will receive data from Rabbit
        // determine the command
        switch (command) {
        case Commands.DOCKER_CONTAINER_START: {
            StartCommandData startParams = null;
            String containerName = "";
            if (expManager.isExpRunning(sessionId)) {
                // Convert data byte array to config data structure
                startParams = deserializeStartCommandData(data);
                // trigger creation
                containerName = createContainer(startParams);
            } else {
                LOGGER.error(
                        "Got a request to start a container for experiment \"{}\" which is either not running or was already stopped. Returning null.", sessionId);
            }
            if (replyTo != null) {
                try {
                    cmdChannel.basicPublish("", replyTo, MessageProperties.PERSISTENT_BASIC,
                            RabbitMQUtils.writeString(containerName));
                } catch (IOException e) {
                    StringBuilder errMsgBuilder = new StringBuilder();
                    errMsgBuilder.append("Error, couldn't sent response after creation of container (");
                    if (startParams != null) {
                        errMsgBuilder.append(startParams.toString());
                    }
                    errMsgBuilder.append(") to replyTo=");
                    errMsgBuilder.append(replyTo);
                    errMsgBuilder.append(".");
                    LOGGER.error(errMsgBuilder.toString(), e);
                }
            }
            break;
        }
        case Commands.DOCKER_CONTAINER_STOP: {
            // get containerId from params
            StopCommandData stopParams = deserializeStopCommandData(data);
            // trigger stop
            stopContainer(stopParams.containerName);
            break;
        }
        case Commands.BENCHMARK_READY_SIGNAL: {
            expManager.systemOrBenchmarkReady(false, sessionId);
            break;
        }
        case Commands.SYSTEM_READY_SIGNAL: {
            expManager.systemOrBenchmarkReady(true, sessionId);
            break;
        }
        case Commands.TASK_GENERATION_FINISHED: {
            expManager.taskGenFinished(sessionId);
            break;
        }
        case Commands.BENCHMARK_FINISHED_SIGNAL: {
            if ((data == null) || (data.length == 0)) {
                LOGGER.error("Got no result model from the benchmark controller.");
            } else {
                expManager.setResultModel(sessionId, data, RabbitMQUtils::readModel);
            }
            break;
        }
        case Commands.REQUEST_SYSTEM_RESOURCES_USAGE: {
            // FIXME use the session id to make sure that only containers of this session
            // are observed
            ResourceUsageInformation resUsage = resInfoCollector.getSystemUsageInformation();
            LOGGER.info("Returning usage information: {}", resUsage != null ? resUsage.toString() : "null");
            if (replyTo != null) {
                byte[] response;
                if (resUsage != null) {
                    response = RabbitMQUtils.writeString(gson.toJson(resUsage));
                } else {
                    response = new byte[0];
                }
                try {
                    cmdChannel.basicPublish("", replyTo, MessageProperties.PERSISTENT_BASIC, response);
                } catch (IOException e) {
                    StringBuilder errMsgBuilder = new StringBuilder();
                    errMsgBuilder.append("Error, couldn't sent the request resource usage statistics to replyTo=");
                    errMsgBuilder.append(replyTo);
                    errMsgBuilder.append(".");
                    LOGGER.error(errMsgBuilder.toString(), e);
                }
            }
        }
        }
    }

    private StopCommandData deserializeStopCommandData(byte[] data) {
        if (data == null) {
            return null;
        }
        String dataString = RabbitMQUtils.readString(data);
        return gson.fromJson(dataString, StopCommandData.class);
    }

    private StartCommandData deserializeStartCommandData(byte[] data) {
        if (data == null) {
            return null;
        }
        String dataString = RabbitMQUtils.readString(data);
        return gson.fromJson(dataString, StartCommandData.class);
    }

    /**
     * Creates and starts a container based on the given {@link StartCommandData}
     * instance.
     *
     * @param data
     *            the data needed to start the container
     * @return the name of the created container
     */
    private String createContainer(StartCommandData data) {
        String parentId = containerManager.getContainerId(data.parent);
        if ((parentId == null) && (CONTAINER_PARENT_CHECK)) {
            LOGGER.error("Couldn't create container because the parent \"{}\" is not known.", data.parent);
            return null;
        }
        String containerId = containerManager.startContainer(data.image, data.type, parentId, data.environmentVariables,
                null);
        if (containerId == null) {
            return null;
        } else {
            return containerManager.getContainerName(containerId);
        }
    }

    /**
     * Stops the container with the given container name.
     *
     * @param containerName
     *            name of the container that should be stopped
     */
    public void stopContainer(String containerName) {
        String containerId = containerManager.getContainerId(containerName);
        if (containerId != null) {
            containerManager.removeContainer(containerId);
        }
    }

    @Override
    public void run() throws Exception {
        // We sleep until the controller should terminate
        terminationMutex.acquire();
    }

    @Override
    public void notifyTermination(String containerId, int exitCode) {
        LOGGER.info("Container " + containerId + " stopped with exitCode=" + exitCode);
        // Check whether this container was part of an experiment
        expManager.notifyTermination(containerId, exitCode);
        // Remove the container from the observer
        containerObserver.removedObservedContainer(containerId);
        // If we should remove all containers created by us
        if (!DEPLOY_ENV.equals(DEPLOY_ENV_TESTING)) {
            // If we remove this container, we have to make sure that there are
            // no children that are still running
            containerManager.removeParentAndChildren(containerId);
        }
    }

    @Override
    public void close() throws IOException {
        // stop the container observer
        try {
            if (containerObserver != null) {
                containerObserver.stopObserving();
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't stop the container observer.", e);
        }
        // get all remaining containers from the observer, terminate and remove them. Do
        // not try to get the list from the container manager since he will return all
        // containers regardless whether the platform created them or not.
        if (containerObserver != null) {
            List<String> containers = containerObserver.getObservedContainers();
            for (String containerId : containers) {
                try {
                    containerManager.removeContainer(containerId);
                } catch (Exception e) {
                    LOGGER.error("Couldn't stop running containers.", e);
                }
            }
        }
        // Close the storage client
        IOUtils.closeQuietly(storage);
        // Close the queue if this is needed
        if ((queue != null) && (queue instanceof Closeable)) {
            IOUtils.closeQuietly((Closeable) queue);
        }
        // Close communication channels
        if (frontEndApiHandler != null) {
            try {
                frontEndApiHandler.closeWhenFinished();
            } catch (Exception e) {
            }
        }
        if (frontEnd2Controller != null) {
            try {
                frontEnd2Controller.close();
            } catch (Exception e) {
            }
        }
        if (controller2Analysis != null) {
            try {
                controller2Analysis.close();
            } catch (Exception e) {
            }
        }

        // Close experiment manager
        IOUtils.closeQuietly(expManager);
        // Closing the super class is the last statement!
        super.close();
    }

    @Override
    public void analyzeExperiment(String uri) throws IOException {
        controller2Analysis.basicPublish("", Constants.CONTROLLER_2_ANALYSIS_QUEUE_NAME,
                MessageProperties.PERSISTENT_BASIC, RabbitMQUtils.writeString(uri));
    }

    /**
     * Sends the given command to the command queue with the given data appended and
     * using the given properties.
     *
     * @param address
     *            address for the message
     * @param command
     *            the command that should be sent
     * @param data
     *            data that should be appended to the command
     * @param props
     *            properties that should be used for the message
     * @throws IOException
     */
    protected void sendToCmdQueue(String address, byte command, byte data[], BasicProperties props) throws IOException {
        byte sessionIdBytes[] = RabbitMQUtils.writeString(address);
        // + 5 because 4 bytes for the session ID length and 1 byte for the
        // command
        int dataLength = sessionIdBytes.length + 5;
        boolean attachData = (data != null) && (data.length > 0);
        if (attachData) {
            dataLength += data.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(dataLength);
        buffer.putInt(sessionIdBytes.length);
        buffer.put(sessionIdBytes);
        buffer.put(command);
        if (attachData) {
            buffer.put(data);
        }
        cmdChannel.basicPublish(Constants.HOBBIT_COMMAND_EXCHANGE_NAME, "", props, buffer.array());
    }

    /**
     * The controller overrides the super method because it does not need to check
     * for the leading hobbit id and delegates the command handling to the
     * {@link #receiveCommand(byte, byte[], String, String)} method.
     */
    protected void handleCmd(byte bytes[], String replyTo) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int idLength = buffer.getInt();
        byte sessionIdBytes[] = new byte[idLength];
        buffer.get(sessionIdBytes);
        String sessionId = new String(sessionIdBytes, Charsets.UTF_8);
        byte command = buffer.get();
        byte remainingData[];
        if (buffer.remaining() > 0) {
            remainingData = new byte[buffer.remaining()];
            buffer.get(remainingData);
        } else {
            remainingData = new byte[0];
        }
        receiveCommand(command, remainingData, sessionId, replyTo);
    }

    public void handleFrontEndCmd(byte bytes[], String replyTo, BasicProperties replyProperties) {
        if (bytes.length == 0) {
            return;
        }
        byte response[] = null;
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        try {
            // The first byte is the command
            switch (buffer.get()) {
            case FrontEndApiCommands.LIST_CURRENT_STATUS: {
                String userName = RabbitMQUtils.readString(buffer);
                ControllerStatus status = getStatus(userName);
                response = RabbitMQUtils.writeString(gson.toJson(status));
                break;
            }
            case FrontEndApiCommands.LIST_AVAILABLE_BENCHMARKS: {
                response = RabbitMQUtils.writeString(gson.toJson(imageManager.getBenchmarks()));
                break;
            }
            case FrontEndApiCommands.GET_BENCHMARK_DETAILS: {
                // get benchmarkUri
                String benchmarkUri = RabbitMQUtils.readString(buffer);
                LOGGER.debug("Loading details for benchmark \"{}\"", benchmarkUri);
                // Get the benchmark
                BenchmarkMetaData benchmark = imageManager.getBenchmark(benchmarkUri);
                List<SystemMetaData> systems4Benchmark = imageManager.getSystemsForBenchmark(benchmarkUri);
                // If there is a username based on that the systems should
                // be filtered
                if (buffer.hasRemaining()) {
                    String userName = RabbitMQUtils.readString(buffer);
                    LOGGER.debug("Fitlering systems for user \"{}\"", userName);
                    Set<SystemMetaData> userSystems = new HashSet<SystemMetaData>(
                            imageManager.getSystemsOfUser(userName));
                    List<SystemMetaData> filteredSystems = new ArrayList<>(systems4Benchmark.size());
                    for (SystemMetaData s : systems4Benchmark) {
                        if (userSystems.contains(s)) {
                            filteredSystems.add(s);
                        }
                    }
                    systems4Benchmark = filteredSystems;
                }
                response = RabbitMQUtils.writeByteArrays(new byte[][] { RabbitMQUtils.writeModel(benchmark.rdfModel),
                        RabbitMQUtils.writeString(gson.toJson(systems4Benchmark)) });
                break;
            }
            case FrontEndApiCommands.ADD_EXPERIMENT_CONFIGURATION: {
                // get the benchmark URI
                String benchmarkUri = RabbitMQUtils.readString(buffer);
                String systemUri = RabbitMQUtils.readString(buffer);
                String serializedBenchParams = RabbitMQUtils.readString(buffer);
                String userName = RabbitMQUtils.readString(buffer);
                String experimentId = addExperimentToQueue(benchmarkUri, systemUri, userName, serializedBenchParams,
                        null, null, null);
                response = RabbitMQUtils.writeString(experimentId);
                break;
            }
            case FrontEndApiCommands.GET_SYSTEMS_OF_USER: {
                // get the user name
                String email = RabbitMQUtils.readString(buffer);
                LOGGER.info("Loading systems of user \"{}\"", email);
                response = RabbitMQUtils.writeString(gson.toJson(imageManager.getSystemsOfUser(email)));
                break;
            }
            case FrontEndApiCommands.CLOSE_CHALLENGE: {
                // get the challenge URI
                String challengeUri = RabbitMQUtils.readString(buffer);
                closeChallenge(challengeUri);
                break;
            }
            case FrontEndApiCommands.REMOVE_EXPERIMENT: {
                // get the experiment ID
                String experimentId = RabbitMQUtils.readString(buffer);
                // get the user name
                String userName = RabbitMQUtils.readString(buffer);
                // Get the experiment from the queue
                ExperimentConfiguration config = queue.getExperiment(experimentId);
                if (config == null) {
                    // The experiment is not known
                    response = new byte[] { 1 };
                }
                // Check whether the use has the right to terminate the experiment
                if ((config != null) && (config.userName != null) && (config.userName.equals(userName))) {
                    // Remove the experiment from the queue
                    if (queue.remove(config)) {
                        // call the Experiment Manager to cancel the experiment if it is running
                        expManager.stopExperimentIfRunning(experimentId);
                        // The experiment has been terminated
                        response = new byte[] { 1 };
                    } else {
                        // The experiment is not known
                        response = new byte[] { 0 };
                    }
                } else {
                    // The user does not have the right to remove the experiment
                    response = new byte[] { 0 };
                }
                break;
            }
            default: {
                LOGGER.error("Got a request from the front end with an unknown command code {}. It will be ignored.",
                        bytes[0]);
                break;
            }
            }
        } catch (Exception e) {
            LOGGER.error("Exception while hadling front end request.", e);
        } finally {
            if (replyTo != null) {
                LOGGER.trace("Replying to " + replyTo);
                try {
                    frontEnd2Controller.basicPublish("", replyTo, replyProperties,
                            response != null ? response : new byte[0]);
                } catch (IOException e) {
                    LOGGER.error("Exception while trying to send response to the front end.", e);
                }
            }
        }
        LOGGER.debug("Finished handling of front end request.");
    }

    /**
     * Retrieves model for the given challenge from the given graph (or without
     * selecting a certain graph if the graphUri is {@code null}).
     * 
     * @param challengeUri
     *            the URI for which the model should be retrieved
     * @param graphUri
     *            the URI from which the data should be retrieved or {@code null} if
     *            all graphs should be taken into account.
     * @return the RDF model of the challenge
     */
    protected Model getChallengeFromUri(String challengeUri, String graphUri) {
        String query = SparqlQueries.getChallengeGraphQuery(challengeUri, graphUri);
        if (query == null) {
            LOGGER.error("Couldn't get challenge {} because the needed SPARQL query couldn't be loaded. Aborting.",
                    challengeUri);
            return null;
        }
        return storage.sendConstructQuery(query);
    }

    private List<ExperimentConfiguration> getChallengeTasksFromUri(String challengeUri) {
        Model model = getChallengeFromUri(challengeUri, Constants.CHALLENGE_DEFINITION_GRAPH_URI);
        if (model == null) {
            LOGGER.error("Couldn't get model for challenge {} . Aborting.", challengeUri);
            return null;
        }
        Resource challengeResource = model.getResource(challengeUri);
        Calendar executionDate = RdfHelper.getDateValue(model, challengeResource, HOBBIT.executionDate);
        String organizer = RdfHelper.getStringValue(model, challengeResource, HOBBIT.organizer);
        ResIterator taskIterator = model.listSubjectsWithProperty(HOBBIT.isTaskOf, challengeResource);
        List<ExperimentConfiguration> experiments = new ArrayList<>();
        while (taskIterator.hasNext()) {
            Resource challengeTask = taskIterator.next();
            String challengeTaskUri = challengeTask.getURI();
            // get benchmark information
            String benchmarkUri = RdfHelper.getStringValue(model, challengeTask, HOBBIT.involvesBenchmark);
            String experimentId, systemUri, serializedBenchParams;
            // iterate participating system instances
            NodeIterator systemInstanceIterator = model.listObjectsOfProperty(challengeTask,
                    HOBBIT.involvesSystemInstance);
            RDFNode sysInstance;
            while (systemInstanceIterator.hasNext()) {
                sysInstance = systemInstanceIterator.next();
                if (sysInstance.isURIResource()) {
                    systemUri = sysInstance.asResource().getURI();
                    experimentId = generateExperimentId();
                    serializedBenchParams = RabbitMQUtils
                            .writeModel2String(createExpModelForChallengeTask(model, challengeTaskUri, systemUri));
                    experiments.add(new ExperimentConfiguration(experimentId, benchmarkUri, serializedBenchParams,
                            systemUri, organizer, challengeUri, challengeTaskUri, executionDate));
                } else {
                    LOGGER.error("Couldn't get the benchmark for challenge task \"{}\". This task will be ignored.",
                            challengeTaskUri);
                }
            }
        }
        return experiments;
    }

    /**
     * Inserts the configured experiments of a challenge into the queue.
     *
     * @param challengeUri
     *            the URI of the challenge
     */
    private void executeChallengeExperiments(String challengeUri) {
        // get experiments from the challenge
        List<ExperimentConfiguration> experiments = getChallengeTasksFromUri(challengeUri);
        if (experiments == null) {
            LOGGER.error("Couldn't get experiments for challenge {} . Aborting.", challengeUri);
            return;
        }
        // add to queue
        for (ExperimentConfiguration ex : experiments) {
            LOGGER.info("Adding experiment " + ex.id + " with benchmark " + ex.benchmarkUri + " and system "
                    + ex.systemUri + " to the queue.");
            queue.add(ex);
        }
    }

    /**
     * Schedules the date of next execution for a repeatable challenge, or closes
     * it.
     *
     * @param storage
     *            storage
     * @param challengeUri
     *            challenge URI
     * @param now
     *            time to use as current when scheduling
     */
    protected static synchronized void scheduleDateOfNextExecution(StorageServiceClient storage, String challengeUri,
            Calendar now) {
        LOGGER.info("Scheduling dateOfNextExecution for challenge {}...", challengeUri);
        String query = SparqlQueries.getRepeatableChallengeInfoQuery(challengeUri,
                Constants.CHALLENGE_DEFINITION_GRAPH_URI);
        Model challengeModel = storage.sendConstructQuery(query);
        ResIterator challengeIterator = challengeModel.listResourcesWithProperty(RDF.type, HOBBIT.Challenge);
        if (!challengeIterator.hasNext()) {
            LOGGER.error("Couldn't retrieve challenge " + challengeUri + ". Aborting.");
            return;
        }

        Resource challenge = challengeIterator.next();
        Calendar registrationCutoffDate = RdfHelper.getDateTimeValue(challengeModel, challenge,
                HOBBIT.registrationCutoffDate);
        if (registrationCutoffDate == null) {
            LOGGER.error("Couldn't retrieve registration cutoff date for challenge " + challengeUri + ". Aborting.");
            return;
        }

        Duration executionPeriod = RdfHelper.getDurationValue(challengeModel, challenge, HOBBIT.executionPeriod);
        if (executionPeriod == null) {
            LOGGER.error("Couldn't retrieve execution period for challenge " + challengeUri + ". Aborting.");
            return;
        }

        Calendar dateOfNextExecution = RdfHelper.getDateTimeValue(challengeModel, challenge,
                HOBBIT.dateOfNextExecution);
        if (dateOfNextExecution == null) {
            dateOfNextExecution = RdfHelper.getDateTimeValue(challengeModel, challenge, HOBBIT.executionDate);
            if (dateOfNextExecution == null) {
                dateOfNextExecution = now;
            }
        }

        int skip = -1;
        do {
            dateOfNextExecution.add(Calendar.MILLISECOND, (int) executionPeriod.toMillis());
            skip++;
        } while (dateOfNextExecution.before(now));
        if (skip > 0) {
            LOGGER.info("Skipping {} executions of repeatable challenge {} due to running late", skip, challenge);
        }

        if (dateOfNextExecution.before(registrationCutoffDate)) {
            // set dateOfNextExecution += executionPeriod
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            LOGGER.info("Next execution date for challenge {} is now set to {}", challengeUri,
                    dateFormat.format(dateOfNextExecution.getTime()));
            if (!storage.sendUpdateQuery(SparqlQueries.getUpdateDateOfNextExecutionQuery(challengeUri,
                    dateOfNextExecution, Constants.CHALLENGE_DEFINITION_GRAPH_URI))) {
                LOGGER.error("Couldn't update dateOfNextExecution for challenge {}", challengeUri);
            }
        } else {
            // delete dateOfNextExecution, since registration cutoff date will be reached
            // already
            LOGGER.info("Removing dateOfNextExecution for challenge {} because it reached cutoff date", challengeUri);
            if (!storage.sendUpdateQuery(SparqlQueries.getUpdateDateOfNextExecutionQuery(challengeUri, null,
                    Constants.CHALLENGE_DEFINITION_GRAPH_URI))) {
                LOGGER.error("Couldn't remove dateOfNextExecution for challenge {}", challengeUri);
            }
        }
    }

    /**
     * Copies the challenge from challenge definition graph to public graph.
     *
     * @param storage
     *            storage
     * @param challengeUri
     *            challenge URI
     */
    protected static synchronized boolean copyChallengeToPublicResultGraph(StorageServiceClient storage,
            String challengeUri) {
        // get the challenge model
        Model challengeModel = storage.sendConstructQuery(
                SparqlQueries.getChallengeGraphQuery(challengeUri, Constants.CHALLENGE_DEFINITION_GRAPH_URI));
        // insert the challenge into the public graph
        return storage.sendInsertQuery(challengeModel, Constants.PUBLIC_RESULT_GRAPH_URI);
    }

    /**
     * Closes the challenge with the given URI by adding the "closed" triple to its
     * graph and inserting the configured experiments into the queue.
     *
     * @param challengeUri
     *            the URI of the challenge that should be closed
     */
    private void closeChallenge(String challengeUri) {
        LOGGER.info("Closing challenge {}...", challengeUri);
        // send SPARQL query to close the challenge
        String query = SparqlQueries.getCloseChallengeQuery(challengeUri, Constants.CHALLENGE_DEFINITION_GRAPH_URI);
        if (query == null) {
            LOGGER.error(
                    "Couldn't close the challenge {} because the needed SPARQL query couldn't be loaded. Aborting.",
                    challengeUri);
            return;
        }
        if (!storage.sendUpdateQuery(query)) {
            LOGGER.error("Couldn't close the challenge {} because the SPARQL query didn't had any effect. Aborting.",
                    challengeUri);
            return;
        }
        executeChallengeExperiments(challengeUri);
    }

    protected synchronized void checkRepeatableChallenges() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        Calendar now = Calendar.getInstance(Constants.DEFAULT_TIME_ZONE);
        LOGGER.info("Processing repeatable challenges at {}...", dateFormat.format(now.getTime()));

        String query = SparqlQueries.getRepeatableChallengeInfoQuery(null, Constants.CHALLENGE_DEFINITION_GRAPH_URI);
        Model challengesModel = storage.sendConstructQuery(query);
        ResIterator challengeIterator = challengesModel.listResourcesWithProperty(RDF.type, HOBBIT.Challenge);
        Resource challenge;
        Calendar registrationCutoffDate;
        Calendar dateOfNextExecution;
        // go through the challenges
        while (challengeIterator.hasNext()) {
            challenge = challengeIterator.next();
            LOGGER.info("Processing repeatable challenge {}...", challenge);

            registrationCutoffDate = RdfHelper.getDateTimeValue(challengesModel, challenge,
                    HOBBIT.registrationCutoffDate);
            if ((registrationCutoffDate != null) && (now.after(registrationCutoffDate))) {
                // registration cutoff date has been reached, close the challenge (it will run
                // remaining experiments)
                closeChallenge(challenge.getURI());
                continue;
            }

            dateOfNextExecution = RdfHelper.getDateTimeValue(challengesModel, challenge, HOBBIT.dateOfNextExecution);
            if (dateOfNextExecution == null) {
                // executions didn't start yet
                Calendar executionDate = RdfHelper.getDateTimeValue(challengesModel, challenge, HOBBIT.executionDate);
                if ((executionDate != null) && (now.after(executionDate))) {
                    LOGGER.info("Starting repeatable challenge {} with execution date {}...", challenge,
                            dateFormat.format(executionDate.getTime()));
                    // executionDate has been reached, copy challenge to public graph and set
                    // dateOfNextExecution
                    if (!copyChallengeToPublicResultGraph(storage, challenge.getURI())) {
                        LOGGER.error("Couldn't copy the graph of the challenge \"{}\". Aborting.", challenge);
                        continue;
                    }
                } else {
                    LOGGER.info("Repeatable challenge {} will start at {}", challenge,
                            dateFormat.format(executionDate.getTime()));
                    continue;
                }
            }

            if (dateOfNextExecution == null || now.after(dateOfNextExecution)) {
                // date of execution has been reached
                LOGGER.info("Execution date has been reached for repeatable challenge {}", challenge);
                executeChallengeExperiments(challenge.getURI());

                // move the [challengeTask hobbit:involvesSystem system] triples from the
                // challenge def graph to the public result graph
                String moveQuery = SparqlQueries.getMoveChallengeSystemQuery(challenge.getURI(),
                        Constants.CHALLENGE_DEFINITION_GRAPH_URI, Constants.PUBLIC_RESULT_GRAPH_URI);
                if (!storage.sendUpdateQuery(moveQuery)) {
                    LOGGER.error("Couldn't move the [task :involvesSystem system] triple to the public graph",
                            challenge);
                }

                scheduleDateOfNextExecution(storage, challenge.getURI(), now);
            }
        }
    }

    /*
     * The method is static for an easier JUnit test implementation
     */
    protected static synchronized void republishChallenges(StorageServiceClient storage, ExperimentQueue queue,
            ExperimentAnalyzer analyzer) {
        LOGGER.info("Checking for challenges to publish...");
        // Get list of all UNPUBLISHED, closed challenges, their tasks and
        // publication dates
        Model challengesModel = storage.sendConstructQuery(
                SparqlQueries.getChallengePublishInfoQuery(null, Constants.CHALLENGE_DEFINITION_GRAPH_URI));
        ResIterator challengeIterator = challengesModel.listResourcesWithProperty(RDF.type, HOBBIT.Challenge);
        Resource challenge;
        Calendar now = Calendar.getInstance(Constants.DEFAULT_TIME_ZONE);
        // go through the challenges
        while (challengeIterator.hasNext()) {
            challenge = challengeIterator.next();
            Calendar publishDate = RdfHelper.getDateTimeValue(challengesModel, challenge, HOBBIT.publicationDate);
            if (publishDate == null) {
                publishDate = RdfHelper.getDateValue(challengesModel, challenge, HOBBIT.publicationDate);
            }
            // If the challenge results should be published
            if ((publishDate != null) && (now.after(publishDate))) {
                List<Resource> taskResources = RdfHelper.getSubjectResources(challengesModel, HOBBIT.isTaskOf,
                        challenge);
                Set<String> tasks = new HashSet<>();
                for (Resource taskResource : taskResources) {
                    tasks.add(taskResource.getURI());
                }
                /*
                 * Check that all experiments that belong to the challenge have been finished.
                 * Note that we don't have to check the experiment that is running at the
                 * moment, since it is part of the queue.
                 */
                int count = 0;
                for (ExperimentConfiguration config : queue.listAll()) {
                    if (tasks.contains(config.challengeTaskUri)) {
                        ++count;
                    }
                }
                // if there are no challenge experiments in the queue
                if (count == 0) {
                    LOGGER.info("publishing challenge {}", challenge.getURI());
                    // copy challenge to the public result graph
                    if (!copyChallengeToPublicResultGraph(storage, challenge.getURI())) {
                        LOGGER.error("Couldn't copy the graph of the challenge \"{}\". Aborting.", challenge.getURI());
                        return;
                    }
                    // copy the results to the public result graph
                    List<Resource> experiments = new ArrayList<>();
                    for (String taskUri : tasks) {
                        Model taskExperimentModel = storage.sendConstructQuery(SparqlQueries
                                .getExperimentOfTaskQuery(null, taskUri, Constants.PRIVATE_RESULT_GRAPH_URI));
                        experiments.addAll(RdfHelper.getSubjectResources(taskExperimentModel, HOBBIT.isPartOf,
                                taskExperimentModel.getResource(taskUri)));
                        if (!storage.sendInsertQuery(taskExperimentModel, Constants.PUBLIC_RESULT_GRAPH_URI)) {
                            LOGGER.error("Couldn't copy experiment results for challenge task \"{}\". Aborting.",
                                    taskUri);
                            return;
                        }
                        // Send the experiment to the analysis component
                        for (Resource experiment : experiments) {
                            try {
                                analyzer.analyzeExperiment(experiment.getURI());
                            } catch (IOException e) {
                                LOGGER.error("Could not send task \"{}\" to AnalyseQueue.", taskUri);
                            }
                        }
                    }
                    // Remove challenge from challenge graph
                    storage.sendUpdateQuery(SparqlQueries.deleteChallengeGraphQuery(challenge.getURI(),
                            Constants.CHALLENGE_DEFINITION_GRAPH_URI));
                    // Remove experiments
                    for (Resource experiment : experiments) {
                        storage.sendUpdateQuery(SparqlQueries.deleteExperimentGraphQuery(experiment.getURI(),
                                Constants.PRIVATE_RESULT_GRAPH_URI));
                    }
                    // Clean up the remaining graph
                    String queries[] = SparqlQueries
                            .cleanUpChallengeGraphQueries(Constants.CHALLENGE_DEFINITION_GRAPH_URI);
                    for (int i = 0; i < queries.length; ++i) {
                        storage.sendUpdateQuery(queries[i]);
                    }
                    queries = SparqlQueries.cleanUpPrivateGraphQueries(Constants.PRIVATE_RESULT_GRAPH_URI);
                    for (int i = 0; i < queries.length; ++i) {
                        storage.sendUpdateQuery(queries[i]);
                    }
                }
            }
        }
    }

    private Model createExpModelForChallengeTask(Model model, String challengeTaskUri, String systemUri) {
        Dataset dataset = DatasetFactory.create();
        dataset.addNamedModel("http://temp.org/challenge", model);
        String query = SparqlQueries.getCreateExperimentFromTaskQuery(Constants.NEW_EXPERIMENT_URI, challengeTaskUri,
                systemUri, "http://temp.org/challenge");
        if (query == null) {
            LOGGER.error("Couldn't load SPARQL query to create an RDF model for a new experiment. Returning null.");
            return null;
        }
        QueryExecution qe = QueryExecutionFactory.create(query, dataset);
        return qe.execConstruct();
    }

    /**
     * Adds a new experiment with the given benchmark, system and benchmark
     * parameter to the queue.
     *
     * @param benchmarkUri
     *            the URI of the benchmark
     * @param systemUri
     *            the URI of the system
     * @param userName
     *            the name of the user who requested the creation of the experiment
     * @param serializedBenchParams
     *            the serialized benchmark parameters
     * @param executionDate
     *            the date at which this experiment should be executed as part of a
     *            challenge. Should be set to <code>null</code> if it is not part of
     *            a challenge.
     * @return the Id of the created experiment
     */
    protected String addExperimentToQueue(String benchmarkUri, String systemUri, String userName,
            String serializedBenchParams, String challengUri, String challengTaskUri, Calendar executionDate) {
        String experimentId = generateExperimentId();
        LOGGER.info("Adding experiment {} with benchmark {}, system {} and user {} to the queue.", experimentId,
                benchmarkUri, systemUri, userName);
        queue.add(new ExperimentConfiguration(experimentId, benchmarkUri, serializedBenchParams, systemUri, userName,
                challengUri, challengTaskUri, executionDate));
        return experimentId;
    }

    /**
     * Creates a status object summarizing the current status of this controller.
     *
     * @return the status of this controller
     */
    private ControllerStatus getStatus(String userName) {
        ControllerStatus status = new ControllerStatus();
        expManager.addStatusInfo(status, userName);
        RunningExperiment runningExperiment = status.experiment;
        if ((runningExperiment != null) && (runningExperiment.systemUri != null)) {
            Model model = imageManager.getSystemModel(runningExperiment.systemUri);
            if (model != null) {
                runningExperiment.systemName = RdfHelper.getLabel(model,
                        model.getResource(runningExperiment.systemUri));
            } else {
                runningExperiment.systemName = runningExperiment.systemUri;
            }
            model = imageManager.getBenchmarkModel(runningExperiment.benchmarkUri);
            if (model != null) {
                runningExperiment.benchmarkName = RdfHelper.getLabel(model,
                        model.getResource(runningExperiment.benchmarkUri));
            } else {
                runningExperiment.benchmarkName = runningExperiment.benchmarkUri;
            }
        }
        List<ExperimentConfiguration> experiments = queue.listAll();
        List<QueuedExperiment> tempQueue = new ArrayList<QueuedExperiment>(experiments.size());
        QueuedExperiment queuedExp;
        for (ExperimentConfiguration experiment : experiments) {
            if ((runningExperiment == null) || (!experiment.id.equals(runningExperiment.experimentId))) {
                queuedExp = new QueuedExperiment();
                queuedExp.experimentId = experiment.id;
                queuedExp.benchmarkUri = experiment.benchmarkUri;
                queuedExp.benchmarkName = experiment.benchmarkUri; // FIXME should be something different :/
                queuedExp.systemUri = experiment.systemUri;
                queuedExp.systemName = experiment.systemUri; // FIXME should be something different :/
                queuedExp.challengeUri = experiment.challengeUri;
                queuedExp.challengeTaskUri = experiment.challengeTaskUri;
                queuedExp.dateOfExecution = experiment.executionDate != null
                        ? experiment.executionDate.getTimeInMillis()
                        : 0;
                queuedExp.canBeCanceled = userName != null && userName.equals(experiment.userName);
                tempQueue.add(queuedExp);
            }
        }
        status.queuedExperiments = tempQueue.toArray(new QueuedExperiment[tempQueue.size()]);
        return status;
    }

    /**
     * Generates a unique experiment Id based on the current time stamp and the last
     * Id ({@link #lastIdTime}) that has been created.
     *
     * @return a unique experiment Id
     */
    private synchronized String generateExperimentId() {
        long time = System.currentTimeMillis();
        while (time <= lastIdTime) {
            ++time;
        }
        lastIdTime = time;
        return Long.toString(time);
    }

    /**
     * Generates an experiment URI using the given id and the experiment URI
     * namespace defined by {@link Constants#EXPERIMENT_URI_NS}.
     *
     * @param id
     *            the id of the experiment
     * @return the experiment URI
     */
    public static String generateExperimentUri(String id) {
        return Constants.EXPERIMENT_URI_NS + id;
    }

    public ImageManager imageManager() {
        return imageManager;
    }

    public StorageServiceClient storage() {
        return storage;
    }

    public String rabbitMQHostName() {
        return rabbitMQExperimentsHostName;
    }

    ///// There are some methods that shouldn't be used by the controller and
    ///// have been marked as deprecated

    /**
     * @deprecated Not used inside the controller. Use
     *             {@link #receiveCommand(byte, byte[], String, String)} instead.
     */
    @Deprecated
    @Override
    public void receiveCommand(byte command, byte[] data) {
    }

    /**
     * @deprecated The {@link PlatformController} should use
     *             {@link #sendToCmdQueue(String, byte, byte[], BasicProperties)}
     */
    @Deprecated
    protected void sendToCmdQueue(byte command) throws IOException {
        sendToCmdQueue(Constants.HOBBIT_SESSION_ID_FOR_PLATFORM_COMPONENTS, command, null, null);
    }

    /**
     * @deprecated The {@link PlatformController} should use
     *             {@link #sendToCmdQueue(String, byte, byte[], BasicProperties)}
     */
    @Deprecated
    protected void sendToCmdQueue(byte command, byte data[]) throws IOException {
        sendToCmdQueue(Constants.HOBBIT_SESSION_ID_FOR_PLATFORM_COMPONENTS, command, data, null);
    }

    /**
     * @deprecated The {@link PlatformController} should use
     *             {@link #sendToCmdQueue(String, byte, byte[], BasicProperties)}
     */
    @Deprecated
    protected void sendToCmdQueue(byte command, byte data[], BasicProperties props) throws IOException {
        sendToCmdQueue(Constants.HOBBIT_SESSION_ID_FOR_PLATFORM_COMPONENTS, command, data, props);
    }

    private static String readVersion() {
        String version = "UNKNOWN";
        String versionKey = "org.hobbit.controller.PlatformController.version";
        InputStream is = null;
        try {
            is = PlatformController.class.getResourceAsStream("/hobbit.version");
            Properties versionProps = new Properties();
            versionProps.load(is);
            if (versionProps.containsKey(versionKey)) {
                version = versionProps.getProperty(versionKey);
            } else {
                LOGGER.error("The loaded version file does not contain the version property. Returning default value.");
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't get version file. Returning default value.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        LOGGER.info("Platform has version {}", version);
        return version;
    }
}
