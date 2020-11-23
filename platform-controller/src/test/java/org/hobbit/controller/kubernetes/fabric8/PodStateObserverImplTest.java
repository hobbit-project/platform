package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import static io.fabric8.kubernetes.client.Watcher.Action.DELETED;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PodStateObserverImplTest extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PodStateObserverImplTest.class);

    private static final Long EVENT_WAIT_PERIOD_MS = 10L;

    @Rule
    public KubernetesServer server = new KubernetesServer(false);


    private PodStateObserverImpl observer;
    private Throwable throwable = null;
    private PodsManager manager;

    private KubernetesClient client;
    private Pod pod1;


    @Override
    public void setUp() throws Exception {
        manager = new K8sContainerManagerImpl();
        observer = new PodStateObserverImpl(manager);

        pod1 = new PodBuilder().withNewMetadata().withNamespace("test").withName("pod1")
            .withResourceVersion("1").endMetadata().build();
    }

    @Test
    public void testStartObserving() {


        // Given
        server.expect()
            .withPath("/api/v1/namespaces/test/pods?fieldSelector=metadata.name%3Dpod1&resourceVersion=1&watch=true")
            .andUpgradeToWebSocket().open()
            .waitFor(EVENT_WAIT_PERIOD_MS).andEmit(new WatchEvent(pod1, "DELETED"))
            .waitFor(EVENT_WAIT_PERIOD_MS).andEmit(outdatedEvent()).done().once();
        final CountDownLatch deleteLatch = new CountDownLatch(1);
        final CountDownLatch closeLatch = new CountDownLatch(1);
        final Watcher<Pod> watcher = new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod resource) {
                if (action != DELETED) {
                    fail();
                }
                deleteLatch.countDown();
            }

            @Override
            public void onClose(KubernetesClientException cause) {
                assertEquals(410, cause.getCode());
                closeLatch.countDown();
            }
        };
        // When
        try (Watch watch = client.pods().withName("pod1").withResourceVersion("1").watch(watcher)) {
            // Then
            assertNotNull(watch);
            assertTrue(deleteLatch.await(10, TimeUnit.SECONDS));
            assertTrue(closeLatch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static WatchEvent outdatedEvent() {
        return new WatchEventBuilder().withStatusObject(
            new StatusBuilder().withCode(HttpURLConnection.HTTP_GONE)
                .withMessage(
                    "410: The event in requested index is outdated and cleared (the requested history has been cleared [3/1]) [2]")
                .build()).build();
    }
}
