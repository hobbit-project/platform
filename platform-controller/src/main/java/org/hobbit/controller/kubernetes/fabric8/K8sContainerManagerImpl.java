package org.hobbit.controller.kubernetes.fabric8;


import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ServiceNotFoundException;
import com.spotify.docker.client.exceptions.TaskNotFoundException;
import com.spotify.docker.client.messages.RegistryAuth;
import com.spotify.docker.client.messages.ServiceCreateResponse;
import com.spotify.docker.client.messages.swarm.*;
import com.spotify.docker.client.messages.swarm.ServiceSpec;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.hobbit.controller.docker.ContainerStateObserver;
import org.hobbit.controller.gitlab.GitlabControllerImpl;
import org.hobbit.controller.orchestration.ClusterManager;
import org.hobbit.controller.orchestration.ContainerManager;
import org.hobbit.controller.utils.Waiting;
import org.hobbit.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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



    private String createContainer(String imageName, String containerType, String parentId, String[] env, String[] netAliases, String[] command) {
        /*
        com.spotify.docker.client.messages.swarm.ServiceSpec.Builder serviceCfgBuilder = com.spotify.docker.client.messages.swarm.ServiceSpec.builder();

        TaskSpec.Builder taskCfgBuilder = TaskSpec.builder();
        // we need to run it just once; configure to never restart
        taskCfgBuilder.restartPolicy(RestartPolicy.builder().condition(RestartPolicy.RESTART_POLICY_NONE).build());

        ContainerSpec.Builder cfgBuilder = ContainerSpec.builder();
        cfgBuilder.image(imageName);

        */
        DeploymentSpecBuilder serviceCfgBuilder = new DeploymentSpecBuilder();

        // generate unique container name
        String serviceName = getInstanceName(imageName);
        serviceCfgBuilder.editOrNewTemplate().editOrNewSpec().addNewContainer().withNewName(serviceName);
        // cfgBuilder.hostname(serviceName);
        // get parent info
        EnvVar envContainer = new EnvVarBuilder().withNewName(Constants.CONTAINER_NAME_KEY).withNewValue(serviceName).build();
        List<EnvVar> defaultEnv = new ArrayList<>();
        defaultEnv.add(envContainer);

        Deployment parent = getContainerInfo(parentId);
        String parentType = (parent == null) ? null : parent.getMetadata().getLabels().get(LABEL_TYPE);
        // If there is no container type try to use the parent type or return null
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

        ClusterManager clusterManager = new K8sClusterManagerImpl();
        numberOfSwarmNodes = clusterManager.getNumberOfNodes();
        numberOfSystemSwarmNodes = clusterManager.getNumberOfNodes("org.hobbit.workergroup=system");
        numberOfBenchmarkSwarmNodes = clusterManager.getNumberOfNodes("org.hobbit.workergroup=benchmark");

        if (numberOfSwarmNodes > 1) {
            // If the parent has "system" --> we do not care what the container
            // would like to have OR if there is no parent or the parent is a
            // benchmark (in case of the benchmark controller) and the container has
            // type "system"
            if ((((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))
                && Constants.CONTAINER_TYPE_SYSTEM.equals(containerType))
                || Constants.CONTAINER_TYPE_SYSTEM.equals(parentType)) {

                serviceCfgBuilder.editOrNewTemplate().editOrNewSpec().addToNodeSelector("org.hobbit.workergroup", "system");
                containerType = Constants.CONTAINER_TYPE_SYSTEM;
            } else if (Constants.CONTAINER_TYPE_DATABASE.equals(containerType)
                && ((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType)
                || Constants.CONTAINER_TYPE_DATABASE.equals(parentType))) {
                serviceCfgBuilder.editOrNewTemplate().editOrNewSpec().addToNodeSelector("org.hobbit.workergroup", "benchmark");
            } else if (Constants.CONTAINER_TYPE_BENCHMARK.equals(containerType)
                && ((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))) {
                serviceCfgBuilder.editOrNewTemplate().editOrNewSpec().addToNodeSelector("org.hobbit.workergroup", "benchmark");
            } else {
                LOGGER.error("Got a request to create a container with type={} and parentType={}. "
                    + "Got no rule to determine its type. Returning null.", containerType, parentType);
                return null;
            }
        } else {
            LOGGER.warn("The swarm cluster got only 1 node, I will not use placement constraints.");
        }

        // add hardware information to environment
        EnvVar envNodes = new EnvVarBuilder().withNewName(Constants.HARDWARE_NUMBER_OF_NODES_KEY).withNewValue(Long.toString(numberOfSwarmNodes)).build();
        defaultEnv.add(envNodes);
        EnvVar envSystem = new EnvVarBuilder().withNewName(Constants.HARDWARE_NUMBER_OF_SYSTEM_NODES_KEY).withNewValue(Long.toString(numberOfSystemSwarmNodes)).build();
        defaultEnv.add(envSystem);
        EnvVar envBenchmark = new EnvVarBuilder().withNewName(Constants.HARDWARE_NUMBER_OF_BENCHMARK_NODES_KEY).withNewValue(Long.toString(numberOfBenchmarkSwarmNodes)).build();
        defaultEnv.add(envBenchmark);


        // create env vars to pass
        if (env != null) {
            for(String en : env){
                String[] en_key_val = en.split("=");
                EnvVar envVar = new EnvVarBuilder().withNewName(en_key_val[0]).withNewValue(en_key_val[1]).build();
                defaultEnv.add(envVar);
            }

        }
        serviceCfgBuilder.editOrNewTemplate().editOrNewSpec().editFirstContainer().addAllToEnv(defaultEnv);
        // create container labels
        Map<String, String> labels = new HashMap<>();
        labels.put(LABEL_TYPE, containerType);
        if (parentId != null) {
            labels.put(LABEL_PARENT, parentId);
        }
        serviceCfgBuilder.editOrNewTemplate().editOrNewMetadata().addToLabels(labels);
        // create logging info
        if (gelfAddress != null) {
            Map<String, String> logOptions = new HashMap<String, String>();
            logOptions.put("gelf-address", gelfAddress);
            String tag = LOGGING_TAG;
            if (experimentId != null) {
                tag = containerType + LOGGING_SEPARATOR + experimentId + LOGGING_SEPARATOR + LOGGING_TAG;
            }
            logOptions.put("tag", tag);
            // Not fully understood
            // taskCfgBuilder.logDriver(Driver.builder().name(LOGGING_DRIVER_GELF).options(logOptions).build());
        }
        // if command is present - execute it
        if ((command != null) && (command.length > 0)) {
            for (String cmd : command){
                serviceCfgBuilder.editOrNewTemplate().editOrNewSpec().editFirstContainer().addNewCommand(cmd);
            }
        }
        // trigger creation
        Deployment deployment = new DeploymentBuilder().withNewMetadata()
                                                            .withName(serviceName)
                                                        .endMetadata()
                                                        .withSpec(serviceCfgBuilder.build()).build();

        // connect to hobbit network only
        // here I should set the port for the cluster service. what port will be used?
        /*
        serviceCfgBuilder.networks(
            NetworkAttachmentConfig.builder().target(HOBBIT_DOCKER_NETWORK).aliases(netAliases).build()
        );
        *
        */
        try{
            final CountDownLatch closeLatch = new CountDownLatch(1);
            k8sClient.apps().deployments().inNamespace("default").create(deployment);
            Watch watch = k8sClient.apps().deployments().inNamespace("default").withName(serviceName).watch(new Watcher<Deployment>() {
                @Override
                public void eventReceived(Action action, Deployment resource) {
                    if (action.name().equals("ADDED")){
                        LOGGER.info("Container {} created", serviceName);
                    }
                    else if (action.name().equals("ERROR")){
                        LOGGER.error("Error creating container {}", serviceName);
                    }
                }
                @Override
                public void onClose(KubernetesClientException cause) {
                    LOGGER.error(cause.getMessage(), cause);
                    closeLatch.countDown();
                }
            });
            closeLatch.await(10, TimeUnit.SECONDS);
            return serviceName;
        }catch (InterruptedException e){
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] netAliases, String[] command, String experimentId) {
        this.experimentId = experimentId;
        return startContainer(imageName, containerType, parentId, env, netAliases, command);
    }

    @Override
    public void stopContainer(String containerId) {
        removeContainer(containerId);
    }

    @Override
    public void removeContainer(String serviceName) {
        try {
            Integer exitCode = getContainerExitCode(serviceName);
            if (DEPLOY_ENV.equals(DEPLOY_ENV_DEVELOP)) {
                LOGGER.info(
                    "Will not remove container {}. "
                        + "Development mode is enabled.",
                    serviceName);
            } else if (DEPLOY_ENV.equals(DEPLOY_ENV_TESTING) && (exitCode == null || exitCode != 0)) {
                // In testing - do not remove containers if they returned non-zero exit code
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

    @Override
    public void stopParentAndChildren(String parentId) {

    }

    @Override
    public void removeParentAndChildren(String parent) {

    }

    @Override
    public Integer getContainerExitCode(String serviceName) {
        if (getContainerInfo(serviceName) == null) {
            LOGGER.warn("Couldn't get the exit code for container {}. Service doesn't exist. Assuming it was stopped by the platform.", serviceName);
            return DOCKER_EXITCODE_SIGKILL;
        }
        // Service exists, but no tasks are observed.
        Integer exitCode = null;

        Integer runningReplicas = k8sClient.apps().deployments().inNamespace("default")
                    .withName(serviceName).get().getStatus().getAvailableReplicas();

        if (runningReplicas == 1) {
            LOGGER.warn("Couldn't get the exit code for container {}. Service has no tasks. Returning null.", serviceName);
            return null;
        }

        return null;
    }

    @Override
    public Deployment getContainerInfo(String deploymentName) {
        if (deploymentName == null) return null;
        Deployment info = null;
        info = k8sClient.apps().deployments().inNamespace("default").withName(deploymentName).get();
        return info;
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


