package org.hobbit.controller.kubernetes.fabric8;

import com.spotify.docker.client.messages.NetworkConfig;
import com.spotify.docker.client.messages.RegistryAuth;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.hobbit.controller.gitlab.GitlabControllerImpl;
import org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources.*;
import org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PodsManagerImpl implements PodsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PodsManagerImpl.class);
    private KubernetesClient k8sClient;

    public static final String USER_NAME_KEY = "GITLAB_USER";
    public static final String USER_EMAIL_KEY = "GITLAB_EMAIL";
    public static final String USER_PASSWORD_KEY = GitlabControllerImpl.GITLAB_TOKEN_KEY;
    public static final String REGISTRY_URL_KEY = "REGISTRY_URL";
    public static final String LOGGING_GELF_ADDRESS_KEY = "LOGGING_GELF_ADDRESS";

    private final RegistryAuth gitlabAuth;
    private String gelfAddress = null;

    private static final int POD_MAX_NAME_LENGTH = 63;
    private static final Pattern PORT_PATTERN = Pattern.compile(":[0-9]+/");


    /**
     * Default network policy namespace for new pods
     */
    public static final String HOBBIT_K8S_NETWORK = "hobbit";

    public PodsManagerImpl() {
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

        CustomResourceDefinition cniCrd = k8sClient.customResourceDefinitions().withName("network-attachment-definitions.k8s.cni.cncf.io").get();



        MixedOperation<NetworkAttachmentDefinition, NetworkAttachmentDefinitionList, DoneableNetworkAttachmentDefinition,
            Resource<NetworkAttachmentDefinition, DoneableNetworkAttachmentDefinition>> netAttachmentDefClient = k8sClient
            .customResources(cniCrd, NetworkAttachmentDefinition.class, NetworkAttachmentDefinitionList.class, DoneableNetworkAttachmentDefinition.class);


        NetworkAttachmentDefinitionList netInterfaces = netAttachmentDefClient.inAnyNamespace().list();

        // try to find hobbit network in existing network interfaces
        String hobbitNetwork = null;
        for (NetworkAttachmentDefinition net: netInterfaces.getItems()){
            LOGGER.info("Network Interface: "+ net.getKind());
            LOGGER.info("Network Interface: "+ net.getMetadata().getName());
            if (net.getMetadata().getName().equals(HOBBIT_K8S_NETWORK)){
                hobbitNetwork = net.getSpec().getConfig().getType();
                break;
            }
        }

        // if not found - create new one
        if (hobbitNetwork == null){
            LOGGER.warn("Could not find hobbit kubernetes CNI network interface, creating a new one");

            NetworkAttachmentDefinition network = new NetworkAttachmentDefinition();
            ObjectMeta meta = new ObjectMeta();
            meta.setName("HOBBIT_K8S_NETWORK");
            network.setMetadata(meta);

            Spec netSpec = new Spec();
            Config netCofig = new Config();

            netCofig.setCniVersion("0.3.0");
            netCofig.setType("macvlan");
            netCofig.setMaster("eth0");
            netCofig.setMode("bridge");

            Ipam ipam = new Ipam();
            ipam.setType("host-local");
            ipam.setSubnet("192.168.59.0/24");
            ipam.setRangeStart("192.168.59.2");
            ipam.setRangeEnd("192.168.59.2");

            List<Routes> routes = new ArrayList<>();
            Routes routes1 = new Routes();
            routes1.setDst("0.0.0.0/0");
            routes.add(routes1);

            ipam.setRoutes(routes);
            ipam.setGateway("192.168.59.1");

            netCofig.setIpam(ipam);
            netSpec.setConfig(netCofig);
            network.setSpec(netSpec);


            netAttachmentDefClient.createOrReplace(network);

            LOGGER.info("Hobbit Network created: ", network);

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
        int maxLength = POD_MAX_NAME_LENGTH - 1 - uuid.length();
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


    /*
    *  mode is either production or development
    *
    *
    * */

    public void createNewPod(String namespace, String mode, String serviceAccountName,  String podName, String label, String imageName,
                             int replicas, int port) {
        namespace = K8sUtility.defaultNamespace(namespace);

        // Namespace currently being used
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName(namespace).addToLabels("mode", mode).endMetadata().build();
        LOGGER.info("Created namespace", k8sClient.namespaces().createOrReplace(ns));

        ServiceAccount serviceAccount = new ServiceAccountBuilder().withNewMetadata().withName(serviceAccountName).endMetadata().build();


        k8sClient.serviceAccounts().inNamespace(ns.getMetadata().getName()).createOrReplace(serviceAccount);

        Deployment deployment = new DeploymentBuilder()
            .withNewMetadata()
                .withName(podName + "-deployment")
            .endMetadata()
            .withNewSpec()
                .withReplicas(replicas)
                .withNewTemplate()
                    .withNewMetadata()
                        .addToLabels("app", podName)
                        .addToLabels("type", label)
                    .endMetadata()
                    .withNewSpec()
                        .addNewContainer()
                            .withName(podName)
                            .withImage(imageName)
                            .addNewPort()
                            .withContainerPort(port)
                            .endPort()
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .withNewSelector()
                .addToMatchLabels("app", podName)
                .addToMatchLabels("type", label )
            .endSelector()
            .endSpec()
            .build();

        deployment = k8sClient.apps().deployments().inNamespace(namespace).create(deployment);
        LOGGER.info("Deployment Created. \n Pod created", deployment);
    }


    @Override
    public Pod getPod(String namespace, String name) {
        namespace = K8sUtility.defaultNamespace(namespace);
        Pod pod = k8sClient.pods().inNamespace(namespace).withName(name).get();
        return pod;
    }

    @Override
    public Pod getPod(String yaml_path){

        Pod pod = null;
        try {
            pod = k8sClient.pods().load(new FileInputStream(yaml_path)).get();
            return pod;
        } catch (FileNotFoundException e) {
            LOGGER.debug("Pod resource not available: "+ e.getMessage());
            //e.printStackTrace();
            return null;
        }
    }

    @Override
    public PodList getPods(String namespace) {
        namespace = K8sUtility.defaultNamespace(namespace);
        PodList podList = k8sClient.pods().inNamespace(namespace).list();

        return podList;
    }

    @Override
    public PodList getPods() {
        PodList podList = k8sClient.pods().inAnyNamespace().list();
        return podList;
    }

    @Override
    public PodList getPods(String namespace, String label1, String label2) {
        namespace = K8sUtility.defaultNamespace(namespace);

        PodList podList = k8sClient.pods().inNamespace(namespace)
                                    .withLabel(label1, label2)
                                    .list();
        return podList;
    }



    @Override
    public void createPod(String name, String container_name, String image, int port, String namespace) {
        namespace = K8sUtility.defaultNamespace(namespace);

        Pod aPod = new PodBuilder().withNewMetadata().withName(name).endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName(container_name)
            .withImage(image)
            .addNewPort().withContainerPort(port).endPort()
            .endContainer()
            .endSpec()
            .build();
        Pod createdPod = k8sClient.pods().inNamespace(namespace).create(aPod);
    }

    @Override
    public void createOrReplacePod(String namespace, Pod pod) {
        namespace = K8sUtility.defaultNamespace(namespace);
        k8sClient.pods().inNamespace(namespace).createOrReplace(pod);
    }

    @Override
    public void editPodAddLabel(String namespace, String name, String label, String new_label) {
        namespace = K8sUtility.defaultNamespace(namespace);
        k8sClient.pods().inNamespace(namespace).withName(name).edit()
            .editOrNewMetadata().addToLabels(new_label,label).endMetadata().done();
    }

    @Override
    public String getPodLog(String name, String namespace) {
        namespace = K8sUtility.defaultNamespace(namespace);
        String log = k8sClient.pods().inNamespace(namespace).withName(name).getLog();
        return log;
    }

    @Override
    public LogWatch getStats(String podName, String namespace) {
        namespace = K8sUtility.defaultNamespace(namespace);
        LogWatch watch = k8sClient.pods().inNamespace(namespace).withName(podName).tailingLines(10).watchLog(System.out);
        return watch;
    }

    @Override
    public Boolean deletePod(String namespace, String name) {
        namespace = K8sUtility.defaultNamespace(namespace);
        Boolean isDeleted = k8sClient.pods().inNamespace(namespace).withName(name).delete();
        return isDeleted;
    }

    @Override
    public Boolean deletePods(String namespace, List<Pod> pods) {
        namespace = K8sUtility.defaultNamespace(namespace);
        Boolean isDeleted = k8sClient.pods().inNamespace(namespace).delete(pods);
        return isDeleted;
    }

    @Override
    public void uploadToPod(String namespace, Pod pod, String filePath, File file) {
        namespace = K8sUtility.defaultNamespace(namespace);
        k8sClient.pods().inNamespace(namespace).withName(pod.getMetadata().getName())
            .file(filePath).upload(file.toPath());
    }

    @Override
    public String readFromPod(String namespace, Pod pod, String path) {
        namespace = K8sUtility.defaultNamespace(namespace);
        try (InputStream is = k8sClient.pods()
            .inNamespace(namespace)
            .withName(pod.getMetadata().getName())
            .file(path)
            .read())  {
            String result = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
            return result;
        } catch (IOException e) {
            LOGGER.debug(e.getMessage());
            return null;
//            e.printStackTrace();
        }
    }





}
