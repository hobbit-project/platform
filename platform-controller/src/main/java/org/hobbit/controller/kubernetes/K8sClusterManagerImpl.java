package org.hobbit.controller.kubernetes;

import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.proto.V1;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.ClientBuilder;
import org.hobbit.controller.docker.ClusterManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class K8sClusterManagerImpl implements K8sClusterManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerImpl.class);

    private ApiClient k8sclient;
    private CoreV1Api api;
    private SharedInformerFactory factory;

    private SharedIndexInformer<V1Node> nodeInformer;

    public K8sClusterManagerImpl() throws IOException, ApiException {
        k8sclient = ClientBuilder.cluster().build();
        Configuration.setDefaultApiClient(k8sclient);
        api = new CoreV1Api();

        factory = new SharedInformerFactory();

        nodeInformer =
            factory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> {
                    return api.listNodeCall(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        params.resourceVersion,
                        params.timeoutSeconds,
                        params.watch,
                        null);
                },
                V1Node.class,
                V1NodeList.class);
    }


    @Override
    public V1PodList getPodsInfo() throws ApiException {

        V1PodList list = api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
        // k8sclient = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();
        // Parameters are currently commented because I do not know the right values yet

        return list;
    }

    @Override
    public long getNumberOfNodes() {
        // Node informer


        Lister<V1Node> nodeLister = new Lister<V1Node>(nodeInformer.getIndexer());

        return nodeLister.list().size();
    }

    @Override
    public int getNumberOfNodes(String label) {

        Lister<V1Node> nodeLister = new Lister<V1Node>(nodeInformer.getIndexer());
        V1Node node = nodeLister.get(label);

        if (node != null)
            return 1;
        return 0;
    }
}
