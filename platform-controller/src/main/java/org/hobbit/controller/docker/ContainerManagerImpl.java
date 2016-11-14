package org.hobbit.controller.docker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.hobbit.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;

/**
 * Created by Timofey Ermilov on 31/08/16
 */
public class ContainerManagerImpl implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);

    private static final String DEPLOY_ENV = System.getProperty("DEPLOY_ENV", "production");
    private static final String DEPLOY_ENV_TESTING = "testing";

    /**
     * Label value that denotes container type as "system"
     */
    public static final String TYPE_SYSTEM = "system";
    /**
     * Label value that denotes container type as "benchmark"
     */
    public static final String TYPE_BENCHMARK = "benchmark";
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

    private List<ContainerStateObserver> containerObservers = new ArrayList<ContainerStateObserver>();

    /**
     * Constructor that creates new docker client instance
     */
    public ContainerManagerImpl() {
        dockerClient = DockerClientBuilder.getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder().build())
                .build();
        // try to find hobbit network in existing ones
        List<Network> networks = dockerClient.listNetworksCmd().exec();
        String hobbitNetwork = null;
        for (Network net : networks) {
            if (net.getName().equals(HOBBIT_DOCKER_NETWORK)) {
                hobbitNetwork = net.getId();
                break;
            }
        }
        // if not found - create new one
        if (hobbitNetwork == null) {
            // CreateNetworkResponse network =
            dockerClient.createNetworkCmd().withName(HOBBIT_DOCKER_NETWORK)
                    // TODO: figure out why it doesn't work with swarm and
                    // overlay networks
                    // .withDriver("overlay")
                    .exec();
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
        final String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        StringBuilder builder = new StringBuilder(imageName.length() + uuid.length() + 1);
        builder.append(imageName.replaceAll("/", "_"));
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
    private void pullImageIfNeeded(String imageName) {
        // check if image contains tag, if not - add ":latest"
        if (!imageName.contains(":")) {
            imageName += ":latest";
        }

        // check if image is already available
        List<Image> images = dockerClient.listImagesCmd().exec();
        for (Image image : images) {
            for (String tag : image.getRepoTags()) {
                if (tag.equals(imageName)) {
                    return;
                }
            }
        }

        // pull image and wait for the pull to finish
        dockerClient.pullImageCmd(imageName).exec(new PullImageResultCallback()).awaitSuccess();
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
        // pull image if needed
        pullImageIfNeeded(imageName);
        CreateContainerCmd cmd = dockerClient.createContainerCmd(imageName);

        // generate unique container name
        String containerName = getInstanceName(imageName);
        cmd.withName(containerName);
        // create env vars to pass
        if (env != null) {
            cmd.withEnv(ArrayUtils.add(env, Constants.CONTAINER_NAME_KEY + "=" + containerName));
        } else {
            cmd.withEnv(new String[] { Constants.CONTAINER_NAME_KEY + "=" + containerName });
        }
        // create container labels
        Map<String, String> labels = new HashMap<>();
        labels.put(LABEL_TYPE, containerType);
        labels.put(LABEL_PARENT, parentId);
        cmd.withLabels(labels);
        // create logging info
        if (!DEPLOY_ENV.equals(DEPLOY_ENV_TESTING)) {
            // Map<String, String> logOptions = new HashMap<String, String>();
            // logOptions.put("gelf-address", LOGGING_GELF_ADDRESS);
            // logOptions.put("tag", LOGGING_TAG);
            // cmd.withLogConfig(new LogConfig(LoggingType.GELF, logOptions));
        }

        cmd.withNetworkMode(HOBBIT_DOCKER_NETWORK);

        // if command is present - execute it
        if ((command != null) && (command.length > 0)) {
            cmd.withCmd(command);
        }

        // trigger creation
        CreateContainerResponse resp = null;
        try {
            resp = cmd.exec();
        } catch (Exception e) {
            LOGGER.error("Couldn't create Docker container. Returning null.", e);
            return null;
        }

        return resp.getId();
    }

    public String startContainer(String imageName) {
        return startContainer(imageName, TYPE_BENCHMARK, "", null);
    }

    public String startContainer(String imageName, String[] command) {
        return startContainer(imageName, TYPE_BENCHMARK, "", command);
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
                dockerClient.startContainerCmd(containerId).exec();
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
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (Exception e) {
            LOGGER.error("Couldn't remove container with id " + containerId + ".", e);
        }
    }

    @Override
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
        } catch (NotModifiedException e) {
            // nothing to do
            // LOGGER.info(
            // "Couldn't stop container with id " + containerId + ". Most
            // probably it has already been stopped.");
        } catch (Exception e) {
            LOGGER.error("Couldn't stop container with id " + containerId + ".", e);
        }
    }

    @Override
    public void stopParentAndChildren(String parentId) {
        // stop parent
        stopContainer(parentId);

        // find children
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container c : containers) {
            if (c != null && c.getLabels().get(LABEL_PARENT) != null
                    && c.getLabels().get(LABEL_PARENT).equals(parentId)) {
                stopParentAndChildren(c.getId());
            }
        }
    }

    @Override
    public void removeParentAndChildren(String parentId) {
        // stop parent
        removeContainer(parentId);

        // find children
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container c : containers) {
            if (c != null && c.getLabels().get(LABEL_PARENT) != null
                    && c.getLabels().get(LABEL_PARENT).equals(parentId)) {
                removeParentAndChildren(c.getId());
            }
        }
    }

    @Override
    public InspectContainerResponse getContainerInfo(String containerId) {
        return dockerClient.inspectContainerCmd(containerId).exec();
    }

    @Override
    public List<Container> getContainers() {
        return dockerClient.listContainersCmd().withShowAll(true).exec();
    }

    @Override
    public String getContainerId(String name) {
        String extName = "/" + name;
        List<Container> containers = getContainers();
        for (Container container : containers) {
            for (String containerName : container.getNames()) {
                if (containerName.equals(name) || containerName.equals(extName)) {
                    return container.getId();
                }
            }
        }
        return null;
    }

    @Override
    public String getContainerName(String containerId) {
        InspectContainerResponse response = getContainerInfo(containerId);
        String containerName = null;
        if (response != null) {
            containerName = response.getName();
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

}
