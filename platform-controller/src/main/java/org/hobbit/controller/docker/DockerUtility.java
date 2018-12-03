package org.hobbit.controller.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;

public class DockerUtility {
    private static DockerClient dockerClient = null;

    protected DockerUtility() {
        // Exists only to defeat instantiation.
    }

    public static synchronized DockerClient getDockerClient(String host) throws DockerCertificateException {
        if(dockerClient != null && dockerClient.getHost()!=host)
            disposeDockerClient();

        dockerClient = initializeDockerClient(host);

        return dockerClient;
    }

    public static synchronized DockerClient getDockerClient() throws DockerCertificateException {
        if(dockerClient == null) {
            dockerClient = initializeDockerClient(null);
        }
        return dockerClient;
    }

    public static DockerClient initializeDockerClient(String host) throws DockerCertificateException {
        DefaultDockerClient.Builder builder = DefaultDockerClient.fromEnv();
        if(host!=null)
            builder.uri(host);
        builder.connectionPoolSize(5000);
        builder.connectTimeoutMillis(1000);
        return builder.build();
    }

    public static void disposeDockerClient(){
        dockerClient.close();
        dockerClient = null;
    }
}
