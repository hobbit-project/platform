package org.hobbit.controller.cloud;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import org.hobbit.awscontroller.SSH.HSession;
import org.hobbit.awscontroller.SSH.SshConnector;
import org.hobbit.awscontroller.SSH.SshTunnelsProvider;
import org.hobbit.cloud.interfaces.Node;
import org.hobbit.controller.PlatformController;
import org.hobbit.controller.cloud.aws.swarm.SwarmClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import static org.hobbit.controller.docker.CloudClusterManager.CLOUD_SSH_KEY_FILE_PATH_KEY;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */
public class CloudSshTunnelsProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudSshTunnelsProvider.class);
    static PlatformController _platformController;
    static String bastionHostUser = "ec2-user";
    static String targetHostUser = "ubuntu";
    static String keyfilepath;
    static String[] portsToForward = new String[]{ "2376", "5672", "9090" };
    static int defaultSshPort = 22;
    static Boolean providerInitilized=false;
    private static Semaphore initFinishedMutex = new Semaphore(0);
    private static HSession bastionHostSession;

    public CloudSshTunnelsProvider(PlatformController platformController){

        _platformController = platformController;
        keyfilepath = System.getenv(CLOUD_SSH_KEY_FILE_PATH_KEY);
    }

//    public static SshTunnelsProvider getTunnelsProvider(){
//        return SshTunnelsProvider;
////        if(sshTunnelsProvider)
////        return sshTunnelsProvider;
//    }

    public static Boolean isProviderInitialized() {
        return providerInitilized;
    }

    public static Boolean isConnected() {
        return SshTunnelsProvider.isConnected();
    }

    public static void initSshProvider(){

        providerInitilized = false;

        SwarmClusterManager clusterManager = ClusterManagerProvider.getManager();

        Node managerHost = null;
        try {
            List<Node> managers = clusterManager.getManagerNodes();
            managerHost = managers.get(0);
        } catch (Exception e) {
            LOGGER.info("Unable to get manager nodes: {}", e.getLocalizedMessage());
            return;
        }

        Node bastionHost = null;
        try {
            bastionHost = clusterManager.getBastion();
        } catch (Exception e) {
            LOGGER.error("Could not get bastion host: {}", e.getLocalizedMessage());
            return;
        }

        String bastionHostIp = bastionHost.getPublicIpAddress();
        String managerHostIp = managerHost.getIngernalIpAddress();

        LOGGER.info("initSshConnection {}, {}", bastionHostIp, managerHostIp);

        bastionHostSession = new HSession(bastionHostUser, bastionHostIp, defaultSshPort, keyfilepath);
        HSession swarmManagerSession = new HSession(targetHostUser, managerHostIp, defaultSshPort, keyfilepath, portsToForward, bastionHostSession);

        SshTunnelsProvider.init(swarmManagerSession, new Function<HSession, String>(){
            @Override
            public String apply(HSession hSession){
                initFinishedMutex.release();
                Map<Integer, Integer> portForwadings = hSession.getForwardings();
                LOGGER.info("SSH connection to {} established. Ports forwardings: {}", hSession.getHost(), portForwadings.toString());
                try {
                    String rabbitHost = "localhost:" + portForwadings.get(5672);
                    LOGGER.info("Switching platform controller to remote rabbitMQ {}", rabbitHost);
                    _platformController.switchCmdToExpRabbit(rabbitHost);
                } catch (Exception e) {
                    LOGGER.error("Cannot switch platform controller {}", e.getLocalizedMessage());
                }

                try {
                    LOGGER.info("Switching resInfoCollector to {}", "localhost:" + portForwadings.get(9090).toString());
                    _platformController.setPrometheusHost("localhost");
                    _platformController.setPrometheusPort(portForwadings.get(9090).toString());
                } catch (Exception e) {
                    LOGGER.error("Cannot switch resInfoCollector {}", e.getLocalizedMessage());
                }
                return null;
            }
        }, new Function<HSession, String>() {
            @Override
            public String apply(HSession hSession){
                providerInitilized = false;
                DockerClientProvider.flushDockerClient();
                try {
                    LOGGER.info("Switching platform controller to local rabbitMQ");
                    _platformController.switchCmdToExpRabbit(null);
                } catch (Exception e) {
                    LOGGER.error("Cannot switch platform controller {}", e.getLocalizedMessage());
                }
                return null;
            }
        });
        providerInitilized = true;

    }

    public static void newSshTunnel(Function<HSession, String> onConnectHandler, Function<HSession, String> onDisconnectConnectHandler){
        SshTunnelsProvider.newSshTunnel(onConnectHandler, onDisconnectConnectHandler);
    }

    public static void execAsyncCommand(String nodeIP, Function<DockerClient, String> onConnect) {

        SshConnector sshConnector = SshTunnelsProvider.getSshConnector();
        HSession targetHostSession;
        if(sshConnector.getOpenedConnections().containsKey(nodeIP))
            targetHostSession = sshConnector.getOpenedConnections().get(nodeIP);
        else
            targetHostSession = new HSession(targetHostUser, nodeIP, defaultSshPort, keyfilepath, portsToForward, bastionHostSession);

        try {
            sshConnector.openTunnel(targetHostSession, 30000, new Function<HSession, String>() {
                @Override
                public String apply(HSession hSession) {
                    Map<Integer, Integer> portForwadings = hSession.getForwardings();
                    String dockerHost = "http://localhost:" + portForwadings.get(2376);
                    try {
                        DockerClient dockerClient = DefaultDockerClient.fromEnv()
                                .uri(dockerHost)
                                .connectionPoolSize(5000)
                                .connectTimeoutMillis(1000).build();
                        onConnect.apply(dockerClient);
                        dockerClient.close();
                    } catch (DockerCertificateException e) {
                        LOGGER.error("Failed to init docker client to {}: {}", dockerHost, e.getLocalizedMessage());
                    }

                    return null;
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to open tunnel to {}: {}", nodeIP, e.getLocalizedMessage());
        }

    }


}
