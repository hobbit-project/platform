package org.hobbit.controller.docker;

import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContainerVolumeTest extends ContainerManagerBasedTest {
    @Test
    public void volumeLifecycle() throws Exception {
        dockerClient.pull("nginx:1.11");
        final Volume toCreate = Volume.builder()
                .name("volumeName")
                .driver("local")
                .labels(ImmutableMap.of("foo", "bar"))
                .build();
        final Volume created = dockerClient.createVolume(toCreate);

        assertTrue(volumeExists(created.name()));

        final HostConfig hostConfig = HostConfig.builder().build();

        final ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image("nginx:1.11")
                .cmd("sh", "-c", "while :; do sleep 1; done")
                .addVolume(created.name())
                .build();

        final ContainerCreation creation = dockerClient.createContainer(containerConfig);
        final String id = creation.id();

        // Inspect container
        final ContainerInfo info = dockerClient.inspectContainer(id);
        dockerClient.startContainer(id);
        dockerClient.killContainer(id);
        dockerClient.removeContainer(id);

        dockerClient.removeVolume("volumeName");

        assertFalse(volumeExists(created.name()));
    }

    private boolean volumeExists(String volumeName) throws DockerException, InterruptedException {
        final VolumeList volumeList = dockerClient.listVolumes();
        final List<Volume> volumes = volumeList.volumes();
        for(Volume volume : volumes) {
            if(volume.name().equals(volumeName))
                return true;
        }
        return false;
    }
}
