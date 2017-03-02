package org.hobbit.controller.docker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hobbit.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.AttachedNetwork;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.NetworkConfig;

/**
 * Created by Timofey Ermilov on 31/08/16
 */
public class ContainerManagerImpl implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);

    private static final String DEPLOY_ENV = System.getenv().containsKey("DEPLOY_ENV")
            ? System.getenv().get("DEPLOY_ENV") : "production";
    private static final String DEPLOY_ENV_TESTING = "testing";
    private static final Pattern PORT_PATTERN = Pattern.compile(":[0-9]+/");

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
     * Logging address.
     */
    public static final String LOGGING_GELF_ADDRESS = "udp://logstash:12201";
    /**
     * Logging pattern.
     */
    public static final String LOGGING_TAG = "{{.ImageName}}/{{.Name}}/{{.ID}}";

    /**
     * Docker client instance
     */
    private DockerClient dockerClient;

    private List<ContainerStateObserver> containerObservers = new ArrayList<>();

    /**
     * Constructor that creates new docker client instance
     */
    public ContainerManagerImpl() throws Exception {
        LOGGER.info("Deployed as \"{}\".", DEPLOY_ENV);
        dockerClient = DefaultDockerClient.fromEnv().build();
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
            dockerClient.pull(imageName);
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
    private void pullImage(String imageName) {
        if (!containsVersionTag(imageName)) {
            imageName += ":latest";
        }

        try {
            // pull image and wait for the pull to finish
            dockerClient.pull(imageName);
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
        // pull image
        pullImage(imageName);
        ContainerConfig.Builder cfgBuilder = ContainerConfig.builder();
        cfgBuilder.image(imageName);

        // generate unique container name
        String containerName = getInstanceName(imageName);
        cfgBuilder.hostname(containerName);
        // get parent info
        List<String> defaultEnv = new ArrayList<>();
        defaultEnv.add(Constants.CONTAINER_NAME_KEY + "=" + containerName);
        ContainerInfo parent = getContainerInfo(parentId);
        String parentType = (parent == null) ? null : parent.config().labels().get(LABEL_TYPE);
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
        cfgBuilder.labels(labels);
        // create logging info
        if (!DEPLOY_ENV.equals(DEPLOY_ENV_TESTING)) {
            // Map<String, String> logOptions = new HashMap<String, String>();
            // logOptions.put("gelf-address", LOGGING_GELF_ADDRESS);
            // logOptions.put("tag", LOGGING_TAG);
            // cfgBuilder.hostConfig(HostConfig.builder().logConfig(LogConfig.create("gelf",
            // logOptions)).build());
        }

        // if command is present - execute it
        if ((command != null) && (command.length > 0)) {
            cfgBuilder.cmd(command);
        }

        // trigger creation
        ContainerConfig cfg = cfgBuilder.build();
        try {
            ContainerCreation resp = dockerClient.createContainer(cfg, containerName);
            String containerId = resp.id();
            // disconnect the container from every network it might be connected
            // to
            ContainerInfo info = getContainerInfo(containerId);
            Map<String, AttachedNetwork> networks = info.networkSettings().networks();
            for (String networkName : networks.keySet()) {
                dockerClient.disconnectFromNetwork(containerId, networkName);
            }
            // connect to hobbit network
            dockerClient.connectToNetwork(resp.id(), HOBBIT_DOCKER_NETWORK);
            // return new container id
            return containerId;
        } catch (Exception e) {
            LOGGER.error("Couldn't create Docker container. Returning null.", e);
            return null;
        }
    }

    public String startContainer(String imageName) {
        return startContainer(imageName, null, "", null);
    }

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
            try {
                dockerClient.startContainer(containerId);
            } catch (Exception e) {
                LOGGER.error("Couldn't start container " + containerId + ". Returning null.", e);
                return null;
            }
            return containerId;
        }
        return null;
    }

    @Override
    public void removeContainer(String containerId) {
        try {
            // If we are not in testing mode, remove all containers. In testing
            // mode, remove only those that have a non-zero status
            if ((!DEPLOY_ENV.equals(DEPLOY_ENV_TESTING)) || (getContainerInfo(containerId).state().exitCode() == 0)) {
                dockerClient.removeContainer(containerId);
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't remove container with id " + containerId + ".", e);
        }
    }

    @Override
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainer(containerId, 5);
        } catch (DockerException e) {
            // nothing to do
            LOGGER.info("Couldn't stop container with id " + containerId + ". " + e.toString());
        } catch (Exception e) {
            LOGGER.error("Couldn't stop container with id " + containerId + ".", e);
        }
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
            List<Container> containers = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
            for (Container c : containers) {
                if (c != null && c.labels().get(LABEL_PARENT) != null
                        && c.labels().get(LABEL_PARENT).equals(parentId)) {
                    removeParentAndChildren(c.id());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while removing containers: " + e.toString());
        }
    }

    @Override
    public ContainerInfo getContainerInfo(String containerId) {
        try {
            return dockerClient.inspectContainer(containerId);
        } catch (Exception e) {
            return null;
        }
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
        String extName = "/" + name;
        List<Container> containers = getContainers();
        for (Container container : containers) {
            for (String containerName : container.names()) {
                // Clean name from Swarm node names if needed
                String cleanedName = containerName.replaceAll("/.+?/", "/");
                if (cleanedName.equals(name) || cleanedName.equals(extName)) {
                    return container.id();
                }
            }
        }
        return null;
    }

    @Override
    public String getContainerName(String containerId) {
        ContainerInfo response = getContainerInfo(containerId);
        String containerName = null;
        if (response != null) {
            containerName = response.name();
            if (containerName.startsWith("/")) {
                containerName = containerName.substring(1);
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
