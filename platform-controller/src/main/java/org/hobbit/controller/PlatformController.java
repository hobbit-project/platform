package org.hobbit.controller;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.hobbit.controller.data.ExperimentStatus;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.hobbit.controller.data.ExperimentStatus.States;
import org.hobbit.controller.docker.*;
import org.hobbit.controller.health.ClusterHealthChecker;
import org.hobbit.controller.health.ClusterHealthCheckerImpl;
import org.hobbit.controller.queue.ExperimentQueue;
import org.hobbit.controller.queue.ExperimentQueueImpl;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.FrontEndApiCommands;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.data.ConfiguredExperiment;
import org.hobbit.core.data.ControllerStatus;
import org.hobbit.core.data.StartCommandData;
import org.hobbit.core.data.StopCommandData;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.vocab.HobbitErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.model.Container;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;

/**
 * This class implements the functionality of the central platform controller.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class PlatformController extends AbstractCommandReceivingComponent implements ContainerTerminationCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformController.class);

    private static final String DEPLOY_ENV = System.getProperty("DEPLOY_ENV", "production");
    private static final String DEPLOY_ENV_TESTING = "testing";

    /**
     * Default time an experiment has to terminate after it has been started.
     */
    private static final long DEFAULT_MAX_EXECUTION_TIME = 30 * 60 * 1000;

    /**
     * RabbitMQ channel between front end and platform controller.
     */
    protected Channel frontEnd2Controller;
    /**
     * The handler for requests coming from the front end.
     */
    protected DefaultConsumer frontEndApiHandler;
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
    private ImageManager imageManager;
    /**
     * Last experiment id that has been used.
     */
    private long lastIdTime = 0;

    private StorageServiceClient storage;

    private ExperimentManager expManager;

    @Override
    public void init() throws Exception {
        // First initialize the super class
        super.init();
        LOGGER.debug("Platform controller initialization started.");

        // create container manager
        containerManager = new ContainerManagerImpl();
        LOGGER.debug("Container manager initialized.");
        // Create container observer (polls status every 5s)
        containerObserver = new ContainerStateObserverImpl(containerManager, 5 * 1000);
        containerObserver.addTerminationCallback(this);
        // Tell the manager to add container to the observer
        containerManager.addContainerObserver(containerObserver);

        containerObserver.startObserving();
        LOGGER.debug("Container observer initialized.");

        imageManager = new ImageManagerImpl();
        LOGGER.debug("Image manager initialized.");

        frontEnd2Controller = connection.createChannel();
        frontEndApiHandler = new DefaultConsumer(frontEnd2Controller) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                    throws IOException {
                if (body.length > 0) {
                    BasicProperties replyProperties;
                    replyProperties = new BasicProperties.Builder().correlationId(properties.getCorrelationId())
                            .deliveryMode(2).build();
                    handleFrontEndCmd(body, properties.getReplyTo(), replyProperties);
                }
            }
        };
        frontEnd2Controller.queueDeclare(Constants.FRONT_END_2_CONTROLLER_QUEUE_NAME, false, false, true, null);
        frontEnd2Controller.basicConsume(Constants.FRONT_END_2_CONTROLLER_QUEUE_NAME, true, frontEndApiHandler);

        queue = new ExperimentQueueImpl();

        storage = StorageServiceClient.create(connection);

        // the experiment manager should be the last module to create since it
        // directly starts to use the other modules
        expManager = new ExperimentManager(this);

        LOGGER.info("Platform controller initialized.");
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
            // Convert data byte array to config data structure
            StartCommandData startParams = deserializeStartCommandData(data);
            // trigger creation
            String containerName = createContainer(startParams);
            if (replyTo != null) {
                try {
                    cmdChannel.basicPublish("", replyTo, MessageProperties.PERSISTENT_BASIC,
                            RabbitMQUtils.writeString(containerName));
                } catch (IOException e) {
                    StringBuilder errMsgBuilder = new StringBuilder();
                    errMsgBuilder.append("Error, couldn't sent response after creation of container (");
                    errMsgBuilder.append(startParams.toString());
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
            expManager.systemOrBenchmarkReady(false);
            break;
        }
        case Commands.SYSTEM_READY_SIGNAL: {
            expManager.systemOrBenchmarkReady(true);
            break;
        }
        case Commands.TASK_GENERATION_FINISHED: {
            expManager.taskGenFinished();
            break;
        }
        case Commands.BENCHMARK_FINISHED_SIGNAL: {
            if ((data == null) || (data.length == 0)) {
                LOGGER.error("Got no result model from the benchmark controller.");
            } else {
                Model model = RabbitMQUtils.readModel(data);
                expManager.setResultModel(model);
            }
            break;
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
     * Creates and starts a container based on the given
     * {@link StartCommandData} instance.
     * 
     * @param data
     *            the data needed to start the container
     * @return the name of the created container
     */
    private String createContainer(StartCommandData data) {
        String parentId = containerManager.getContainerId(data.parent);
        if (parentId == null) {
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
            containerManager.stopContainer(containerId);
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
            containerManager.stopParentAndChildren(containerId);
            containerManager.removeContainer(containerId);
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
        // get all remaining containers from the container manager,
        // terminate and remove them
        try {
            List<Container> containers = containerManager.getContainers();
            for (Container c : containers) {
                containerManager.stopContainer(c.getId());
                containerManager.removeContainer(c.getId());
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't stop running containers.", e);
        }
        // Close the storage client
        IOUtils.closeQuietly(storage);
        // Close the queue if this is needed
        if ((queue != null) && (queue instanceof Closeable)) {
            IOUtils.closeQuietly((Closeable) queue);
        }
        if (frontEnd2Controller != null) {
            try {
                frontEnd2Controller.close();
            } catch (Exception e) {
            }
        }
        // Close experiment manager
        IOUtils.closeQuietly(expManager);
        // Closing the super class is the last statement!
        super.close();
    }

    /**
     * Sends the given command to the command queue with the given data appended
     * and using the given properties.
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
     * The controller overrides the super method because it does not need to
     * check for the leading hobbit id and delegates the command handling to the
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

    protected void handleFrontEndCmd(byte bytes[], String replyTo, BasicProperties replyProperties) {
        byte response[] = null;
        try {
            switch (bytes[0]) {
            case FrontEndApiCommands.LIST_CURRENT_STATUS: {
                ControllerStatus status = getStatus();
                response = RabbitMQUtils.writeString(gson.toJson(status));
                break;
            }
            case FrontEndApiCommands.LIST_AVAILABLE_BENCHMARKS: {
                response = RabbitMQUtils.writeString(gson.toJson(imageManager.getBenchmarks()));
                break;
            }
            case FrontEndApiCommands.GET_BENCHMARK_DETAILS: {
                // get benchmarkUri
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                // skip the command Id
                buffer.get();
                String benchmarkUri = RabbitMQUtils.readString(buffer);
                LOGGER.info("Loading details for benchmark \"{}\"", benchmarkUri);
                Model benchmarkModel = imageManager.getBenchmarkModel(benchmarkUri);
                if (benchmarkModel != null) {
                    response = RabbitMQUtils
                            .writeByteArrays(new byte[][] { RabbitMQUtils.writeModel(benchmarkModel), RabbitMQUtils
                                    .writeString(gson.toJson(imageManager.getSystemsForBenchmark(benchmarkModel))) });
                }
                break;
            }
            case FrontEndApiCommands.ADD_EXPERIMENT_CONFIGURATION: {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                // We can skip the first byte
                buffer.get();
                // get the benchmark URI
                String benchmarkUri = RabbitMQUtils.readString(buffer);
                String systemUri = RabbitMQUtils.readString(buffer);
                String serializedBenchParams = RabbitMQUtils.readString(buffer);
                String experimentId = generateExperimentId();
                LOGGER.info("Adding experiment " + experimentId + " with benchmark " + benchmarkUri + " and system "
                        + systemUri + " to the queue.");
                queue.add(new ExperimentConfiguration(experimentId, benchmarkUri, serializedBenchParams, systemUri));
                response = RabbitMQUtils.writeString(experimentId);
                break;
            }
            case FrontEndApiCommands.GET_SYSTEMS_OF_USER: {
                String userName = RabbitMQUtils.readString(bytes);
                response = RabbitMQUtils.writeString(gson.toJson(imageManager.getSystemsForBenchmark(userName)));
                break;
            }
            case FrontEndApiCommands.CLOSE_CHALLENGE: {
                // TODO
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
                LOGGER.info("Replying to " + replyTo);
                try {
                    frontEnd2Controller.basicPublish("", replyTo, replyProperties,
                            response != null ? response : new byte[0]);
                } catch (IOException e) {
                    LOGGER.error("Exception while trying to send response to the front end.", e);
                }
            }
        }
        LOGGER.info("Finished handling of front end request.");
    }

    /**
     * Creates a status object summarizing the current status of this
     * controller.
     * 
     * @return the status of this controller
     */
    private ControllerStatus getStatus() {
        ControllerStatus status = new ControllerStatus();
        expManager.addStatusInfo(status);
        List<ExperimentConfiguration> experiments = queue.listAll();
        ExperimentConfiguration experiment;
        status.queue = new ConfiguredExperiment[experiments.size()];
        for (int i = 0; i < status.queue.length; ++i) {
            experiment = experiments.get(i);
            status.queue[i] = new ConfiguredExperiment();
            status.queue[i].benchmarkName = experiment.benchmarkName;
            status.queue[i].systemName = experiment.systemUri;
        }
        return status;
    }

    /**
     * Generates a unique experiment Id based on the current time stamp and the
     * last Id ({@link #lastIdTime}) that has been created.
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
    public static String generateBenchmarkUri(String id) {
        return Constants.EXPERIMENT_URI_NS + id;
    }

    /**
     * This class encapsulates (and synchronizes) all methods that are applied
     * on a running experiment.
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    protected static class ExperimentManager implements Closeable {
        /**
         * Time interval with which the experiment manager checks for a new
         * experiment to start.
         */
        public static final long CHECK_FOR_NEW_EXPERIMENT = 10000;

        /**
         * The controller this manager belongs to.
         */
        private PlatformController controller;

        /**
         * Mutex used to synchronize the access to the {@link #experimentStatus}
         * instance.
         */
        private Semaphore experimentMutex = new Semaphore(1);
        /**
         * Status of the current experiment. <code>null</code> if no benchmark
         * is running.
         */
        private ExperimentStatus experimentStatus = null;
        /**
         * Timer used to trigger the creation of the next benchmark.
         */
        private Timer expStartTimer;

        public ExperimentManager(PlatformController controller) {
            this.controller = controller;

            expStartTimer = new Timer();
            expStartTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // trigger the creation of the next benchmark
                        createNextExperiment();
                    } catch (Throwable e) {
                        LOGGER.error("The experiment starting timer got an unexpected exception.", e);
                    }
                }
            }, CHECK_FOR_NEW_EXPERIMENT, CHECK_FOR_NEW_EXPERIMENT);
        }

        /**
         * Creates the next experiment if there is no experiment running and
         * there is an experiment waiting in the queue.
         */
        public void createNextExperiment() {
            try {
                experimentMutex.acquire();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for the experiment mutex. Returning.", e);
                return;
            }
            try {
                // if there is no benchmark running (and the queue has been
                // initialized)
                if ((experimentStatus == null) && (controller.queue != null)) {
                    ExperimentConfiguration config = controller.queue.getNextExperiment();
                    LOGGER.debug("Trying to start the next benchmark.");
                    if (config == null) {
                        LOGGER.debug("There is no experiment to start.");
                    } else {
                        LOGGER.info("Creating next experiment " + config.id + " with benchmark " + config.benchmarkUri
                                + " and system " + config.systemUri + " to the queue.");
                        experimentStatus = new ExperimentStatus(config, generateBenchmarkUri(config.id), controller,
                                DEFAULT_MAX_EXECUTION_TIME);
                        String benchImageName = controller.imageManager.getBenchmarkImageName(config.benchmarkUri);
                        if (benchImageName == null) {
                            experimentStatus.addError(HobbitErrors.BenchmarkImageMissing);
                            throw new Exception("Couldn't find image name for benchmark " + config.benchmarkUri);
                        } else {
                            String sysImageName = controller.imageManager.getSystemImageName(config.systemUri);
                            if (sysImageName == null) {
                                experimentStatus.addError(HobbitErrors.SystemImageMissing);
                                throw new Exception("Couldn't find image name for system " + config.systemUri);
                            } else {
                                LOGGER.info("Creating benchmark controller " + benchImageName);
                                String containerId = controller.containerManager.startContainer(benchImageName,
                                        Constants.CONTAINER_TYPE_BENCHMARK, null,
                                        new String[] {
                                                Constants.RABBIT_MQ_HOST_NAME_KEY + "=" + controller.rabbitMQHostName,
                                                Constants.HOBBIT_SESSION_ID_KEY + "=" + config.id,
                                                Constants.HOBBIT_EXPERIMENT_URI_KEY + "="
                                                        + experimentStatus.experimentUri,
                                                Constants.BENCHMARK_PARAMETERS_MODEL_KEY + "="
                                                        + config.serializedBenchParams,
                                                Constants.SYSTEM_URI_KEY + "=" + config.systemUri },
                                        null);
                                if (containerId == null) {
                                    experimentStatus.addError(HobbitErrors.BenchmarkCreationError);
                                    throw new Exception("Couldn't create benchmark controller " + config.benchmarkUri);
                                } else {
                                    experimentStatus.setBenchmarkContainer(containerId);

                                    LOGGER.info("Creating system " + sysImageName);
                                    containerId = controller.containerManager.startContainer(sysImageName,
                                            Constants.CONTAINER_TYPE_BENCHMARK,
                                            experimentStatus.getBenchmarkContainer(),
                                            new String[] {
                                                    Constants.RABBIT_MQ_HOST_NAME_KEY + "="
                                                            + controller.rabbitMQHostName,
                                                    Constants.HOBBIT_SESSION_ID_KEY + "=" + config.id },
                                            null);
                                    if (containerId == null) {
                                        LOGGER.error("Couldn't start the system. Trying to cancel the benchmark.");
                                        forceBenchmarkTerminate_unsecured(HobbitErrors.SystemCreationError);
                                        throw new Exception("Couldn't start the system " + config.systemUri);
                                    } else {
                                        experimentStatus.setSystemContainer(containerId);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Exception while trying to start a new benchmark. Removing it from the queue.", e);
                // Add an error if there is a model but no error was added
                if (experimentStatus != null) {
                    experimentStatus.addErrorIfNonPresent(HobbitErrors.UnexpectedError);
                }
                handleExperimentTermination_unsecured();
            } finally {
                experimentMutex.release();
            }
        }

        /**
         * Sets the result model of the current running experiment.
         * 
         * @param model
         *            the result model
         */
        public void setResultModel(Model model) {
            try {
                experimentMutex.acquire();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for the experiment mutex. Returning.", e);
                return;
            }
            try {
                if (experimentStatus != null) {
                    experimentStatus.setOrMergeResultModel(model);
                } else {
                    LOGGER.error("Got a result model while there is no experiment running.");
                }
            } finally {
                experimentMutex.release();
            }
        }

        /**
         * This method handles the storing of the experiment results in the
         * database, the removing of the experiment from the queue and its
         * closing in a synchronized way.
         */
        public synchronized void handleExperimentTermination() {
            try {
                experimentMutex.acquire();
                handleExperimentTermination_unsecured();
                experimentMutex.release();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for the experiment mutex. Returning.", e);
            }
        }

        private synchronized void handleExperimentTermination_unsecured() {
            if (experimentStatus != null) {
                LOGGER.info("Benchmark terminated. Experiment " + experimentStatus.config.id
                        + " has been finished. Removing it from the queue and setting the config to null.");
                // Close the experiment to stop its internal timer
                IOUtils.closeQuietly(experimentStatus);
                // TODO add information about the hardware
                // Store the result model in DB (TODO choose the correct
                // graph)
                Model resultModel = experimentStatus.getResultModel();
                if (resultModel == null) {
                    experimentStatus.addError(HobbitErrors.UnexpectedError);
                    resultModel = experimentStatus.getResultModel();
                }
                if (!controller.storage.sendInsertQuery(resultModel, Constants.PUBLIC_RESULT_GRAPH_URI)) {
                    if (resultModel != null) {
                        StringWriter writer = new StringWriter();
                        resultModel.write(writer, "TTL");
                        LOGGER.error("Error while storing the result model of the experiment. Logging it: ",
                                writer.toString().replace(String.format("%n"), "|"));
                    }
                }
                // We have to discard the config from the queue
                controller.queue.remove(experimentStatus.config);
                // Remove the experiment status object
                experimentStatus = null;
            }
        }

        /**
         * Forces the benchmark controller and its child containers to
         * terminate. If the given error is not <code>null</code> it is added to
         * the result model of the experiment.
         * 
         * @param error
         *            error that is added to the result model of the experiment
         */
        private void forceBenchmarkTerminate_unsecured(Resource error) {
            if (experimentStatus != null) {
                String parent = experimentStatus.getBenchmarkContainer();
                controller.containerManager.stopParentAndChildren(parent);
                // if (!DEPLOY_ENV.equals(DEPLOY_ENV_TESTING)) {
                // controller.containerManager.removeParentAndChildren(parent);
                // }
                if (error != null) {
                    experimentStatus.addError(error);
                }
            }
        }

        /**
         * Handles the termination of the container with the given container Id
         * and the given exit code.
         * 
         * @param containerId
         *            Id of the terminated container
         * @param exitCode
         *            exit code of the termination
         */
        public void notifyTermination(String containerId, int exitCode) {
            try {
                experimentMutex.acquire();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for the experiment mutex. Returning.", e);
                return;
            }
            try {
                if (experimentStatus != null) {
                    // If this container is the benchmark controller of the
                    // current
                    // experiment
                    if (containerId.equals(experimentStatus.getBenchmarkContainer())) {
                        experimentStatus.setState(States.STOPPED);
                        controller.containerManager.stopParentAndChildren(containerId);
                        if (exitCode != 0) {
                            LOGGER.warn("The benchmark container " + experimentStatus.getBenchmarkContainer()
                                    + " terminated with an exit code != 0.");
                            experimentStatus.addErrorIfNonPresent(HobbitErrors.BenchmarkCrashed);
                        }
                        handleExperimentTermination_unsecured();
                        // If this is the system container and benchmark and
                        // system are not running
                    } else if (containerId.equals(experimentStatus.getSystemContainer())
                            && (experimentStatus.getState() == States.INIT)) {
                        LOGGER.info("The system has been stopped before the benchmark has been started. Aborting.");
                        // Cancel the experiment
                        forceBenchmarkTerminate_unsecured(HobbitErrors.SystemCrashed);
                    } else {
                        LOGGER.info("Sending broadcast message...");
                        // send a message using sendToCmdQueue(command,
                        // data) comprising a command that indicates that a
                        // container terminated and the container name
                        String containerName = controller.containerManager.getContainerName(containerId);
                        if (containerName != null) {
                            try {
                                controller.sendToCmdQueue(Constants.HOBBIT_SESSION_ID_FOR_BROADCASTS,
                                        Commands.DOCKER_CONTAINER_TERMINATED,
                                        RabbitMQUtils.writeByteArrays(null,
                                                new byte[][] { RabbitMQUtils.writeString(containerName) },
                                                new byte[] { (byte) exitCode }),
                                        null);
                            } catch (IOException e) {
                                LOGGER.error(
                                        "Couldn't send the " + Constants.HOBBIT_SESSION_ID_FOR_BROADCASTS
                                                + " message for container " + containerName + " to the command queue.",
                                        e);
                            }
                        } else {
                            LOGGER.info("Unknown container " + containerId + " stopped with exitCode=" + exitCode);
                        }
                    }
                }
            } finally {
                experimentMutex.release();
            }
        }

        /**
         * Handles the messages that either the system or the benchmark
         * controller are ready.
         * 
         * @param systemReportedReady
         *            <code>true</code> if the message was sent by the system,
         *            <code>false</code> if the benchmark controller is ready
         */
        public void systemOrBenchmarkReady(boolean systemReportedReady) {
            try {
                experimentMutex.acquire();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for the experiment mutex. Returning.", e);
                return;
            }
            try {
                // If there is an experiment waiting with the state INIT and if
                // both - system and benchmark are ready
                if ((experimentStatus != null) && (experimentStatus.setReadyAndCheck(systemReportedReady)
                        && (experimentStatus.getState() == States.INIT))) {
                    try {
                        startBenchmark_unsecured();
                    } catch (IOException e) {
                        // Let's retry this
                        try {
                            startBenchmark_unsecured();
                        } catch (IOException e2) {
                            LOGGER.error(
                                    "Couldn't sent start signal to the benchmark controller. Terminating experiment.",
                                    e2);
                            // We have to terminate the experiment
                            forceBenchmarkTerminate_unsecured(HobbitErrors.UnexpectedError);
                        }
                    }
                }
            } finally {
                experimentMutex.release();
            }
        }

        /**
         * Sends the start message to the benchmark controller.
         * 
         * @throws IOException
         *             if there is a communication problem or if the name of the
         *             system container can not be retrieved from the docker
         *             daemon
         */
        private void startBenchmark_unsecured() throws IOException {
            String containerName = controller.containerManager.getContainerName(experimentStatus.getSystemContainer());
            if (containerName == null) {
                throw new IOException(
                        "Couldn't derive container name of the system container for sending start message to the benchmark.");
            }
            try {
                // send the start signal (we are only reading the config object
                // and do not need to wait for the mutex)
                controller.sendToCmdQueue(experimentStatus.config.id, Commands.START_BENCHMARK_SIGNAL,
                        RabbitMQUtils.writeString(containerName), null);
                experimentStatus.setState(States.STARTED);
            } catch (IOException e) {
                LOGGER.error("Couldn't send " + Commands.START_BENCHMARK_SIGNAL + " signal for experiment "
                        + experimentStatus.config.id, e);
                throw e;
            }
        }

        /**
         * Adds the status of the current experiment to the given status object.
         * 
         * @param status
         *            the status object to which the data should be added
         */
        public void addStatusInfo(ControllerStatus status) {
            try {
                experimentMutex.acquire();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for the experiment mutex. Returning empty status.", e);
                return;
            }
            try {
                if (experimentStatus != null) {
                    status.currentBenchmarkName = experimentStatus.config.benchmarkName;
                    status.currentBenchmarkUri = experimentStatus.config.benchmarkUri;
                    status.currentSystemUri = experimentStatus.config.systemUri;
                    status.currentExperimentId = experimentStatus.config.id;
                    status.currentStatus = experimentStatus.getState().description;
                }
            } catch (Exception e) {
                LOGGER.error("Exception while trying to generate controller status object.", e);
            } finally {
                experimentMutex.release();
            }
        }

        /**
         * Changes the state of the internal experiment to
         * {@link States#EVALUATION}.
         */
        public void taskGenFinished() {
            try {
                experimentMutex.acquire();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for the experiment mutex. Returning empty status.", e);
                return;
            }
            try {
                if (experimentStatus != null) {
                    experimentStatus.setState(States.EVALUATION);
                }
            } finally {
                experimentMutex.release();
            }
        }

        @Override
        public void close() throws IOException {
            expStartTimer.cancel();
        }

    }

    ///// There are some methods that shouldn't be used by the controller and
    ///// have been marked as deprecated

    /**
     * @deprecated Not used inside the controller. Use
     *             {@link #receiveCommand(byte, byte[], String, String)}
     *             instead.
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
}
