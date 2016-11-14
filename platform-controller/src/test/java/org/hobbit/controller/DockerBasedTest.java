package org.hobbit.controller;

import java.util.List;

import org.junit.Before;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;

/**
 * Created by Timofey Ermilov on 02/09/16.
 */
public class DockerBasedTest {
    protected DockerClient dockerClient;
    protected static final String busyboxImageName = "busybox";
    protected static final String[] sleepCommand = {"sleep", "10000"};

    protected boolean findImageWithTag(final String id, final List<Image> images) {
        for (Image image : images) {
            for (String tag : image.getRepoTags()) {
                if (tag.contains(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Before
    public void initClient() {
        DockerClientConfig cfg = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        dockerClient = DockerClientBuilder.getInstance(cfg).build();

        // check if busybox is present
        List<Image> images = dockerClient.listImagesCmd().withShowAll(true).exec();
        if (!findImageWithTag(busyboxImageName, images)) {
            dockerClient.pullImageCmd(busyboxImageName).withTag("latest").exec(new PullImageResultCallback()).awaitSuccess();
        }
    }
}
