package org.hobbit.controller.cloud;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import org.hobbit.awscontroller.SSH.HSession;
import org.hobbit.awscontroller.SSH.SshTunnelsProvider;
import org.hobbit.controller.docker.DockerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */

public class DockerClientProvider {

    private static Logger LOGGER = LoggerFactory.getLogger(DockerClientProvider.class);
    static DockerClient dockerClient;
    static String dockerHost;
    private static boolean alreadyRequested =false;
    private static Semaphore initFinishedMutex = new Semaphore(0);


    public static DockerClient getDockerClient(){
        if(dockerClient!=null && SshTunnelsProvider.isConnected()){
            try {
                dockerClient.ping();
                return dockerClient;
            }
            catch (Exception e){
                LOGGER.info("Failed to ping docker host. Closing existing dockerClient", e.getLocalizedMessage());
                dockerClient.close();

                if(dockerHost!=null){
                    LOGGER.info("Trying to get docker client to host {}", dockerHost);
                    try {
                        dockerClient = DockerUtility.getDockerClient(dockerHost);
                        dockerClient.ping();
                        return dockerClient;
                    } catch (Exception e2) {
                        LOGGER.error("Failed to connect to existing docker client at {}: {}. Closing the existing client", dockerHost, e2.getLocalizedMessage());
                        dockerClient.close();
                    }
                }
            }
        }

        if(!alreadyRequested){
            alreadyRequested = true;

            if(!CloudSshTunnelsProvider.isProviderInitialized()) {
                LOGGER.debug("Initilizing ssh provider");
                CloudSshTunnelsProvider.initSshProvider();
            }

            if(CloudSshTunnelsProvider.isProviderInitialized()){
                LOGGER.debug("Ssh tunnel required. Trying to create it");
                CloudSshTunnelsProvider.newSshTunnel(new Function<HSession, String>() {
                    @Override
                    public String apply(HSession hSession) {
                        Map<Integer, Integer> portForwadings = hSession.getForwardings();
                        dockerHost = "http://localhost:" + portForwadings.get(2376);
                        try {
                            dockerClient = DockerUtility.getDockerClient(dockerHost);
                            LOGGER.info("Initialized new docker client to {}", dockerHost);
                            initFinishedMutex.release();

                        } catch (DockerCertificateException e) {
                            LOGGER.error("Could not init new docker client: {}", e.getLocalizedMessage());
                        }
                        return null;
                    }
                }, new Function<HSession, String>() {
                    @Override
                    public String apply(HSession hSession) {
                        flushDockerClient();
                        return null;
                    }
                });
            }

        }else {
            LOGGER.debug("Waiting ssh tunnel from other thread");
            try {
                initFinishedMutex.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        alreadyRequested =false;

        if(dockerClient==null)
            LOGGER.warn("Returning dockerClient=null");
        return dockerClient;
    }


    public static void flushDockerClient(){
        LOGGER.debug("Flushing docker client");
        dockerClient = null;
    }

}
