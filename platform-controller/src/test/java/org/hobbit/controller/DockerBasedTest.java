package org.hobbit.controller;

import java.util.List;

import org.junit.Before;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Image;

/**
 * Created by Timofey Ermilov on 02/09/16.
 */
public class DockerBasedTest {
    protected DockerClient dockerClient;
    protected static final String busyboxImageName = "busybox:latest";
    protected static final String[] sleepCommand = {"sleep", "10000"};

    protected boolean findImageWithTag(final String id, final List<Image> images) {
        for (Image image : images) {
            for (String tag : image.repoTags()) {
                if (tag.contains(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Before
    public void initClient() throws Exception {
        dockerClient = DefaultDockerClient.fromEnv().build();

        // check if busybox is present
        List<Image> images = dockerClient.listImages(DockerClient.ListImagesParam.allImages());
        if (!findImageWithTag(busyboxImageName, images)) {
            dockerClient.pull(busyboxImageName);
        }
    }
}
