package org.hobbit.controller.containers.kubernetes;

import org.hobbit.controller.containers.ContainerManager;
import org.hobbit.controller.containers.ContainerPodException;
import org.hobbit.controller.containers.ContainerStateObserver;
import org.hobbit.controller.data.ContainerCriteria;
import org.hobbit.controller.utils.Waiting;
import org.hobbit.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContainerManagerImpl implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);
    private ApiClient client;
    //TODO make configable
    private int TIMEOUT_MILLISECONDS = 60000;
    private String nameSpace = "default";
    private static final long KUBERNETES_POLL_INTERVAL = 5000; // Poll interval in ms
    private static final long KUBERNETES_EXITCODE_SIGKILL = 137L; // Equivalent to SIGKILL exit code
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

    private static final int MAX_POD_NAME_LENGTH = 63;
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
        // we dont need this in kubernetes
//        if (pullImage) {
//            pullImage(imageName);
//        }
        //todo why in Interface there is no experimentID ?
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

    public static String shortenAndValidatePodName(String podName) {
        if (podName == null || podName.isEmpty()) {
            throw new IllegalArgumentException("Pod name cannot be null or empty.");
        }

        String shortenedName = podName;

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

        LOGGER.info("start Container podname is {}", podName);

        if (imageName != null) {
            LOGGER.info("image name is {}", imageName);
        } else {
            LOGGER.info("image name is null");
        }

        if (containerType != null) {
            LOGGER.info("container type is {}", containerType);
        } else {
            LOGGER.info("container type is null");
        }

        if (parentId != null) {
            LOGGER.info("Parent ID is {}", parentId);
        } else {
            LOGGER.info("Parent ID is null");
        }

        if (env != null && env.length > 0) {
            LOGGER.info("env is {}", String.join(",", env));
        } else {
            LOGGER.info("env is null or empty");
        }

        if (command != null && command.length > 0) {
            LOGGER.info("command is {}", String.join(",", command));
        } else {
            LOGGER.info("command is null or empty");
        }

        if (experimentId != null) {
            LOGGER.info("experimentID is {}", experimentId);
        } else {
            LOGGER.info("experimentID is null");
        }

        return createContainerKub(imageName, podName, containerType, parentId, env, command, constraints);
    }

    /**
     * Creates environment variables from the provided list of key-value pairs.
     */
//    private List<V1EnvVar> createEnvVariables(List<AbstractMap.SimpleEntry<String, String>> variables) {
//        List<V1EnvVar> env = new ArrayList<>();
//        if (variables != null) {
//            for (Map.Entry<String, String> entry : variables) {
//                V1EnvVar v1env = new V1EnvVar();
//                v1env.setName(entry.getKey());
//                v1env.setValue(entry.getValue());
//                env.add(v1env);
//            }
//        }
//        return env;
//    }

    /**
     * Initializes and configures a Kubernetes API client with custom timeout settings.
     *
     * This method:
     * - Creates a default Kubernetes API client using the default configuration.
     * - Sets custom connection, read, and write timeouts.
     * - Sets the configured client as the default client in the global Configuration class.
     *
     * @return ApiClient configured with specified timeouts.
     * @throws IOException if an error occurs during client initialization.
     */
    protected ApiClient initiateClient() throws IOException {
        try{
            LOGGER.info("initiating Kubernetes client");
            ApiClient client = Config.defaultClient();
            client.setConnectTimeout(TIMEOUT_MILLISECONDS);
            client.setReadTimeout(TIMEOUT_MILLISECONDS);
            client.setWriteTimeout(TIMEOUT_MILLISECONDS);
            Configuration.setDefaultApiClient(client);
            LOGGER.info("Kubernetes API client initiated with default configuration.");
            return client;
        }
        catch (Exception ex){
            LOGGER.error("Failed to initiate Kubernetes API client", ex);
            throw ex;
        }
    }

    private String createContainerKub(String imageName, String podName, String containerType, String parentPodName,
                                      String[] env, String[] command, Map<String, Object> constraints) {
        LOGGER.info("Creating container: Image = {}, Pod Name = {}, Container Type = {}, Parent ID = {}",
            imageName, podName, containerType, parentPodName);

        // Prepare environment variable
        List<V1EnvVar> environmentVariables = new ArrayList<>();
        if (env != null) {
            LOGGER.info("Processing environment variables, total count: {}", env.length);

            for (String envVar : env) {
                LOGGER.info("Parsing environment variable: {}", envVar);
                String[] parts = envVar.split("=", 2);

                if (parts.length == 2) {
                    LOGGER.info("Adding environment variable - Name: {}, Value: {}", parts[0], parts[1]);
                    environmentVariables.add(new V1EnvVar().name(parts[0]).value(parts[1]));
                } else {
                    LOGGER.warn("Skipping invalid environment variable: {}", envVar);
                }
            }
        } else {
            LOGGER.warn("No environment variables provided.");
        }

        LOGGER.info("Determining container type for parent pod: {}", parentPodName);
        String parentType = null;
        if(parentPodName != null) {
            parentType = getContainerType(parentPodName);
        }


        if (containerType == null || containerType.isEmpty()) {
            LOGGER.info("No container type provided, attempting to use parent pod's type.");

            if (parentType == null) {
                LOGGER.error("Unable to resolve parent container type. Returning null to avoid creating pod with no type.");
                return null;
            } else {
                LOGGER.info("Using parent container type: {}", parentType);
                containerType = parentType;
            }
        }


        // Create resource requirements if constraints are provided
        LOGGER.info("Creating resource requirements from constraints.");
        V1ResourceRequirements resourceRequirements = createResourceRequirementsFromConstraints(constraints);

        // Prepare the container specification
        LOGGER.info("Preparing container specification with name: {}, image: {}", podName, imageName);
        V1Container container = new V1Container()
            .name(podName)
            .image(imageName)
            .env(environmentVariables)
            .command(command != null ? Arrays.asList(command) : null)
            .resources(resourceRequirements);

        LOGGER.info("Container specification created successfully.");


        // Create a volume and volume mount for shared directories
//        V1Volume volume = createVolumeForSharedDirectory();
//        V1VolumeMount volumeMount = new V1VolumeMount()
//            .name("shared-dir")
//            .mountPath("/shared");

//        container.setVolumeMounts(Collections.singletonList(volumeMount));

        // Create the pod specification
        LOGGER.info("Creating pod specification for container: {}", podName);
        V1PodSpec podSpec = new V1PodSpec()
            .restartPolicy("Never")
            .containers(Collections.singletonList(container));
            //.volumes(Collections.singletonList(volume));

        LOGGER.info("Determining node selector based on container type: {} and parent type: {}", containerType, parentType);
        if ((((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))
            && Constants.CONTAINER_TYPE_SYSTEM.equals(containerType))
            || Constants.CONTAINER_TYPE_SYSTEM.equals(parentType)) {

            LOGGER.info("Setting node selector to 'system-nodes' for container type: {}", containerType);
            podSpec.nodeSelector(Collections.singletonMap("node-group", "system-nodes"));

            LOGGER.debug("Assigning container type to SYSTEM.");
            containerType = Constants.CONTAINER_TYPE_SYSTEM;

        } else if (Constants.CONTAINER_TYPE_DATABASE.equals(containerType)
            && ((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType)
            || Constants.CONTAINER_TYPE_DATABASE.equals(parentType))) {

            LOGGER.info("Setting node selector to 'benchmark-nodes' for DATABASE container type.");
            podSpec.nodeSelector(Collections.singletonMap("node-group", "benchmark-nodes"));

        } else if (Constants.CONTAINER_TYPE_BENCHMARK.equals(containerType)
            && ((parentType == null) || Constants.CONTAINER_TYPE_BENCHMARK.equals(parentType))) {

            LOGGER.info("Setting node selector to 'benchmark-nodes' for BENCHMARK container type.");
            podSpec.nodeSelector(Collections.singletonMap("node-group", "benchmark-nodes"));

        } else {
            LOGGER.error("Got a request to create a container with type={} and parentType={}. "
                + "No rule found to determine its type. Returning null.", containerType, parentType);
            return null;
        }

        LOGGER.info("Pod specification created successfully.");


        LOGGER.info("Creating labels for pod: {}", podName);

        Map<String, String> labels = new HashMap<>();
        labels.put(LABEL_TYPE, containerType);
        labels.put(LABEL_PARENT, parentPodName);

        LOGGER.info("Labels set - Type: {}, Parent: {}", containerType, parentPodName);

// Build the pod metadata
        LOGGER.info("Building pod metadata for pod: {} in namespace: {}", podName, nameSpace);
        V1ObjectMeta metadata = new V1ObjectMeta()
            .name(podName)
            .namespace(nameSpace)
            .labels(labels);

// Build the pod object
        LOGGER.info("Constructing the pod object.");
        V1Pod pod = new V1Pod()
            .metadata(metadata)
            .spec(podSpec);

        LOGGER.info("Pod object created successfully: {}", podName);


        // Deploy the pod
        try {
            GenericKubernetesApi<V1Pod, V1PodList> podClient =
                new GenericKubernetesApi<>(V1Pod.class, V1PodList.class, "", "v1", "pods", client);

            LOGGER.info("Sending request to create pod with name: {}", podName);

            V1Pod createdPod = podClient.create(pod).throwsApiException().getObject();
            String createdPodName = createdPod.getMetadata().getName();

            // If the creation was successful
            if (createdPodName != null) {
                LOGGER.info("Successfully created pod with name: {}", createdPodName);
                for (ContainerStateObserver observer : containerObservers) {
                    LOGGER.info("Notifying observer about created pod with name: {}", createdPodName);
                    observer.addObservedContainer(createdPodName);
                }
            }

            return createdPodName;
        } catch (ApiException e) {
            LOGGER.error("Failed to create pod for image: {}. Returning null.", imageName, e);
            return null;
        }

    }


    private V1Pod getPod(String podName) {
        LOGGER.info("Attempting to retrieve pod: {} in namespace: {}", podName, this.nameSpace);

        CoreV1Api coreV1Api = new CoreV1Api(client);

        try {
            // Inspect the pod by name
            V1Pod pod = coreV1Api.readNamespacedPod(podName, this.nameSpace, null);
            LOGGER.info("Successfully retrieved pod: {} in namespace: {}", podName, this.nameSpace);
            return pod;
        } catch (ApiException exc) {
            LOGGER.error("Failed to get pod: {} in namespace: {}. Error: {}", podName, this.nameSpace, exc.getMessage(), exc);
            return null;
        }
    }


//    /**
//     *
//     * @param parentId
//     * @return String of type based on the label of the parent
//     */
//    private String resolveContainerType(String parentId) {
//        // Logic to resolve container type based on the parentId
//        // return null if resolution fails
//        V1Pod parent = getPod(parentId);
//        return (parent == null) ? null : parent.getMetadata().getLabels().get(LABEL_TYPE);
//    }

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

    private V1Volume createVolumeForSharedDirectory() {
        return new V1Volume().name("shared-dir")
            .hostPath(new V1HostPathVolumeSource().path("/shared").type("Directory"));
    }

    @Deprecated
    @Override
    public void stopContainer(String containerId) {
        LOGGER.error("ContainerManager.stopContainer() is deprecated! Will remove container instead");
        removeContainer(containerId);
    }


    @Override
    public void removeContainer(String podName) {
        try {
            Long exitCode = getContainerPodExitCode(podName);

            if (DEPLOY_ENV.equals(DEPLOY_ENV_DEVELOP)) {
                LOGGER.info("Will not remove pod {}. Development mode is enabled.", podName);
            } else if (DEPLOY_ENV.equals(DEPLOY_ENV_TESTING) && (exitCode != null && exitCode != 0)) {
                LOGGER.info("Will not remove pod {}. ExitCode: {} != 0 and testing mode is enabled.", podName, exitCode);
            } else {
                LOGGER.info("Removing pod {}.", podName);

                // Initialize the API client
                CoreV1Api api = new CoreV1Api(client);

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
                LOGGER.error("Couldn't remove pod {} because it doesn't exist", podName);
            } else {
                LOGGER.error("Couldn't remove pod {}.", podName, e);
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error while removing pod {}.", podName, e);
        }
    }

    @Deprecated
    @Override
    public void stopParentAndChildren(String parentId) {
        LOGGER.error("ContainerManager.stopParentAndChildren() is deprecated! Will remove them instead");
        removeParentAndChildren(parentId);
    }

    @Override
    public void removeParentAndChildren(String parentPodName) {
        // Remove the parent pod
        removeContainer(parentPodName);

        // Find child pods
        try {
            CoreV1Api api = new CoreV1Api(client);

            // Search for pods with the label "parent=<parentPodName>"
            String labelSelector = String.format(LABEL_PARENT+"=%s", parentPodName);
            V1PodList childPods = api.listNamespacedPod(
                nameSpace, null, null, null, null, labelSelector, null, null, null, null, false);

            for (V1Pod childPod : childPods.getItems()) {
                if (childPod != null && childPod.getMetadata() != null) {
                    String childPodName = childPod.getMetadata().getName();
                    if (childPodName != null) {
                        // Recursively remove the child pod and its children
                        removeParentAndChildren(childPodName);
                    }
                }
            }
        } catch (ApiException e) {
            LOGGER.error("Error while finding child pods: " + e.getResponseBody(), e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while removing pods: " + e.toString(), e);
        }
    }


    @Override
    public Long getContainerPodExitCode(String podName) throws ContainerPodException {
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
    public String getContainerPodId(String podName) {
        try {
            // Fetch the pod information using its name and namespace
            CoreV1Api api = new CoreV1Api(client);
            V1Pod pod = api.readNamespacedPod(podName, nameSpace, null);

            // Return the Pod's UID (unique identifier)
            return pod.getMetadata().getUid();
        } catch (Exception e) {
            LOGGER.error("Failed to fetch Pod ID for Pod name: {} in namespace: {}. Error: {}", podName, nameSpace, e);
            return null;
        }
    }

    @Override
    public String getContainerPodName(String podId) {
        try {
            // Fetch the list of all pods in the namespace
            CoreV1Api api = new CoreV1Api(client);
            V1PodList podList = api.listNamespacedPod(nameSpace, null, null, null, null, null, null, null, null, null, false);

            // Search for the pod with the given UID
            for (V1Pod pod : podList.getItems()) {
                if (pod.getMetadata().getUid().equals(podId)) {
                    // Return the Pod's name
                    return pod.getMetadata().getName();
                }
            }

            // If no matching pod is found, return null
            LOGGER.warn("No Pod found with ID: {} in namespace: {}", podId, nameSpace);
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to fetch Pod name for Pod ID: {} in namespace: {}. Error: {}", podId, nameSpace, e);
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


    @Override
    public String getContainerType(String podName) {
        LOGGER.info("Resolving container type for pod: {}", podName);

        // Logic to retrieve the parent pod based on the podName
        V1Pod parent = getPod(podName);

        if (parent == null) {
            LOGGER.warn("Parent pod not found for pod name: {}", podName);
            return null;
        }

        String containerType = parent.getMetadata().getLabels().get(LABEL_TYPE);

        if (containerType == null) {
            LOGGER.warn("Container type not found in labels for pod: {}", podName);
        } else {
            LOGGER.info("Resolved container type for pod {}: {}", podName, containerType);
        }

        return containerType;
    }
}
