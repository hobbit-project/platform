package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.log4j.Level;
import org.hobbit.controller.docker.ContainerStateObserverImpl;
import org.hobbit.controller.docker.ContainerTerminationCallback;
import org.hobbit.controller.docker.ContainerTerminationCallbackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PodStateObserverImpl implements PodStateObserver{

    private static final Logger LOGGER = LoggerFactory.getLogger(PodStateObserverImpl.class);

    private List<PodTerminationCallback> terminationCallbacks;

    private PodsManager manager;

    private Timer timer;

    private Watch watch;

    private KubernetesClient k8sClient;

    final CountDownLatch closeLatch = new CountDownLatch(1);

    public PodStateObserverImpl(PodsManager manager) {
        this.k8sClient = K8sUtility.getK8sClient();;
        this.manager = manager;
        terminationCallbacks = new ArrayList<>();
        timer = new Timer();
    }

    @Override
    public void startObserving() {

        watch = k8sClient.pods().inAnyNamespace().watch(new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod pod) {
                LOGGER.info( action.name() + " " + pod.getMetadata().getName());

                switch (action.name()) {
                    case "ADDED":
                        LOGGER.info(pod.getMetadata().getName() + " pod got added");
                        break;
                    case "DELETED":
                        LOGGER.info(pod.getMetadata().getName() + "pod got deleted");
                        PodTerminationCallback cb_delete = new PodTerminationCallbackImpl();
                        cb_delete.notifyTermination(pod.getMetadata().getName(), action.name());
                        break;
                    case "MODIFIED":
                        LOGGER.info(pod.getMetadata().getName() + "pod got modified");
                        break;
                    default:
                        LOGGER.error("Unrecognized event: " + action.name());
                        PodTerminationCallback cb_default = new PodTerminationCallbackImpl();
                        cb_default.notifyTermination(pod.getMetadata().getName(), action.name());
                }
            }

            @Override
            public void onClose(KubernetesClientException e) {
                LOGGER.info("Observation: Watcher onClose");
                closeLatch.countDown();
            }
        });
    }

    @Override
    public void stopObserving() {
        try {
            watch.close();
            closeLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.debug(e.getMessage());
        }
    }

    /*
    @Override
    public void addTerminationCallback(PodTerminationCallback callback) {

    }

    @Override
    public void removeTerminationCallback(PodTerminationCallback callback) {

    }

    @Override
    public void addObservedPod(String podId) {

    }

    @Override
    public void removedObservedPod(String cpodId) {

    }
    */

    @Override
    public PodList getObservedPods() {
        return k8sClient.pods().inAnyNamespace().list();
    }

}
