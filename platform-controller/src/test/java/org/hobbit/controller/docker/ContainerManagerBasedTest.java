package org.hobbit.controller.docker;

import java.util.ArrayList;
import java.util.List;

import org.hobbit.controller.DockerBasedTest;
import org.junit.After;
import org.junit.Before;

/**
 * Created by Timofey Ermilov on 01/09/16.
 */
public class ContainerManagerBasedTest extends DockerBasedTest {
    protected ContainerManagerImpl manager;
    protected List<String> containers = new ArrayList<String>();

    @Before
    public void initManager() {
        manager = new ContainerManagerImpl();
    }

    @After
    public void cleanUp() {
        for (String containerId : containers) {
            try {
                dockerClient.stopContainerCmd(containerId).exec();
            } catch (Exception e) {
            }
            try {
                dockerClient.removeContainerCmd(containerId).exec();
            } catch (Exception e) {
            }
        }
    }
}
