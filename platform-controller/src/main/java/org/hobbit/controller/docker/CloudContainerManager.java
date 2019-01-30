package org.hobbit.controller.docker;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ExecCreation;
import com.spotify.docker.client.messages.swarm.Node;
import com.spotify.docker.client.messages.swarm.Task;
import org.hobbit.controller.cloud.CloudSshTunnelsProvider;
import org.hobbit.controller.cloud.DockerClientProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */

public class CloudContainerManager extends ContainerManagerImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudClusterManager.class);


    public CloudContainerManager(ClusterManager clusterManager) throws Exception {
        super(clusterManager);

    }

    @Override
    public DockerClient getDockerClient(){
        return  DockerClientProvider.getDockerClient();
    }


    @Override
    public String getContainerId(String name){
        if(getDockerClient()==null)
            return null;
        return super.getContainerId(name);
    }

    public void abc (String serviceName){

    }

    @Override
    public boolean execAsyncCommand(String serviceName, String[] command){
        boolean ret = false;
        Semaphore initFinishedMutex = new Semaphore(0);
        try {

//            String taskId = containerToTaskMapping.get(serviceName);
            List<Task> tasksList = getDockerClient().listTasks(Task.Criteria.builder().serviceName(serviceName).build());
            //Task task = inspectTask(taskId);
            Task task = tasksList.get(0);
            String containerId = task.status().containerStatus().containerId().substring(0,12);

            Node node = getDockerClient().listNodes(Node.Criteria.builder().nodeId(task.nodeId()).build()).get(0);
            String nodeIp = node.status().addr();

            CloudSshTunnelsProvider.execAsyncCommand(nodeIp, new Function<DockerClient, String>() {
                @Override
                public String apply(DockerClient dockerClient) {
                    try {
                        ExecCreation execCreation = dockerClient.execCreate(containerId, command, DockerClient.ExecCreateParam.detach());
                        dockerClient.execStart(execCreation.id());
                        initFinishedMutex.release();
                    } catch (Exception e) {
                        LOGGER.error("Failed to execute the command: {}", e.getLocalizedMessage());
                    }

                    return null;
                }
            });
            initFinishedMutex.acquire();
            ret = true;
        } catch (Exception e) {
            LOGGER.error("Failed to execute the command: {}", e.getLocalizedMessage());
        }
        return ret;
    }
}
