package org.hobbit.controller.containers.kubernetes;

import org.hobbit.controller.containers.ContainerManager;
import org.hobbit.controller.containers.ContainerPodException;
import org.hobbit.controller.containers.ContainerStateObserver;
import org.hobbit.controller.containers.KubExtendedContainerManager;
import org.hobbit.controller.data.ContainerCriteria;
import org.hobbit.controller.utils.Waiting;
import org.hobbit.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContainerManagerImpl implements ContainerManager, KubExtendedContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);
    private static final String DEFAULT_GIT_USERNAME = "gitadmin";
    private static final String DEFAULT_GIT_EMAIL = "gitadmin@project-hobbit.eu";
    private static final String RUN_ON_KUBERNETES_FLAG = "kubernetes";
    private ApiClient client;
    //TODO do we need to make config able ?
    private String nameSpace = "default";
    private static final long KUBERNETES_POLL_INTERVAL = 5000; // Poll interval in ms

    /**
     * Logging separator for type/experiment id.
     */
    private static final String LOGGING_SEPARATOR = "_sep_";
    //public static final String LOGGING_TAG = "{{.ImageName}}/{{.Name}}/{{.ID}}";
    public static final String DEPLOY_ENV_KEY = "DEPLOY_ENV";
    private static final String DEPLOY_ENV_DEVELOP = "develop";
    private static final String DEPLOY_ENV_TESTING = "testing";
    private static final String DEPLOY_ENV = System.getenv().containsKey(DEPLOY_ENV_KEY)
        ? System.getenv().get(DEPLOY_ENV_KEY)
        : "production";
    private static final long TIMEOUT_SECONDS = 60; // Adjust the timeout as needed
    private static final int MAX_POD_NAME_LENGTH = 63;
    private static final long POD_PHASE_CHECK_INTERVAL = 2000; // in ms
    private static final Pattern VALID_POD_NAME_REGEX = Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$");


    /**
     * Observers that should be notified if a container terminates.
     */
    private final List<ContainerStateObserver> containerObservers = new ArrayList<>();


    public ContainerManagerImpl(ApiClient client) {
        LOGGER.info("Initializing Kubernetes client");
            this.client = client;
    }



    @Deprecated
    public String startContainer(String imageName) {
        return null;
    }

    @Deprecated
    public String startContainer(String imageName, String[] command){
        return null;
    }

    @Override
    public String startContainer(String imageName, String type, String parentId) {
        return startContainer(imageName, type, parentId, null);
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
        return startContainer(imageName, containerType, parentId, env, null, command, true,
            Collections.emptyMap());
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] command, boolean pullImage) {
        return startContainer(imageName, containerType, parentId, env, null, command, true, Collections.emptyMap());
    }

    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] netAliases, String[] command, boolean pullImage, Map<String, Object> constraints) {
        //in Interface there is no experimentID , keep it for backward compatibility
        return startContainer(imageName, containerType, parentId, env, null,  command, "", constraints);
    }

    public String generatePodName(String moduleIri,String containerType) {
        /*
         * MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
         * messageDigest.update(moduleIri.getBytes()); String stringHash = new
         * String(messageDigest.digest());
         */
        if(containerType==null){
            return "notype" + (moduleIri.hashCode() * 31 + (int) (System.currentTimeMillis()));
        }
        return containerType + (moduleIri.hashCode() * 31 + (int) (System.currentTimeMillis()));

    }

    /**
     * Shortens and validates a pod name to ensure it meets Kubernetes naming conventions.
     *
     * @param podName The original pod name.
     * @return The shortened and validated pod name.
     * @throws IllegalArgumentException If the pod name is null, empty, or cannot be sanitized.
     */
    public static String shortenAndValidatePodName(String podName) {
        if (podName == null || podName.isEmpty()) {
            throw new IllegalArgumentException("Pod name cannot be null or empty.");
        }

        String shortenedName = podName;

        // Shorten the name if it exceeds the maximum allowed length
        if (podName.length() > MAX_POD_NAME_LENGTH) {
            // Shorten the name, prioritizing the end (more unique).
            int excessLength = podName.length() - MAX_POD_NAME_LENGTH;
            int charsToKeep = podName.length() - excessLength;
            shortenedName = podName.substring(0, charsToKeep);

            //Ensure it ends with alphanumeric
            while (shortenedName.length() > 0 && !Character.isLetterOrDigit(shortenedName.charAt(shortenedName.length()-1))) {
                shortenedName = shortenedName.substring(0, shortenedName.length()-1);
            }
        }

        //Validate the shortened name
        Matcher matcher = VALID_POD_NAME_REGEX.matcher(shortenedName);
        if (!matcher.matches()) {
            //Handle invalid characters (replace with hyphens or remove)
            shortenedName = shortenedName.replaceAll("[^a-z0-9-]", "-");
            // Ensure starts and ends with alphanumeric after sanitization
            while (shortenedName.length() > 0 && !Character.isLetterOrDigit(shortenedName.charAt(0))) {
                shortenedName = shortenedName.substring(1);
            }
            while (shortenedName.length() > 0 && !Character.isLetterOrDigit(shortenedName.charAt(shortenedName.length()-1))) {
                shortenedName = shortenedName.substring(0, shortenedName.length()-1);
            }
            //Re-check length after sanitization
            if (shortenedName.length() > MAX_POD_NAME_LENGTH) {
                shortenedName = shortenedName.substring(0, MAX_POD_NAME_LENGTH);
            }
            matcher = VALID_POD_NAME_REGEX.matcher(shortenedName);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Pod name could not be sanitized to comply with naming conventions.");
            }
        }

        return shortenedName;
    }

    // we do not use netAliases
    @Override
    public String startContainer(String imageName, String containerType, String parentId, String[] env, String[] netAliases, String[] command, String experimentId, Map<String, Object> constraints) {
        String podName = generatePodName(imageName,containerType);
        if (experimentId != null) {
            podName =  experimentId + LOGGING_SEPARATOR + generatePodName(imageName,containerType);
        }

        podName = shortenAndValidatePodName(podName);

        LOGGER.debug("start Container podname is {}", podName);

        if (imageName != null) {
            LOGGER.debug("image name is {}", imageName);
        } else {
            LOGGER.debug("image name is null");
        }

        if (containerType != null) {
            LOGGER.debug("container type is {}", containerType);
        } else {
            LOGGER.debug("container type is null");
        }
        if (parentId != null) {
            if (!parentId.contains(".pod.cluster.")) {
            LOGGER.debug("wrong parent ID {}",parentId);
                //parentId = getContainerPodName(parentId);
            LOGGER.debug("new parent ID is {}",parentId);
            }
        }

        if (parentId != null) {
            LOGGER.debug("Parent ID is {}", parentId);
            //todo just as a patch, the parent id somewhere insert wrong and as container id
        } else {
            LOGGER.debug("Parent ID is null");
        }

        if (env != null && env.length > 0) {
            LOGGER.debug("env is {}", String.join(",", env));
        } else {
            LOGGER.debug("env is null or empty");
        }

        if (command != null && command.length > 0) {
            LOGGER.debug("command is {}", String.join(",", command));
        } else {
            LOGGER.debug("command is null or empty");
        }

        if (experimentId != null) {
            LOGGER.debug("experimentID is {}", experimentId);
        } else {
            LOGGER.debug("experimentID is null");
        }

        return createContainerKub(imageName, podName, containerType, parentId, env, command, constraints);
    }

    /**
     * Retrieves the IPv4 address of the pod.
     *
     * This method iterates through the network interfaces to find an active, non-loopback interface
     * and returns the first available IPv4 address it finds.
     *
     * @return The IPv4 address of the pod, or "Unknown-IP" if no valid IP is found.
     * @throws RuntimeException if no valid IPv4 address is found.
     */
    public static String getPodIP() {
        try {
            // Get the local host's network interfaces
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Ignore loopback and inactive interfaces
                if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();

                        // Ignore IPv6 addresses and return the first available IPv4 address found
                        if (address instanceof Inet4Address) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            LOGGER.error("getPodIP failed", e);
            throw new RuntimeException("Error retrieving pod IP address", e);
        }

        // Log and throw an exception if no valid IP address is found
        LOGGER.error("No valid IPv4 address found for the pod");
        throw new RuntimeException("No valid IPv4 address found for the pod");
    }

    // this is kind of the patch can remove after ckan image fetch without docker.io
    private String completeImageName(String imageName) {
        if ("dicegroup/ckan-hobbit-db".equals(imageName)) {
            return "docker.io/dicegroup/ckan-hobbit-db:latest";
        }
        return imageName;
    }

    /**
     * Prepares and updates the environment variables array by adding a new variable representing the current pod's DNS ID.
     *
     * @param env The original array of environment variables. If {@code null} or empty, a new array will be created.
     * @return A new array of environment variables with the additional variable for the current pod's DNS ID.
     */
    private String[] prepareEnvironmentVariables(String[] env) {
        String thisPodName = Constants.CONTAINER_NAME_KEY + "=" + convertIP2dnsId(getPodIP());
        if (env == null || env.length == 0) {
            return new String[]{thisPodName};
        }
        String[] updatedEnv = Arrays.copyOf(env, env.length + 1);
        updatedEnv[env.length] = thisPodName;
        return updatedEnv;
    }

    /**
     * Converts all container names in the environment variables array to their corresponding DNS IDs.
     *
     * @param env The array of environment variables to be converted.
     * @return The updated array of environment variables with converted container names.
     */
    private String[] convertAllNamesInENV(String[] env) {
        for (int i = 0; i < env.length; i++) {
            env[i] = changeIfItIsContainerName(env[i]);
        }
        return env;
    }

    /**
     * Parses an array of environment variables in the format "key=value" into a list of {@code V1EnvVar} objects.
     *
     * @param env The array of environment variables to be parsed.
     * @return A list of {@code V1EnvVar} objects representing the parsed environment variables.
     */
    private List<V1EnvVar> parseEnvironmentVariables(String[] env) {
        List<V1EnvVar> environmentVariables = new ArrayList<>();
        for (String envVar : env) {
            String[] parts = envVar.split("=", 2);
            if (parts.length == 2) {
                environmentVariables.add(new V1EnvVar().name(parts[0]).value(parts[1]));
            } else {
                LOGGER.warn("Skipping invalid environment variable: {}", envVar);
            }
        }
        return environmentVariables;
    }

    /**
     * Adds default environment variables to the given list of {@code V1EnvVar} objects.
     *
     * @param environmentVariables The list of environment variables to which the default variables will be added.
     */
    private void addDefaultEnvironmentVariables(List<V1EnvVar> environmentVariables) {
        environmentVariables.add(new V1EnvVar().name("GITLAB_USER").value(DEFAULT_GIT_USERNAME));
        environmentVariables.add(new V1EnvVar().name("GITLAB_EMAIL").value(DEFAULT_GIT_EMAIL));
        environmentVariables.add(new V1EnvVar().name("RUN_ON").value(RUN_ON_KUBERNETES_FLAG));
    }

    /**
     * Determines the type of the container based on the provided container type or the type of its parent pod.
     *
     * @param containerType The type of the container. If {@code null} or empty, it will be resolved from the parent pod.
     * @param parentPodName The name of the parent pod. If {@code null}, no parent resolution will be attempted.
     * @return The determined type of the container, or {@code null} if the type cannot be resolved.
     */
    private String determineContainerType(String containerType, String parentPodName) {
        String parentType = parentPodName != null ? getContainerType(parentPodName) : null;
        if (containerType == null || containerType.isEmpty()) {
            if (parentType == null) {
                LOGGER.error("Unable to resolve parent container type. Returning null.");
                return null;
            }
            containerType = parentType;
        }
        return containerType;
    }

    /**
     * Creates a security context for a container based on the provided image name.
     *
     * @param imageName The name of the Docker image for the container.
     * @return A {@code V1SecurityContext} object configured with appropriate security settings. If the image name matches
     *         "earthquakesan/ckan-solr:2.8.0", a less privileged security context is returned.
     */
    private V1SecurityContext createSecurityContext(String imageName) {
        V1SecurityContext securityContext = new V1SecurityContext();
        if (!imageName.contains("earthquakesan/ckan-solr:2.8.0")) {
            securityContext.setCapabilities(new V1Capabilities()
                .addAddItem("NET_RAW").addAddItem("SYS_ADMIN").addAddItem("AUDIT_WRITE"));
            securityContext.setRunAsUser(0L);
            securityContext.setRunAsGroup(0L);
            securityContext.allowPrivilegeEscalation(true);
        } else {
            LOGGER.debug("do not use as admin");
        }
        return securityContext;
    }

    /**
     * Creates a container specification for a Kubernetes pod based on the provided parameters.
     *
     * @param podName The name of the pod.
     * @param imageName The Docker image name for the container.
     * @param environmentVariables A list of {@code V1EnvVar} objects representing the environment variables for the container.
     * @param command An array of strings representing the command to run in the container. If {@code null}, no command is set.
     * @param constraints A map of resource constraints that will be used to create resource requirements for the container.
     * @return A {@code V1Container} object configured with the provided specifications.
     */
    private V1Container createContainerSpec(String podName, String imageName, List<V1EnvVar> environmentVariables,
                                            String[] command, Map<String, Object> constraints) {
        V1ResourceRequirements resourceRequirements = createResourceRequirementsFromConstraints(constraints);
        return new V1Container()
            .name(podName)
            .image(imageName)
            .env(environmentVariables)
            .command(command != null ? Arrays.asList(command) : null)
            .resources(resourceRequirements)
            .securityContext(createSecurityContext(imageName));
    }

    /**
     * Applies a node selector to the given pod specification based on the container type and its parent type.
     *
     * @param podSpec The {@code V1PodSpec} object to which the node selector will be applied.
     * @param containerType The type of the container. This determines the appropriate node group for the pod.
     * @param parentType The type of the parent container or pod. If {@code null}, it is assumed there is no parent.
     */
    private void applyNodeSelector(V1PodSpec podSpec, String containerType, String parentType) {
        if ((((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))
            && Constants.CONTAINER_TYPE_SYSTEM.equals(containerType))
            || Constants.CONTAINER_TYPE_SYSTEM.equals(parentType)) {

            LOGGER.debug("Setting node selector to 'system-nodes' for container type: {}", containerType);
            podSpec.nodeSelector(Collections.singletonMap("node-group", "system-nodes"));

        } else if (Constants.CONTAINER_TYPE_DATABASE.equals(containerType)
            && ((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType)
            || Constants.CONTAINER_TYPE_DATABASE.equals(parentType))) {

            LOGGER.debug("Setting node selector to 'benchmark-nodes' for DATABASE container type.");
            podSpec.nodeSelector(Collections.singletonMap("node-group", "benchmark-nodes"));

        } else if (Constants.CONTAINER_TYPE_BENCHMARK.equals(containerType)
            && ((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))) {

            LOGGER.debug("Setting node selector to 'benchmark-nodes' for BENCHMARK container type.");
            podSpec.nodeSelector(Collections.singletonMap("node-group", "benchmark-nodes"));

        } else {
            LOGGER.error("Got a request to create a container with type={} and parentType={}. "
                + "No rule found to determine its type.", containerType, parentType);
            // then apply it where you (KUBERNETES) can
            podSpec.nodeSelector(null);
        }
    }

    /**
     * Creates a pod specification with the given container and additional settings based on the image name, container type, and parent type.
     *
     * @param container The {@code V1Container} object that will be added to the pod.
     * @param imageName The name of the Docker image used for the container.
     * @param containerType The type of the container. This determines the appropriate node group for the pod.
     * @param parentType The type of the parent container or pod. If {@code null}, it is assumed there is no parent.
     * @return A {@code V1PodSpec} object configured with the provided specifications and settings.
     */
    private V1PodSpec createPodSpec(V1Container container, String imageName, String containerType, String parentType) {
        V1PodSpec podSpec = new V1PodSpec()
            .restartPolicy("Never")
            .addContainersItem(container)
            .addImagePullSecretsItem(new V1LocalObjectReference().name("gitlab-registry-secret"))
            .addImagePullSecretsItem(new V1LocalObjectReference().name("my-dockerhub-secret"));

        if (!imageName.contains("earthquakesan/ckan-solr:2.8.0")) {
            V1PodSecurityContext podSecurityContext = new V1PodSecurityContext();
            podSecurityContext.setFsGroup(0L);
            podSecurityContext.setRunAsUser(0L);
            podSpec.setSecurityContext(podSecurityContext);
        }

        applyNodeSelector(podSpec, containerType, parentType);
        return podSpec;
    }


    /**
     * Creates metadata for a Kubernetes pod, including labels that identify the container type and parent pod.
     *
     * @param podName The name of the pod.
     * @param containerType The type of the container. This will be included as a label in the metadata.
     * @param parentPodName The name of the parent pod, if any. This will also be included as a label in the metadata.
     * @return A {@code V1ObjectMeta} object containing the metadata for the pod.
     */
    private V1ObjectMeta createPodMetadata(String podName, String containerType, String parentPodName) {
        Map<String, String> labels = new HashMap<>();
        labels.put(LABEL_TYPE, containerType);
        labels.put(LABEL_PARENT, parentPodName);
        labels.put("app", podName);
        return new V1ObjectMeta().name(podName).namespace(nameSpace).labels(labels);
    }


    /**
     * Deploys a Kubernetes pod and monitors its startup.
     *
     * @param pod The {@code V1Pod} object representing the pod to be deployed.
     * @param podName The name of the pod.
     * @return A string which is the pod IP as a DNS frinely version, or {@code null} if the deployment fails.
     */
    private String deployPod(V1Pod pod, String podName) {
        try {
            GenericKubernetesApi<V1Pod, V1PodList> podClient = new GenericKubernetesApi<>(V1Pod.class, V1PodList.class, "", "v1", "pods", client);
            V1Pod createdPod = podClient.create(pod).throwsApiException().getObject();
            return monitorPodStartup(podClient, createdPod);
        } catch (ApiException e) {
            LOGGER.error("Failed to create pod for image. Returning null.", e);
            return null;
        }
    }


    /**
     * Monitors the startup of a Kubernetes pod and waits until it reaches the "Running" phase with an assigned IP.
     * If the pod successfully starts, it converts the pod's IP to a DNS ID and notifies registered observers.
     *
     * @param podClient The Kubernetes API client for managing pods.
     * @param createdPod The newly created pod object.
     * @return The converted DNS ID of the pod if it reaches the "Running" phase with an assigned IP within the timeout period, or {@code null} otherwise.
     */

    private String monitorPodStartup(GenericKubernetesApi<V1Pod, V1PodList> podClient, V1Pod createdPod) {
        String createdPodName = createdPod.getMetadata().getName();

        if (createdPodName != null) {
            LOGGER.debug("Successfully created pod with name: {}", createdPodName);
            long startTime = System.currentTimeMillis();
            V1PodList podList = podClient.list(nameSpace).getObject();
            while (System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)) {
                try {
                    V1Pod targetPod = podList.getItems().stream()
                        .filter(podTocheck -> createdPodName.equals(podTocheck.getMetadata().getName()))
                        .findFirst()
                        .orElse(null);

                        LOGGER.debug("Target pod found: {}", targetPod.getMetadata().getName());

                    String podPhase = targetPod.getStatus().getPhase();
                    if ("Running".equals(podPhase)) {
                        String podIP = targetPod.getStatus().getPodIP();
                        if (podIP != null && !podIP.isEmpty()) {
                            String convertedIP = convertIP2dnsId(podIP);
                            for (ContainerStateObserver observer : containerObservers) {
                                observer.addObservedContainer(convertedIP);
                            }
                            LOGGER.debug("return this converted  IP: {}", convertedIP);
                            return convertedIP;
                        }
                    }
                        LOGGER.trace("pod phase is {}", podPhase);
                    podList = podClient.list(nameSpace).getObject();
                }catch (Exception ex){
                    LOGGER.error(ex.getMessage());
                }
                try {
                    Thread.sleep(POD_PHASE_CHECK_INTERVAL); // Wait for 2 second before checking again
                } catch (InterruptedException e) {
                    LOGGER.error("Thread interrupted while waiting for pod IP: " + e.getMessage());
                }
            }

            LOGGER.warn("Timeout reached. Pod {} IP is still not available.", createdPodName);
        } else {
            LOGGER.error("Failed to create the pod.");
        }
        LOGGER.warn("return null");
        return null; // Return null if the IP address is not available within the timeout
    }

    /**
     * Creates and deploys a Kubernetes container within a pod.
     *
     * @param imageName The name of the Docker image to be used for the container.
     * @param podName The name of the pod in which the container will run.
     * @param containerType The type of the container, which can influence how it is managed or monitored.
     * @param parentPodName The name of the parent pod, if any. This helps in organizing and tracking related containers.
     * @param env An array of environment variables to be set for the container.
     * @param command An array of commands to be executed within the container.
     * @param constraints A map of constraints that may affect the deployment or behavior of the container.
     * @return POD name which is a DNS friendly pod IP , or {@code null} if it fails.
     */
    private String createContainerKub(String imageName, String podName, String containerType, String parentPodName,
                                          String[] env, String[] command, Map<String, Object> constraints) {
        imageName = completeImageName(imageName);
        LOGGER.debug("Creating container: Image = {}, Pod Name = {}, Container Type = {}, Parent ID = {}",
            imageName, podName, containerType, parentPodName);

        env = prepareEnvironmentVariables(env);
        env = convertAllNamesInENV(env);

        List<V1EnvVar> environmentVariables = parseEnvironmentVariables(env);
        addDefaultEnvironmentVariables(environmentVariables);

        containerType = determineContainerType(containerType, parentPodName);
        if (containerType == null) return null;

        V1Container container = createContainerSpec(podName, imageName, environmentVariables, command, constraints);

        String parentType = getParentType(parentPodName);

        V1PodSpec podSpec = createPodSpec(container, imageName, containerType, parentType);
        // this is a patch for some situation which container type is null
        if ((((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))
            && Constants.CONTAINER_TYPE_SYSTEM.equals(containerType))
            || Constants.CONTAINER_TYPE_SYSTEM.equals(parentType)) {
            containerType  = Constants.CONTAINER_TYPE_SYSTEM;
            LOGGER.info("TTHHIISS IISS HHAAPPEENNEEDD");
        }
        V1ObjectMeta metadata = createPodMetadata(podName, containerType, parentPodName);
        V1Pod pod = new V1Pod().metadata(metadata).spec(podSpec);

        return deployPod(pod, podName);
    }

    /**
     * Retrieves the type of the parent pod.
     *
     * @param parentPodName The name of the parent pod.
     * @return The type of the parent pod, or {@code null} if the parent pod does not exist or its type cannot be determined.
     */
    private String getParentType(String parentPodName) {
        String parentType = null;
        if(parentPodName != null) {
            parentType = getContainerType(parentPodName);
        }
        return parentType;
    }

    /**
     * Checks if a given string is a container name and, if so, replaces it with its corresponding IP address.
     *
     * @param s The input string to be checked and potentially modified.
     * @return The original string if it is not a valid container name, or the modified string with the container name replaced by its IP address.
     */
    private String changeIfItIsContainerName(String s) {
        String[] parts = s.split("=");
        if(parts.length !=2 ){
         LOGGER.error("Invalid container name. {}",s);
         return s;
        }else {
            if (isItContainerName(parts[1])) {
                String newName = mapName2IP(parts[1]);
                return parts[0] + "=" + newName;
            }
            return s;
        }
    }

    /**
     * Converts a pod IP address to its corresponding DNS identifier.
     *
     * @param podIP The IP address of the pod.
     * @return A string representing the DNS identifier for the pod, formatted as `ip-address.default.pod.cluster.local`.
     */
    private String convertIP2dnsId(String podIP) {
        return podIP.replace(".", "-") + ".default.pod.cluster.local";
    }

    /**
     * Maps a DNS identifier back to the pod name by querying the Kubernetes API.
     *
     * @param ip The DNS identifier of the pod, typically in the format `ip-address.default.pod.cluster.local`.
     * @return The name of the pod corresponding to the given IP address, or {@code null} if no matching pod is found.
     */
    private String mapIp2Name(String ip) {

        String podIp = ip.replace("-",".").replace(".default.pod.cluster.local", "");
        //LOGGER.info("map this IP: {} which extracted from this {}", podIp, ip);

        GenericKubernetesApi<V1Pod, V1PodList> podClient =
            new GenericKubernetesApi<>(V1Pod.class, V1PodList.class, "", "v1", "pods", client);

        V1PodList podList = podClient.list(nameSpace).getObject();
        V1Pod targetPod = podList.getItems().stream()
            .filter(podTocheck -> podIp.equals(podTocheck.getStatus().getPodIP()))
            .findFirst()
            .orElse(null);
        return targetPod.getMetadata().getName();
        }

    /**
     * Retrieves a Kubernetes pod by its IP address.
     *
     * @param podIP The IP address of the pod.
     * @return The {@link V1Pod} object corresponding to the given IP address, or {@code null} if no matching pod is found.
     */
    @Override
    public V1Pod getPod(String podIP) {
        //LOGGER.info("getPod with {}",podIP);
        String podName = mapIp2Name(podIP);
        //LOGGER.info("Attempting to retrieve pod: {} in namespace: {}", podName, this.nameSpace);
        if(podName == null){
            LOGGER.error("No pod found with podIP: {}", podIP);
        }
        CoreV1Api coreV1Api = new CoreV1Api(client);
        try {
            // Inspect the pod by name
            V1Pod pod = coreV1Api.readNamespacedPod(podName, this.nameSpace, null);
            //LOGGER.info("Successfully retrieved pod: {} in namespace: {}", podName, this.nameSpace);
            return pod;
        } catch (ApiException exc) {
            LOGGER.error("Failed to get pod: {} in namespace: {}", podName, this.nameSpace);
            return null;
        }
    }

    /**
     * Creates Kubernetes resource requirements based on the provided constraints.
     *
     * @param constraints A map of constraints that may include memory and CPU limits.
     * @return A {@link V1ResourceRequirements} object representing the resource limits for a pod or container.
     */
    private V1ResourceRequirements createResourceRequirementsFromConstraints(Map<String, Object> constraints) {
        V1ResourceRequirements resourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();

        if (constraints != null && (constraints.containsKey(MEMORY_LIMIT_CONSTRAINT)
            || constraints.containsKey(NANO_CPU_LIMIT_CONSTRAINT))) {

            // if there is a memory limitation
            if (constraints.containsKey(MEMORY_LIMIT_CONSTRAINT)) {
                long memory = (Long) constraints.get(MEMORY_LIMIT_CONSTRAINT);
                limits.put("memory", new Quantity(String.valueOf(memory)));
            }
            // if there is a CPU limitation
            if (constraints.containsKey(NANO_CPU_LIMIT_CONSTRAINT)) {
                // CPU quota in units of 10^-9 CPUs.
                long nanoCPUs = (Long) constraints.get(NANO_CPU_LIMIT_CONSTRAINT);
                limits.put("cpu", new Quantity(String.valueOf(nanoCPUs)));
            }
        }

        resourceRequirements.setLimits(limits);
        return resourceRequirements;
    }


    @Deprecated
    @Override
    public void stopContainer(String containerId) {
        LOGGER.error("ContainerManager.stopContainer() is deprecated! Will remove container instead");
        removeContainer(containerId);
    }

    /**
     * Removes a Kubernetes pod by its IP address, with conditions based on the deployment environment and exit code.
     *
     * @param podIp The IP address of the pod to be removed.
     */
    @Override
    public void removeContainer(String podIp) {

        LOGGER.info("removing pod {}.", podIp);
        //String podName = mapIp2Name(podIp);
        try {
            Long exitCode = getContainerPodExitCode(podIp);

            if (DEPLOY_ENV.equals(DEPLOY_ENV_DEVELOP)) {
                LOGGER.info("Will not remove pod {}. Development mode is enabled.", podIp);
            } else if (DEPLOY_ENV.equals(DEPLOY_ENV_TESTING) && (exitCode != null && exitCode != 0)) {
                LOGGER.info("Will not remove pod {}. ExitCode: {} != 0 and testing mode is enabled.", podIp, exitCode);
            } else {
                LOGGER.info("Removing pod {}.", podIp);

                // Initialize the API client
                CoreV1Api api = new CoreV1Api(client);

                String podName = mapIp2Name(podIp);

                // Delete the pod
                V1DeleteOptions deleteOptions = new V1DeleteOptions();
                api.deleteNamespacedPod(podName, nameSpace, null, null, null, null, null, deleteOptions);

                // Wait for the pod to be deleted
                Waiting.waitFor(() -> {
                    try {
                        api.readNamespacedPod(podName, nameSpace, null);
                        return false;
                    } catch (ApiException e) {
                        if (e.getCode() == 404) {
                            return true; // Pod not found
                        }
                        throw e; // Other errors should not be ignored
                    }
                }, KUBERNETES_POLL_INTERVAL);
            }
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                LOGGER.error("Couldn't remove pod {} because it doesn't exist", podIp);
            } else {
                LOGGER.error("Couldn't remove pod {}.", podIp, e);
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error while removing pod {}.", podIp, e);
        }
    }

    @Deprecated
    @Override
    public void stopParentAndChildren(String parentId) {
        LOGGER.error("ContainerManager.stopParentAndChildren() is deprecated! Will remove them instead");
        removeParentAndChildren(parentId);
    }


    /**
     * Removes a parent pod and all its child pods recursively.
     *
     * @param parentPodIp The IP address of the parent pod to be removed.
     */
    @Override
    public void removeParentAndChildren(String parentPodIp) {
        LOGGER.info("removing parent and child pod {}.", parentPodIp);
        // Remove the parent pod

        removeContainer(parentPodIp);
        // Find child pods
        try {
            CoreV1Api api = new CoreV1Api(client);

            // Search for pods with the label "parent=<parentPodName>"
            String labelSelector = String.format(LABEL_PARENT+"=%s", parentPodIp);
            LOGGER.info("Removing parent and child pod {}.", labelSelector);
            V1PodList childPods = api.listNamespacedPod(
                nameSpace, null, null, null, null, labelSelector, null, null, null, null, false);

            for (V1Pod childPod : childPods.getItems()) {
                if (childPod != null && childPod.getMetadata() != null) {
                    String childPodIp = childPod.getStatus().getPodIP();
                    LOGGER.info("Removing child pod with ip{}.", childPodIp);
                    String convertedChildIP =convertIP2dnsId(childPodIp);
                    LOGGER.info("Removing child pod with convertedip{}.", convertedChildIP);
                    // Recursively remove the child pod and its children
                    removeParentAndChildren(convertedChildIP);
                }
            }
        } catch (ApiException e) {
            LOGGER.error("Error while finding child pods: " + e.getResponseBody(), e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while removing pods: " + e.toString(), e);
        }
    }

    /**
     * Retrieves the exit code of a container within a Kubernetes pod.
     *
     * @param podIp The IP address of the pod.
     * @return The exit code of the terminated container, or null if the pod is still running or has no container statuses.
     * @throws ContainerPodException If there is an error retrieving the pod's exit code.
     */
    @Override
    public Long getContainerPodExitCode(String podIp) throws ContainerPodException {
        LOGGER.info("get exit code for pod {}.", podIp);
        String podName = mapIp2Name(podIp);
        LOGGER.info("get exit code for pod {}.", podName);
        try {
            // Initialize the API client
            CoreV1Api api = new CoreV1Api(client);

            // Fetch the pod details
            V1Pod pod = api.readNamespacedPod(podName, nameSpace, null);

            // Check if the pod has a terminated container
            if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
                LOGGER.warn("Couldn't get the exit code for pod {}. Pod has no container statuses.", podName);
                return null;
            }

            for (V1ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
                V1ContainerStateTerminated terminatedState = containerStatus.getState().getTerminated();
                if (terminatedState != null) {
                    return terminatedState.getExitCode().longValue();
                }
            }

            // Pod is still running
            LOGGER.warn("Couldn't get the exit code for pod {}. Pod is not terminated.", podName);
            return null;
        } catch (ApiException e) {
            throw new ContainerPodException("Error getting container pod exit code: " + podName, e);
        }
    }

    // we do not use this here
    // todo change in interface and define another class as return value which is Service and V1Pod
    // maybe for kubernetes we dont need this method ! we cover usage of this method in docker version  in in the method in diffrent way here

//    @Override
//    public Service getContainerInfo(String serviceName) throws InterruptedException, DockerException {
//        return null;
//    }


    @Override
    public List<String> getContainers(ContainerCriteria criteria) {
        return null;
    }


    @Override
    public String getContainerPodId(String podIp) {
        return  podIp;
    }

    // this is the container name which is the dns friendly IP of a  pod
    @Override
    public String getContainerPodName(String podId) {
        return podId;
    }
    /**
     * Checks if a given name corresponds to an existing pod in the specified namespace.
     *
     * @param name The name of the pod to check.
     * @return {@code true} if the pod exists, otherwise {@code false}.
     */

    private Boolean isItContainerName(String name) {
        try {
            CoreV1Api api = new CoreV1Api(client);
            V1PodList podList = api.listNamespacedPod(nameSpace, null, null, null, null, null, null, null, null, null, false);
            V1Pod targetPod = podList.getItems().stream()
                .filter(podTocheck -> name.equals(podTocheck.getMetadata().getName()))
                .findFirst()
                .orElse(null);

            if (targetPod == null) {
                return false;
            }
            return true;
        }catch (ApiException e) {
            LOGGER.error("Error while finding pods: " + e.getResponseBody(), e);
            return false;
        }
    }

    /**
     * Maps a pod name to its corresponding IP address and converts it to a DNS-compatible format.
     *
     * @param name The name of the pod.
     * @return The converted DNS-compatible IP address of the pod, or null if the pod is not found or in an unexpected state.
     */
    private String mapName2IP(String name) {
        try {
            CoreV1Api api = new CoreV1Api(client);
            V1PodList podList = api.listNamespacedPod(nameSpace, null, null, null, null, null, null, null, null, null, false);
            V1Pod targetPod = podList.getItems().stream()
                .filter(podTocheck -> name.equals(podTocheck.getMetadata().getName()))
                .findFirst()
                .orElse(null);

            LOGGER.debug("Target pod found: {}", targetPod.getMetadata().getName());

            String podPhase = targetPod.getStatus().getPhase();
            if ("Running".equals(podPhase)) {
                String podIP = targetPod.getStatus().getPodIP();
                LOGGER.debug("Pod IP: {}", podIP);
                if (podIP != null && !podIP.isEmpty()) {
                    LOGGER.debug("Obtained Pod IP: {}", podIP);
                    String convertedIP = convertIP2dnsId(podIP);
                    LOGGER.debug("Converted IP: {}", convertedIP);
                    return convertedIP;
                }
            }
            return null;
        }catch (Exception ex){
            LOGGER.error(ex.getMessage());
            return null;
        }
    }


    @Override
    public void addContainerObserver(ContainerStateObserver containerObserver) {
        containerObservers.add(containerObserver);
    }

    @Override
    public void pullImage(String imageName) {
        // we dont need this in kubernetes
    }

//    //TODO: do we need this for kubernetes? no usage in docker version
//    @Override
//    public ContainerStats getStats(String containerId) {
//        return null;
//    }


    /**
     * Retrieves the type of a container within a Kubernetes pod based on its labels.
     *
     * @param podIP The IP address of the pod.
     * @return The type of the container, or null if the pod or its type label is not found.
     */
    @Override
    public String getContainerType(String podIP) {
        LOGGER.debug("getContainerType({})", podIP);
        // Logic to retrieve the parent pod based on the podName
        V1Pod parent = getPod(podIP);

        if (parent == null) {
            LOGGER.warn("Parent pod not found for pod : {}", podIP);
            return null;
        }

        String containerType = parent.getMetadata().getLabels().get(LABEL_TYPE);

        if (containerType == null) {
            LOGGER.warn("Container type not found in labels for pod: {}", podIP);
        } else {
            LOGGER.debug("Resolved container type for pod {}: {}", podIP, containerType);
        }

        return containerType;
    }
}
