package org.hobbit.controller.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;

public class DockerUtility {
    public static DockerClient getDockerClient() throws DockerCertificateException {
        DefaultDockerClient.Builder builder = DefaultDockerClient.fromEnv();
        builder.connectionPoolSize(5000);
        builder.connectTimeoutMillis(1000);
        return builder.build();
    }
}
