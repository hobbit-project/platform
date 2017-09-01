/**
 * This file is part of platform-controller.
 *
 * platform-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * platform-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with platform-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
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
    protected static final String[] sleepCommand = {"sleep", "20s"};

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
