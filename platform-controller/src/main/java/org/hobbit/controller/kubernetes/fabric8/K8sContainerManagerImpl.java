package org.hobbit.controller.kubernetes.fabric8;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.NetworkConfig;
import com.spotify.docker.client.messages.RegistryAuth;
import com.spotify.docker.client.messages.swarm.TaskStatus;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.hobbit.controller.docker.ContainerManagerImpl;
import org.hobbit.controller.docker.ContainerStateObserver;
import org.hobbit.controller.docker.DockerUtility;
import org.hobbit.controller.gitlab.GitlabControllerImpl;
import org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources.*;
import org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources.Config;
import org.hobbit.controller.orchestration.ContainerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class K8sContainerManagerImpl implements ContainerManager<Deployment, PodMetrics> {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8sContainerManagerImpl.class);

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
    private KubernetesClient k8sClient;
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

    public K8sContainerManagerImpl() {
        LOGGER.info("Deployed as \"{}\".", DEPLOY_ENV);
        this.k8sClient = K8sUtility.getK8sClient();

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
        ServiceList networks = k8sClient.services().inAnyNamespace().list();
        String hobbitNetwork = null;

        for (Service net: networks.getItems()){
            if (net.getMetadata().getName().equals(HOBBIT_DOCKER_NETWORK)){
                hobbitNetwork = net.getMetadata().getUid();
                break;
            }
        }
        // if not found - create new one
        if (hobbitNetwork == null) {
            LOGGER.warn("Could not find hobbit kubernetes ClusterIP network service, creating a new one");
            Service hobbit = new ServiceBuilder()
                .withNewMetadata()
                    .withName(HOBBIT_DOCKER_NETWORK)
                .endMetadata()
                .withNewSpec()
                .withSelector(Collections.singletonMap("app", "MyApp"))
                .addNewPort()
                .withName("test-port")
                .withProtocol("TCP")
                .withPort(80)
                .withTargetPort(new IntOrString(9376))
                .endPort()
                .endSpec()
                .build();
        }
    }

    @Override
    public String startContainer(String imageName) {
        return startContainer(imageName, null, "", null);
    }

    @Override
    public String startContainer(String imageName, String[] command) {
        return startContainer(imageName, null, "", command);
    }

    @Override
    public String startContainer(String imageName, String type, String parent) {
        return startContainer(imageName, type, parent, null);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] command) {
        return startContainer(imageName, containerType, parentId, null, command);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] command) {
        return startContainer(imageName, containerType, parentId, env, null, command);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] netAliases, String[] command) {
        return startContainer(imageName, containerType, parentId, env, netAliases, command, true);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] command, boolean pullImage) {
        return startContainer(imageName, containerType, parentId, env, null, command, true);
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] netAliases, String[] command, boolean pullImage) {
        return null;
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] netAliases, String[] command, String experimentId) {
        return null;
    }

    @Override
    public void stopContainer(String containerId) {

    }

    @Override
    public void removeContainer(String serviceName) {

    }

    @Override
    public void stopParentAndChildren(String parentId) {

    }

    @Override
    public void removeParentAndChildren(String parent) {

    }

    @Override
    public Integer getContainerExitCode(String serviceName) {
        return null;
    }

    @Override
    public Deployment getContainerInfo(String serviceName) {
        return null;
    }

    @Override
    public List<Deployment> getContainers(String parent) {
        return null;
    }

    @Override
    public String getContainerId(String name) {
        return null;
    }

    @Override
    public String getContainerName(String containerId) {
        return null;
    }

    @Override
    public void addContainerObserver(ContainerStateObserver containerObserver) {

    }

    @Override
    public void pullImage(String imageName) {

    }

    @Override
    public PodMetrics getStats(String containerId) {
        return null;
    }

    private String getInstanceName(String imageName) {
        return getInstanceName(imageName, "");
    }

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





}
