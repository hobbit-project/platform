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
package org.hobbit.controller.docker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hobbit.controller.gitlab.GitlabControllerImpl;
import org.hobbit.controller.utils.Waiting;
import org.hobbit.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ServiceNotFoundException;
import com.spotify.docker.client.exceptions.TaskNotFoundException;
import com.spotify.docker.client.messages.ContainerStats;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.NetworkConfig;
import com.spotify.docker.client.messages.RegistryAuth;
import com.spotify.docker.client.messages.ServiceCreateResponse;
import com.spotify.docker.client.messages.swarm.ContainerSpec;
import com.spotify.docker.client.messages.swarm.Driver;
import com.spotify.docker.client.messages.swarm.NetworkAttachmentConfig;
import com.spotify.docker.client.messages.swarm.Placement;
import com.spotify.docker.client.messages.swarm.RestartPolicy;
import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.ServiceMode;
import com.spotify.docker.client.messages.swarm.ServiceSpec;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.TaskSpec;
import com.spotify.docker.client.messages.swarm.TaskStatus;

/**
 * Created by Timofey Ermilov on 31/08/16
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 */
public class ContainerManagerImpl implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);

    public static final String DEPLOY_ENV_KEY = "DEPLOY_ENV";
    public static final String DOCKER_AUTOPULL_ENV_KEY = "DOCKER_AUTOPULL";
    public static final String LOGGING_GELF_ADDRESS_KEY = "LOGGING_GELF_ADDRESS";
    public static final String USER_NAME_KEY = "GITLAB_USER";
    public static final String USER_EMAIL_KEY = "GITLAB_EMAIL";
    public static final String USER_PASSWORD_KEY = GitlabControllerImpl.GITLAB_TOKEN_KEY;
    public static final String REGISTRY_URL_KEY = "REGISTRY_URL";

    private static final int DOCKER_MAX_NAME_LENGTH = 63;

    private static final String DEPLOY_ENV = System.getenv().containsKey(DEPLOY_ENV_KEY)
            ? System.getenv().get(DEPLOY_ENV_KEY)
            : "production";
    private static final String DEPLOY_ENV_TESTING = "testing";
    private static final String DEPLOY_ENV_DEVELOP = "develop";
    private static final String LOGGING_DRIVER_GELF = "gelf";
    private static final Pattern PORT_PATTERN = Pattern.compile(":[0-9]+/");

    private static final Boolean DOCKER_AUTOPULL = System.getenv().containsKey(DOCKER_AUTOPULL_ENV_KEY)
            ? System.getenv().get(DOCKER_AUTOPULL_ENV_KEY) == "1"
            : true;

    private static final long DOCKER_POLL_INTERVAL = 100;
    private static final long DOCKER_IMAGE_PULL_MAX_WAITING_TIME = 1200000; // 20 min

    /**
     * Default network for new containers
     */
    public static final String HOBBIT_DOCKER_NETWORK = "hobbit";
    /**
     * Logging pattern.
     */
    public static final String LOGGING_TAG = "{{.ImageName}}/{{.Name}}/{{.ID}}";
    /**
     * Task states which are considered as not running yet.
     */
    public static final Set<String> NEW_TASKS_STATES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(new String[] { TaskStatus.TASK_STATE_NEW, TaskStatus.TASK_STATE_ALLOCATED,
                    TaskStatus.TASK_STATE_PENDING, TaskStatus.TASK_STATE_ASSIGNED, TaskStatus.TASK_STATE_ACCEPTED,
                    TaskStatus.TASK_STATE_PREPARING, TaskStatus.TASK_STATE_READY, TaskStatus.TASK_STATE_STARTING, })));
    /**
     * Task states which are considered as not finished yet.
     */
    public static final Set<String> UNFINISHED_TASK_STATES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            new String[] { TaskStatus.TASK_STATE_NEW, TaskStatus.TASK_STATE_ALLOCATED, TaskStatus.TASK_STATE_PENDING,
                    TaskStatus.TASK_STATE_ASSIGNED, TaskStatus.TASK_STATE_ACCEPTED, TaskStatus.TASK_STATE_PREPARING,
                    TaskStatus.TASK_STATE_READY, TaskStatus.TASK_STATE_STARTING, TaskStatus.TASK_STATE_RUNNING, })));
    /**
     * Logging separator for type/experiment id.
     */
    private static final String LOGGING_SEPARATOR = "_sep_";
    /**
     * Docker client instance
     */
    private DockerClient dockerClient;
    /**
     * Authentication configuration for accessing private repositories.
     */
    private final RegistryAuth gitlabAuth;
    /**
     * Empty authentication configuration. Docker client's createService() uses
     * ConfigFileRegistryAuthSupplier by default (if auth is omitted) and warns
     * about not being able to use it with swarm each time.
     */
    private final RegistryAuth nullAuth = RegistryAuth.builder().build();
    /**
     * Observers that should be notified if a container terminates.
     */
    private List<ContainerStateObserver> containerObservers = new ArrayList<>();

    private String gelfAddress = null;
    private String experimentId = null;

    /**
     * Constructor that creates new docker client instance
     */
    public ContainerManagerImpl() throws Exception {
        LOGGER.info("Deployed as \"{}\".", DEPLOY_ENV);
        dockerClient = DockerUtility.getDockerClient();

        String username = System.getenv(USER_NAME_KEY);
        String email = System.getenv(USER_EMAIL_KEY);
        String password = System.getenv(USER_PASSWORD_KEY);
        String registryUrl = System.getenv().containsKey(REGISTRY_URL_KEY) ? System.getenv(REGISTRY_URL_KEY)
                : "git.project-hobbit.eu:4567";
        if ((username != null) && (password != null)) {
            gitlabAuth = RegistryAuth.builder().serverAddress(registryUrl).username(username).password(password)
                    .email(email).build();
        } else {
            LOGGER.warn(
                    "Couldn't load a username ({}), email ({}) and a security token ({}) to access private repositories. This platform won't be able to pull protected or private images.",
                    USER_NAME_KEY, USER_EMAIL_KEY, USER_PASSWORD_KEY);
            gitlabAuth = null;
        }
        gelfAddress = System.getenv(LOGGING_GELF_ADDRESS_KEY);
        if (gelfAddress == null) {
            LOGGER.info(
                    "Didn't find a gelf address ({}). Containers created by this platform will use the default logging.",
                    LOGGING_GELF_ADDRESS_KEY);
        }
        // try to find hobbit network in existing ones
        List<Network> networks = dockerClient.listNetworks();
        String hobbitNetwork = null;
        for (Network net : networks) {
            if (net.name().equals(HOBBIT_DOCKER_NETWORK)) {
                hobbitNetwork = net.id();
                break;
            }
        }
        // if not found - create new one
        if (hobbitNetwork == null) {
            LOGGER.warn("Could not find hobbit docker network, creating a new one");
            final NetworkConfig networkConfig = NetworkConfig.builder().name(HOBBIT_DOCKER_NETWORK).driver("overlay")
                    .build();
            dockerClient.createNetwork(networkConfig);
        }
    }

    /**
     * Generates new unique instance name based on image name
     *
     * @param imageName
     *            base image name
     *
     * @return instance name
     */
    private String getInstanceName(String imageName) {
        return getInstanceName(imageName, "");
    }

    /**
     * Generates new unique instance name based on image name
     *
     * @param imageName
     *            base image name
     * @param prefix
     *            additional prefix
     *
     * @return instance name
     */
    private String getInstanceName(String imageName, String prefix) {
        String baseName = imageName;
        // If there is a tag it has to be removed
        if (containsVersionTag(baseName)) {
            int pos = imageName.lastIndexOf(':');
            baseName = baseName.substring(0, pos - 1);
        }
        int posSlash = baseName.lastIndexOf('/');
        int posColon = baseName.lastIndexOf(':');
        if (posSlash > posColon) {
            baseName = baseName.substring(posSlash + 1);
        } else if (posSlash < posColon) {
            baseName = baseName.substring(posColon + 1);
        }
        final String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        StringBuilder builder = new StringBuilder(prefix.length() + baseName.length() + uuid.length() + 2);
        if (prefix.length() != 0) {
            builder.append(prefix);
            builder.append('-');
        }
        builder.append(baseName.replaceAll("[^-a-z0-9]", "-"));
        int maxLength = DOCKER_MAX_NAME_LENGTH - 1 - uuid.length();
        if (builder.length() > maxLength) {
            builder.setLength(maxLength);
        }
        builder.append('-');
        builder.append(uuid);
        return builder.toString();
    }

    private ServiceCreateResponse createService(ServiceSpec serviceSpec) throws DockerException, InterruptedException {
        // If we have authentication credentials and the image name contains
        // the server address of these credentials, we should use them
        if ((gitlabAuth != null) && (serviceSpec.taskTemplate().containerSpec().image().startsWith(gitlabAuth.serverAddress()))) {
            return dockerClient.createService(serviceSpec, gitlabAuth);
        } else {
            return dockerClient.createService(serviceSpec, nullAuth);
        }
    }

    /**
     * Pulls the image with the given name.
     *
     * @param imageName
     *            the name of the image that should be pulled
     */
    public void pullImage(String imageName) {
        // do not pull if env var is set to false
        if (!DOCKER_AUTOPULL) {
            LOGGER.warn("Skipping image pulling because DOCKER_AUTOPULL is unset");
            return;
        }

        LOGGER.info("Pulling the image \"{}\"", imageName);

        ServiceSpec.Builder serviceCfgBuilder = ServiceSpec.builder();
        TaskSpec.Builder taskCfgBuilder = TaskSpec.builder();
        ContainerSpec.Builder cfgBuilder = ContainerSpec.builder();

        serviceCfgBuilder.mode(ServiceMode.withGlobal());

        taskCfgBuilder.restartPolicy(RestartPolicy.builder().condition(RestartPolicy.RESTART_POLICY_NONE).build());

        cfgBuilder.image(imageName);

        // TODO: put some labels on it?

        // create logging info
        if (gelfAddress != null) {
            Map<String, String> logOptions = new HashMap<String, String>();
            logOptions.put("gelf-address", gelfAddress);
            logOptions.put("tag", LOGGING_TAG);
            taskCfgBuilder.logDriver(Driver.builder().name(LOGGING_DRIVER_GELF).options(logOptions).build());
        }

        // hello-world image used in tests (so we can safely remove all containers
        // depending on it)
        // and it does not have anything besides "/hello" executable
        String[] command = imageName.equals("hello-world") ? new String[] { "/hello" } : new String[] { "true" };
        cfgBuilder.command(command);

        ContainerSpec cfg = cfgBuilder.build();
        taskCfgBuilder.containerSpec(cfg);
        serviceCfgBuilder.taskTemplate(taskCfgBuilder.build());
        serviceCfgBuilder.name(getInstanceName(imageName, "pull"));
        ServiceSpec serviceCfg = serviceCfgBuilder.build();
        Integer totalNodes;
        try {
            // TODO: use ClusterManager
            totalNodes = dockerClient.listNodes().size();
        } catch (Exception e) {
            LOGGER.error("Couldn't retrieve list of swarm nodes!");
            return;
        }
        try {
            ServiceCreateResponse resp = createService(serviceCfg);
            String serviceId = resp.id();
            LOGGER.info("Pulling service id: {}", serviceId);

            // create a set to collect the tasks of nodes that have finished the pulling
            final Set<String> finshedTaskIds = Collections.synchronizedSet(new HashSet<String>());

            // wait for any container of that service to start on each node
            try {
                Waiting.waitFor(() -> {
                    List<Task> pullingTasks = dockerClient
                            .listTasks(Task.Criteria.builder().serviceName(serviceId).build());
                    for (Task pullingTask : pullingTasks) {
                        String state = pullingTask.status().state();
                        if (!UNFINISHED_TASK_STATES.contains(state)) {
                            if (state.equals(TaskStatus.TASK_STATE_REJECTED)) {
                                LOGGER.error("Couldn't pull image {} on node {}. {}", imageName, pullingTask.nodeId(),
                                        pullingTask.status().err());
                                throw new Exception("Couldn't pull image on node " + pullingTask.nodeId() + ": " + pullingTask.status().err());
                            }
                            finshedTaskIds.add(pullingTask.id());
                        }
                    }
                    if (finshedTaskIds.size() >= totalNodes) {
                        LOGGER.info("Swarm pulled the image \"{}\" ({})", imageName,
                                pullingTasks.stream().map(t -> t.status().state()).collect(Collectors.joining(", ")));
                        return true;
                    } else {
                        return false;
                    }
                }, DOCKER_POLL_INTERVAL, DOCKER_IMAGE_PULL_MAX_WAITING_TIME);
            } catch (InterruptedException e) {
                LOGGER.warn(
                        "Interrupted while waiting for the image {} to be pulled. Assuming that pulling was successful. Exception: {}",
                        imageName, e.getLocalizedMessage());
            }

            dockerClient.removeService(serviceId);
        } catch (Exception e) {
            LOGGER.error("Exception while pulling the image \"" + imageName + "\".", e);
        }
    }

    /**
     * Creates new container using given image and assigns given type and parent
     *
     * @param imageName
     *            image to use as base for container
     * @param containerType
     *            container type
     * @param parentId
     *            parent id
     * @param env
     *            (optional) environment variables
     * @param command
     *            (optional) command to be executed with image
     *
     * @return String the container Id or <code>null</code> if an error occurs
     */
    private String createContainer(String imageName, String containerType, String parentId, String[] env,
            String[] netAliases, String[] command) {
        ServiceSpec.Builder serviceCfgBuilder = ServiceSpec.builder();

        TaskSpec.Builder taskCfgBuilder = TaskSpec.builder();
        // we need to run it just once; configure to never restart
        taskCfgBuilder.restartPolicy(RestartPolicy.builder().condition(RestartPolicy.RESTART_POLICY_NONE).build());

        ContainerSpec.Builder cfgBuilder = ContainerSpec.builder();
        cfgBuilder.image(imageName);

        // generate unique container name
        String serviceName = getInstanceName(imageName);
        cfgBuilder.hostname(serviceName);
        // get parent info
        List<String> defaultEnv = new ArrayList<>();
        defaultEnv.add(Constants.CONTAINER_NAME_KEY + "=" + serviceName);
        Service parent = null;
        try {
            parent = getContainerInfo(parentId);
        } catch (Exception e) {
        }
        String parentType = (parent == null) ? null : parent.spec().labels().get(LABEL_TYPE);
        // If there is no container type try to use the parent type or return
        // null
        if ((containerType == null) || containerType.isEmpty()) {
            if ((parentType != null) && (!parentType.isEmpty())) {
                containerType = parentType;
            } else {
                LOGGER.error(
                        "Can't create container using image {} without a container type (either a given type or one that can be derived from the parent). Returning null.",
                        imageName);
                return null;
            }
        }

        // Assume we have at least one node (which we're running at).
        long numberOfSwarmNodes = 1;
        long numberOfSystemSwarmNodes = 0;
        long numberOfBenchmarkSwarmNodes = 0;
        try {
            ClusterManager clusterManager = new ClusterManagerImpl();
            numberOfSwarmNodes = clusterManager.getNumberOfNodes();
            numberOfSystemSwarmNodes = clusterManager.getNumberOfNodes("org.hobbit.workergroup=system");
            numberOfBenchmarkSwarmNodes = clusterManager.getNumberOfNodes("org.hobbit.workergroup=benchmark");
        } catch (DockerCertificateException e) {
            LOGGER.error("Could not initialize Cluster Manager, will use container placement constraints by default. ",
                    e);
        } catch (Exception e) {
            LOGGER.error("Could not get number of swarm nodes. ", e);
        }

        if (numberOfSwarmNodes > 1) {
            // If the parent has "system" --> we do not care what the container
            // would like to have OR if there is no parent or the parent is a
            // benchmark (in case of the benchmark controller) and the container has
            // type "system"
            if ((((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))
                    && Constants.CONTAINER_TYPE_SYSTEM.equals(containerType))
                    || Constants.CONTAINER_TYPE_SYSTEM.equals(parentType)) {
                taskCfgBuilder.placement(Placement
                        .create(new ArrayList<String>(Arrays.asList("node.labels.org.hobbit.workergroup==system"))));
                containerType = Constants.CONTAINER_TYPE_SYSTEM;
            } else if (Constants.CONTAINER_TYPE_DATABASE.equals(containerType)
                    && ((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType)
                            || Constants.CONTAINER_TYPE_DATABASE.equals(parentType))) {
                // defaultEnv.add("constraint:org.hobbit.workergroup==" +
                // Constants.CONTAINER_TYPE_DATABASE);
                // defaultEnv.add("constraint:org.hobbit.type==data");
                // database containers have to be deployed on the benchmark nodes (see
                // https://github.com/hobbit-project/platform/issues/170)
                taskCfgBuilder.placement(Placement
                        .create(new ArrayList<String>(Arrays.asList("node.labels.org.hobbit.workergroup==benchmark"))));
            } else if (Constants.CONTAINER_TYPE_BENCHMARK.equals(containerType)
                    && ((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))) {
                taskCfgBuilder.placement(Placement
                        .create(new ArrayList<String>(Arrays.asList("node.labels.org.hobbit.workergroup==benchmark"))));
            } else {
                LOGGER.error("Got a request to create a container with type={} and parentType={}. "
                        + "Got no rule to determine its type. Returning null.", containerType, parentType);
                return null;
            }
        } else {
            LOGGER.warn("The swarm cluster got only 1 node, I will not use placement constraints.");
        }

        // add hardware information to environment
        defaultEnv.add(Constants.HARDWARE_NUMBER_OF_NODES_KEY + "=" + numberOfSwarmNodes);
        defaultEnv.add(Constants.HARDWARE_NUMBER_OF_SYSTEM_NODES_KEY + "=" + numberOfSystemSwarmNodes);
        defaultEnv.add(Constants.HARDWARE_NUMBER_OF_BENCHMARK_NODES_KEY + "=" + numberOfBenchmarkSwarmNodes);

        // create env vars to pass
        if (env != null) {
            defaultEnv.addAll(Arrays.asList(env));
            cfgBuilder.env(defaultEnv);
        } else {
            cfgBuilder.env(defaultEnv);
        }
        // create container labels
        Map<String, String> labels = new HashMap<>();
        labels.put(LABEL_TYPE, containerType);
        if (parentId != null) {
            labels.put(LABEL_PARENT, parentId);
        }
        serviceCfgBuilder.labels(labels);
        cfgBuilder.labels(labels);
        // create logging info
        if (gelfAddress != null) {
            Map<String, String> logOptions = new HashMap<String, String>();
            logOptions.put("gelf-address", gelfAddress);
            String tag = LOGGING_TAG;
            if (experimentId != null) {
                tag = containerType + LOGGING_SEPARATOR + experimentId + LOGGING_SEPARATOR + LOGGING_TAG;
            }
            logOptions.put("tag", tag);
            taskCfgBuilder.logDriver(Driver.builder().name(LOGGING_DRIVER_GELF).options(logOptions).build());
        }

        // if command is present - execute it
        if ((command != null) && (command.length > 0)) {
            cfgBuilder.command(command);
        }

        // trigger creation
        ContainerSpec cfg = cfgBuilder.build();
        taskCfgBuilder.containerSpec(cfg);
        serviceCfgBuilder.taskTemplate(taskCfgBuilder.build());

        // connect to hobbit network only
        serviceCfgBuilder.networks(
            NetworkAttachmentConfig.builder().target(HOBBIT_DOCKER_NETWORK).aliases(netAliases).build()
        );

        serviceCfgBuilder.name(serviceName);
        ServiceSpec serviceCfg = serviceCfgBuilder.build();
        String serviceId = null;
        try {
            ServiceCreateResponse resp = createService(serviceCfg);
            serviceId = resp.id();
            final String serviceIdForLambda = serviceId;
            // wait for a container of that service to start
            List<Task> serviceTasks = new ArrayList<Task>();
            Waiting.waitFor(() -> {
                serviceTasks.clear();
                serviceTasks.addAll(dockerClient.listTasks(Task.Criteria.builder().serviceName(serviceIdForLambda).build()));

                if (!serviceTasks.isEmpty()) {
                    TaskStatus status = serviceTasks.get(0).status();
                    if (status.state().equals(TaskStatus.TASK_STATE_PENDING)) {
                        if (status.err() != null && status.err().matches("no suitable node.*")) {
                            throw new Exception(status.err());
                        }
                    }
                    return !NEW_TASKS_STATES.contains(status.state());
                }

                return false;
            }, DOCKER_POLL_INTERVAL);
            // return new container id
            LOGGER.info("Container {} created", serviceName);
            return serviceName;
        } catch (Exception e) {
            if (serviceId != null) {
                try {
                    LOGGER.info("Removing service {} which didn't cleanly start", serviceId);
                    dockerClient.removeService(serviceId);
                } catch (Exception cleanupE) {
                    LOGGER.error("Couldn't remove service {} which didn't cleanly start", serviceId, cleanupE);
                }
            }

            LOGGER.error("Couldn't create Docker container. Returning null.", e);
            return null;
        }
    }

    @Deprecated
    public String startContainer(String imageName) {
        return startContainer(imageName, null, "", null);
    }

    @Deprecated
    public String startContainer(String imageName, String[] command) {
        return startContainer(imageName, null, "", command);
    }

    public String startContainer(String imageName, String type, String parent) {
        return startContainer(imageName, type, parent, null);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] command) {
        return startContainer(imageName, containerType, parentId, null, command);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
            String[] command) {
        return startContainer(imageName, containerType, parentId, env, null, command);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
    String[] netAliases, String[] command) {
        return startContainer(imageName, containerType, parentId, env, netAliases, command, true);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
            String[] command, boolean pullImage) {
        return startContainer(imageName, containerType, parentId, env, null, command, true);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
            String[] netAliases, String[] command, boolean pullImage) {
        if (pullImage) {
            pullImage(imageName);
        }

        String containerId = createContainer(imageName, containerType, parentId, env, netAliases, command);

        // if the creation was successful
        if (containerId != null) {
            for (ContainerStateObserver observer : containerObservers) {
                observer.addObservedContainer(containerId);
            }
            return containerId;
        }
        return null;
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env,
            String[] netAliases, String[] command, String experimentId) {
        this.experimentId = experimentId;
        return startContainer(imageName, containerType, parentId, env, netAliases, command);
    }

    @Override
    public void removeContainer(String serviceName) {
        try {
            Long exitCode = getContainerExitCode(serviceName);
            if (DEPLOY_ENV.equals(DEPLOY_ENV_DEVELOP)) {
                LOGGER.info(
                        "Will not remove container {}. "
                        + "Development mode is enabled.",
                        serviceName);
            } else if (DEPLOY_ENV.equals(DEPLOY_ENV_TESTING) && (exitCode != null && exitCode != 0)) {
                // In testing - do not remove containers if they returned non-zero exit code
                // null exit code usually means that the container is running at the moment
                LOGGER.info(
                        "Will not remove container {}. "
                        + "ExitCode: {} != 0 and testing mode is enabled.",
                        serviceName, exitCode);
            } else {
                LOGGER.info("Removing container {}. ", serviceName);
                dockerClient.removeService(serviceName);

                // wait for the service to disappear
                Waiting.waitFor(() -> {
                    try {
                        dockerClient.inspectService(serviceName);
                        return false;
                    } catch (ServiceNotFoundException e) {
                        return true;
                    }
                }, DOCKER_POLL_INTERVAL);
            }
        } catch (TaskNotFoundException | ServiceNotFoundException e) {
            LOGGER.error("Couldn't remove container {} because it doesn't exist", serviceName);
        } catch (Exception e) {
            LOGGER.error("Couldn't remove container {}.", serviceName, e);
        }
    }

    @Deprecated
    @Override
    public void stopContainer(String containerId) {
        LOGGER.error("ContainerManager.stopContainer() is deprecated! Will remove container instead");
        removeContainer(containerId);
    }

    @Deprecated
    @Override
    public void stopParentAndChildren(String parentId) {
        LOGGER.error("ContainerManager.stopParentAndChildren() is deprecated! Will remove them instead");
        removeParentAndChildren(parentId);
    }

    @Override
    public void removeParentAndChildren(String parent) {
        // stop parent
        removeContainer(parent);

        // find children
        try {
            List<Service> services = dockerClient.listServices(Service.Criteria.builder().labels(ImmutableMap.of(LABEL_PARENT, parent)).build());

            for (Service c : services) {
                if (c != null) {
                    removeParentAndChildren(c.spec().name());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while removing containers: " + e.toString());
        }
    }

    @Override
    public Service getContainerInfo(String serviceName) throws InterruptedException, DockerException {
        if (serviceName == null) {
            return null;
        }
        Service info = null;
        try {
            info = dockerClient.inspectService(serviceName);
        } catch (ServiceNotFoundException e) {
            // return null
        }
        return info;
    }

    @Override
    public List<Service> getContainers(Service.Criteria criteria) {
        try {
            return dockerClient.listServices(criteria);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public Long getContainerExitCode(String serviceName) throws DockerException, InterruptedException {
        if (getContainerInfo(serviceName) == null) {
            LOGGER.warn("Couldn't get the exit code for container {}. Service doesn't exist. Assuming it was stopped by the platform.", serviceName);
            return DOCKER_EXITCODE_SIGKILL;
        }

        // Service exists, but no tasks are observed.
        List<Task> tasks = dockerClient.listTasks(Task.Criteria.builder().serviceName(serviceName).build());
        if (tasks.size() == 0) {
            LOGGER.warn("Couldn't get the exit code for container {}. Service has no tasks. Returning null.", serviceName);
            return null;
        }

        for (Task task : tasks) {
            if (!UNFINISHED_TASK_STATES.contains(task.status().state())) {
                // Task is finished.
                Long exitCode = task.status().containerStatus().exitCode();
                if (exitCode == null) {
                    LOGGER.warn("Couldn't get the exit code for container {}. Task is finished. Returning 0.", serviceName);
                    return 0l;
                }
                return exitCode;
            }
        }

        // Task is not finished.
        return null;
    }

    @Deprecated
    @Override
    public String getContainerId(String name) {
        return name;
    }

    @Deprecated
    @Override
    public String getContainerName(String containerId) {
        return containerId;
    }

    @Override
    public void addContainerObserver(ContainerStateObserver containerObserver) {
        containerObservers.add(containerObserver);
    }

    public static boolean containsVersionTag(String imageName) {
        int pos = 0;
        // Check whether the given image name contains a host with a port
        Matcher matcher = PORT_PATTERN.matcher(imageName);
        while (matcher.find()) {
            pos = matcher.end();
        }
        // Check whether there is a ':' in the remaining part of the image name
        return imageName.indexOf(':', pos) >= 0;
    }

    @Override
    public ContainerStats getStats(String containerId) {
        ContainerStats stats = null;
        try {
            stats = dockerClient.stats(containerId);
        } catch (Exception e) {
            LOGGER.warn("Error while requesting usage stats for {}. Returning null. Error: {}", containerId,
                    e.getLocalizedMessage());
        }
        return stats;
    }
}
