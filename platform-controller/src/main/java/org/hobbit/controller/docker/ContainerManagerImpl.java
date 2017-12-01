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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hobbit.controller.gitlab.GitlabControllerImpl;
import org.hobbit.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DefaultDockerClient.Builder;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ServiceNotFoundException;
import com.spotify.docker.client.exceptions.TaskNotFoundException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.NetworkConfig;
import com.spotify.docker.client.messages.RegistryAuth;
import com.spotify.docker.client.messages.ServiceCreateResponse;
import com.spotify.docker.client.messages.swarm.ContainerSpec;
import com.spotify.docker.client.messages.swarm.Driver;
import com.spotify.docker.client.messages.swarm.NetworkAttachmentConfig;
import com.spotify.docker.client.messages.swarm.RestartPolicy;
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

    private static final String DEPLOY_ENV = System.getenv().containsKey(DEPLOY_ENV_KEY)
            ? System.getenv().get(DEPLOY_ENV_KEY) : "production";
    private static final String DEPLOY_ENV_TESTING = "testing";
    private static final String LOGGING_DRIVER_GELF = "gelf";
    private static final Pattern PORT_PATTERN = Pattern.compile(":[0-9]+/");

    private static final Boolean DOCKER_AUTOPULL = System.getenv().containsKey(DOCKER_AUTOPULL_ENV_KEY)
            ? System.getenv().get(DOCKER_AUTOPULL_ENV_KEY) == "1" : true;

    /**
     * Label that denotes container type
     */
    public static final String LABEL_TYPE = "org.hobbit.type";
    /**
     * Label that denotes container parent
     */
    public static final String LABEL_PARENT = "org.hobbit.parent";
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
    private static final List<String> newTaskStates = Arrays.asList(new String[] {
        TaskStatus.TASK_STATE_NEW,
        TaskStatus.TASK_STATE_ALLOCATED,
        TaskStatus.TASK_STATE_PENDING,
        TaskStatus.TASK_STATE_ASSIGNED,
        TaskStatus.TASK_STATE_ACCEPTED,
        TaskStatus.TASK_STATE_PREPARING,
        TaskStatus.TASK_STATE_READY,
        TaskStatus.TASK_STATE_STARTING,
    });
    /**
     * Task states which are considered as not finished yet.
     */
    private static final List<String> unfinishedTaskStates = Arrays.asList(new String[] {
        TaskStatus.TASK_STATE_NEW,
        TaskStatus.TASK_STATE_ALLOCATED,
        TaskStatus.TASK_STATE_PENDING,
        TaskStatus.TASK_STATE_ASSIGNED,
        TaskStatus.TASK_STATE_ACCEPTED,
        TaskStatus.TASK_STATE_PREPARING,
        TaskStatus.TASK_STATE_READY,
        TaskStatus.TASK_STATE_STARTING,
        TaskStatus.TASK_STATE_RUNNING,
    });
    /**
     * Docker client instance
     */
    private DockerClient dockerClient;
    /**
     * Authentication configuration for accessing private repositories.
     */
    private final RegistryAuth gitlabAuth;
    /**
     * Observers that should be notified if a container terminates.
     */
    private List<ContainerStateObserver> containerObservers = new ArrayList<>();

    private String gelfAddress = null;

    /**
     * Constructor that creates new docker client instance
     */
    public ContainerManagerImpl() throws Exception {
        LOGGER.info("Deployed as \"{}\".", DEPLOY_ENV);
        Builder builder = DefaultDockerClient.fromEnv();
        builder.connectionPoolSize(5000);
        builder.connectTimeoutMillis(1000);
        dockerClient = builder.build();

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
            final NetworkConfig networkConfig = NetworkConfig.builder().name(HOBBIT_DOCKER_NETWORK).build();
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
        StringBuilder builder = new StringBuilder(baseName.length() + uuid.length() + 1);
        builder.append(baseName.replaceAll("[/\\.]", "_"));
        builder.append('-');
        builder.append(uuid);
        return builder.toString();
    }

    /**
     * Pulls the image with the given name if it does not appear in the list of
     * available images.
     *
     * @param imageName
     *            the name of the image that should be pulled
     */
    @SuppressWarnings("unused")
    private void pullImageIfNeeded(String imageName) {
        // do not pull if env var is set to false
        if (!DOCKER_AUTOPULL) {
            return;
        }

        if (!containsVersionTag(imageName)) {
            imageName += ":latest";
        }

        // check if image is already available
        try {
            List<Image> images = dockerClient.listImages();
            for (Image image : images) {
                if (image.repoTags() != null) {
                    for (String tag : image.repoTags()) {
                        if (tag.equals(imageName)) {
                            return;
                        }
                    }
                }
            }

            // pull image and wait for the pull to finish
            pullImage(imageName);
        } catch (Exception e) {
            LOGGER.error("Exception while pulling the image \"" + imageName + "\". " + e.getClass().getName() + ": "
                    + e.getLocalizedMessage());
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

        // hello-world image used in tests (so we can safely remove all containers depending on it)
        // and it does not have anything besides "/hello" executable
        String[] command = imageName.equals("hello-world") ? new String[]{ "/hello" } : new String[]{  "true" };
        cfgBuilder.command(command);

        ContainerSpec cfg = cfgBuilder.build();
        taskCfgBuilder.containerSpec(cfg);
        serviceCfgBuilder.taskTemplate(taskCfgBuilder.build());
        ServiceSpec serviceCfg = serviceCfgBuilder.build();
        Integer totalNodes = 0;
        try {
            totalNodes = dockerClient.listNodes().size();
        } catch (Exception e) {
            LOGGER.error("Couldn't retrieve list of swarm nodes!");
            return;
        }
        Integer alreadyPulled = 0;
        // SWARM FIXME: remove file LoggingPullHandler
        try {
            ServiceCreateResponse resp;
            // If we have authentication credentials and the image name contains
            // the server address of these credentials, we should use them
            if ((gitlabAuth != null) && (imageName.startsWith(gitlabAuth.serverAddress()))) {
                // pull image and wait for the pull to finish
                resp = dockerClient.createService(serviceCfg, gitlabAuth);
            } else {
                // pull image and wait for the pull to finish
                resp = dockerClient.createService(serviceCfg);
            }
            String serviceId = resp.id();

            // FIXME wait for any container of that service to start
            List<Task> pullingTasks;
            do {
                Thread.sleep(100);

                pullingTasks = dockerClient.listTasks(Task.Criteria.builder().serviceName(serviceId).build());
                Integer nowPulled = 0;
                for (Task pullingTask : pullingTasks) {
                    String state = pullingTask.status().state();
                    if (!unfinishedTaskStates.contains(state)) {
                        if (state.equals(TaskStatus.TASK_STATE_REJECTED)) {
                            LOGGER.error("Couldn't pull image {} on node {}. {}", imageName, pullingTask.nodeId(), pullingTask.status().err());
                            return;
                        }

                        nowPulled++;
                    }
                }

                if (nowPulled > alreadyPulled) {
                    alreadyPulled = nowPulled;
                    /*
                    LOGGER.info("{}/{} swarm nodes pulled image '{}'", alreadyPulled, totalNodes, imageName);
                    */
                }
            } while (alreadyPulled < totalNodes);

            LOGGER.info("{} swarm nodes pulled image '{}' with statuses: {}", totalNodes, imageName,
                    pullingTasks.stream().map(t -> t.status().state()).collect(Collectors.joining(", ")));

            dockerClient.removeService(serviceId);
        } catch (Exception e) {
            LOGGER.error("Exception while pulling the image \"" + imageName + "\". " + e.getClass().getName() + ": "
                    + e.getLocalizedMessage());
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
            String[] command) {
        ServiceSpec.Builder serviceCfgBuilder = ServiceSpec.builder();

        TaskSpec.Builder taskCfgBuilder = TaskSpec.builder();
        // we need to run it just once; configure to never restart
        taskCfgBuilder.restartPolicy(RestartPolicy.builder().condition(RestartPolicy.RESTART_POLICY_NONE).build());

        // pull image
        pullImage(imageName);
        ContainerSpec.Builder cfgBuilder = ContainerSpec.builder();
        cfgBuilder.image(imageName);

        // generate unique container name
        String containerName = getInstanceName(imageName);
        cfgBuilder.hostname(containerName);
        // get parent info
        List<String> defaultEnv = new ArrayList<>();
        defaultEnv.add(Constants.CONTAINER_NAME_KEY + "=" + containerName);
        Task parent = null;
        try {
            parent = getContainerInfo(parentId);
        } catch (Exception e) {
        }
        String parentType = (parent == null) ? null : parent.labels().get(LABEL_TYPE);
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
        // If the parent has "system" --> we do not care what the container
        // would like to have OR if there is no parent or the parent is a
        // benchmark (in case of the benchmark controller) and the container has
        // type "system"
        if ((((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))
                && Constants.CONTAINER_TYPE_SYSTEM.equals(containerType))
                || Constants.CONTAINER_TYPE_SYSTEM.equals(parentType)) {
            defaultEnv.add("constraint:org.hobbit.workergroup==system");
            containerType = Constants.CONTAINER_TYPE_SYSTEM;
        } else if (Constants.CONTAINER_TYPE_DATABASE.equals(containerType)
                && ((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType)
                        || Constants.CONTAINER_TYPE_DATABASE.equals(parentType))) {
            // defaultEnv.add("constraint:org.hobbit.workergroup==" +
            // Constants.CONTAINER_TYPE_DATABASE);
            defaultEnv.add("constraint:org.hobbit.type==data");
        } else if (Constants.CONTAINER_TYPE_BENCHMARK.equals(containerType)
                && ((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))) {
            defaultEnv.add("constraint:org.hobbit.workergroup==benchmark");
        } else {
            LOGGER.error(
                    "Got a request to create a container with type={} and parentType={}. Got no rule to determine its type. Returning null.",
                    containerType, parentType);
            return null;
        }

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
            logOptions.put("tag", LOGGING_TAG);
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
        serviceCfgBuilder.networks(NetworkAttachmentConfig.builder().target(HOBBIT_DOCKER_NETWORK).build());

        serviceCfgBuilder.name(containerName);
        ServiceSpec serviceCfg = serviceCfgBuilder.build();
        try {
            ServiceCreateResponse resp = dockerClient.createService(serviceCfg);
            String containerId = resp.id();
            // FIXME wait for any container of that service to start
            List<Task> serviceTasks;
            do {
                Thread.sleep(100);
                serviceTasks = dockerClient.listTasks(Task.Criteria.builder().serviceName(containerId).build());
            } while (serviceTasks.isEmpty() || newTaskStates.contains(serviceTasks.get(0).status().state()));
            containerId = serviceTasks.get(0).id();
            // return new container id
            return containerId;
        } catch (Exception e) {
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
        String containerId = createContainer(imageName, containerType, parentId, env, command);

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
    public void removeContainer(String containerId) {
        try {
            Task taskInfo = dockerClient.inspectTask(containerId);
            String serviceId = taskInfo.serviceId();

            // If we are not in testing mode, remove all containers. In testing
            // mode, remove only those that have a non-zero status
            Integer exitCode = taskInfo.status().containerStatus().exitCode();
            if ((!DEPLOY_ENV.equals(DEPLOY_ENV_TESTING)) || (exitCode == null) || (exitCode == 0)) {
                dockerClient.removeService(serviceId);

                // wait for service to disappear
                // FIXME SWARM
                try {
                    do {
                        Thread.sleep(100);
                        Object serviceInfo = dockerClient.inspectService(serviceId);

                        if (serviceInfo != null) {
                            while (serviceInfo != null) {
                                try {
                                    Thread.sleep(100);
                                    serviceInfo = dockerClient.inspectService(serviceId);
                                } catch (ServiceNotFoundException e) {
                                }
                                break;
                            }
                            /*
                            List<Task> serviceTasks = dockerClient.listTasks(Task.Criteria.builder().serviceName(serviceId).build());

                            if (serviceTasks.isEmpty()) {
                                LOGGER.info("Looking for service's tasks, no tasks found");
                            } else {
                                LOGGER.info("Looking for service's tasks, {} tasks found, state: {}",
                                        serviceTasks.size(), serviceTasks.get(0).status().state());
                            }
                            */
                        }
                    } while (true);
                } catch (ServiceNotFoundException e) {
                }
            } else {
                LOGGER.info("Will not remove container with id {} because its exitCode != 0 and testing mode is enabled", containerId);
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't remove container with id " + containerId + ".", e);
        }
    }

    @Deprecated
    @Override
    public void stopContainer(String containerId) {
        LOGGER.error("stopContainer() is deprecated! Will remove container instead");
        removeContainer(containerId);
    }

    @Override
    public void stopParentAndChildren(String parentId) {
        // stop parent
        stopContainer(parentId);

        // find children
        try {
            List<Container> containers = dockerClient.listContainers();
            for (Container c : containers) {
                if (c != null && c.labels().get(LABEL_PARENT) != null
                        && c.labels().get(LABEL_PARENT).equals(parentId)) {
                    stopParentAndChildren(c.id());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while stopping containers: " + e.toString());
        }
    }

    @Override
    public void removeParentAndChildren(String parentId) {
        // stop parent
        removeContainer(parentId);

        // find children
        try {
            String label = LABEL_PARENT + "=" + parentId;
            List<Task> containers = dockerClient.listTasks(Task.Criteria.builder().label(label).build());
            for (Task c : containers) {
                // FIXME SWARM no labels?
                if (c != null/* && c.labels().get(LABEL_PARENT) != null
                        && c.labels().get(LABEL_PARENT).equals(parentId)*/) {
                    removeParentAndChildren(c.id());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while removing containers: " + e.toString());
        }
    }

    @Override
    public Task getContainerInfo(String containerId) throws InterruptedException, DockerException {
        if (containerId == null) {
            return null;
        }
        Task info = null;
        try {
            info = dockerClient.inspectTask(containerId);
        } catch (TaskNotFoundException e) {
            // return null
        }
        return info;
    }

    @Override
    public List<Container> getContainers() {
        try {
            return dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public String getContainerId(String name) {
        try {
            List<Task> serviceTasks = dockerClient.listTasks(Task.Criteria.builder().taskName(name).build());
            if (!serviceTasks.isEmpty()) {
                return serviceTasks.get(0).id();
            }
        } catch (Exception e) {
            LOGGER.error("Could not get docker swarm task by name {}", name, e);
        }
        return null;
    }

    @Override
    public String getContainerName(String containerId) {
        Task response = null;
        try {
            response = getContainerInfo(containerId);
        } catch (Exception e) {
            LOGGER.error("Couldn't retrieve info of container {} to get the name", containerId, e);
        }
        String containerName = null;
        if (response != null) {
            try {
                containerName = dockerClient.inspectService(response.serviceId()).spec().name();
            } catch (Exception e) {
                LOGGER.error("Couldn't inspect docker service {} to get the name", containerId, e);
            }
        }
        return containerName;
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

}
