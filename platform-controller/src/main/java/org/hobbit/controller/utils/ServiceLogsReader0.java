//package org.hobbit.controller.utils;
//
//import com.spotify.docker.client.DockerClient;
//import com.spotify.docker.client.LogStream;
//import com.spotify.docker.client.messages.swarm.Task;
//import org.hobbit.controller.docker.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.*;
//
//
///**
// * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
// */
//
//
//
//public class ServiceLogsReader implements ContainerStateObserver {
//    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLogsReader.class);
//    private Logger logger;
//
//
//    private Map<String, Integer> displayedLogsLength;
//    private Map<String, String> taskServiceMapping;
//    private Map<String, String> taskNodeMapping;
//    private Map<String, String> serviceImageMapping;
//
//
//
//    private List<String> monitoredTasks;
//    private List<ContainerTerminationCallback> terminationCallbacks;
//    private ContainerManager manager;
//    private int repeatInterval;
//    private Timer timer;
//
//    public ServiceLogsReader(ContainerManager manager, int repeatInterval) {
//        this.manager = manager;
//        this.repeatInterval = repeatInterval;
//        monitoredTasks = new ArrayList<>();
//        timer = new Timer();
//        displayedLogsLength = new HashMap<>();
//        taskServiceMapping = new HashMap<>();
//        serviceImageMapping = new HashMap<>();
//        taskNodeMapping = new HashMap<>();
//    }
//
//    @Override
//    public void startObserving() {
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                String taskIds[] = null;
//                // copy the list of containers so that we don't have to care for
//                // access conflicts with other threads after this point
//                synchronized (monitoredTasks) {
//                    taskIds = monitoredTasks.toArray(new String[monitoredTasks.size()]);
//                }
//
//                for (String taskId : taskIds){
//
//                    if(!taskServiceMapping.containsKey(taskId)){
//                        Task task = manager.inspectTask(taskId);
//                        if(task!=null){
//                            String serviceId = task.serviceId();
//                            if (serviceId != null && serviceId.length() > 0){
//                                taskServiceMapping.put(taskId, serviceId);
//                                taskNodeMapping.put(serviceId, task.nodeId());
//                            }
//                        }
//                    }
//
//                    if(!taskServiceMapping.containsKey(taskId))
//                        continue;
//
//                    String serviceId = taskServiceMapping.get(taskId);
//                    if(!serviceImageMapping.containsKey(serviceId)){
//                        try {
//                            String imageName = manager.listServices().stream().filter(s -> s.id().equals(serviceId)).findFirst().get().spec().taskTemplate().containerSpec().image();
//                            String[] splitted = imageName.split("/");
//                            serviceImageMapping.put(serviceId, splitted[splitted.length - 1]);
//                        }
//                        catch (Exception e){
//                            logger.warn("Failed to get imageName by service id: {}", e.getLocalizedMessage());
//                        }
//                    }
//
//
//                    LogStream logStream = null;
//
//                    String logs = "";
//                    try {
//                        String loggerName = "service."+serviceImageMapping.get(serviceId)+" (id="+serviceId+"_node="+taskNodeMapping.get(serviceId)+")";
//                        logs="";
//                        try {
//                            logStream = manager.serviceLogs(serviceId,
//                                    DockerClient.LogsParam.stderr(),
//                                    DockerClient.LogsParam.stdout()
//                                    //DockerClient.LogsParam.since(readLogsSince)
//                            );
//                            if(logStream==null) {
//                                removedObservedContainer(taskId);
//                                return;
//                            }
//                            logs = logStream.readFully();
//                        } catch (Exception e) {
//                            LOGGER.warn("No service logs are available {}", loggerName, e);
//                        } finally {
//                            if (logStream != null) {
//                                logStream.close();
//                            }
//                        }
//
//                        int prevLogsLength = (displayedLogsLength.containsKey(serviceId)? displayedLogsLength.get(serviceId):0);
//                        if (logs.length() > prevLogsLength) {
//                            logger = LoggerFactory.getLogger(loggerName);
//                            String logsToPrint = logs.substring(prevLogsLength);
//                            String[] splitted = logsToPrint.split("\n");
//                            String imageName = serviceImageMapping.get(serviceId);
//                            if(imageName.toString().equals("null")){
//                                String test="123";
//                            }
//                            for(String line : splitted){
//                                System.out.println(imageName+" (id="+serviceId+"): "+line);
//                                //logger.debug(line);
//                            }
//                            displayedLogsLength.put(serviceId, logs.length());
//                        }
//
//                    }
//                    catch (Exception e){
//                        LOGGER.error("Failed to process logs for service {}: {}", serviceId, e.getMessage());
//                    }
////
//
//                }
//            }
//        }, repeatInterval, repeatInterval);
//    }
//
//    @Override
//    public void stopObserving() {
//        timer.cancel();
//        timer.purge();
//    }
//
//    @Override
//    public void addTerminationCallback(ContainerTerminationCallback callback) {
//
//    }
//
//    @Override
//    public void removeTerminationCallback(ContainerTerminationCallback callback) {
//
//    }
//
//    @Override
//    public void addObservedContainer(String taskId) {
//        synchronized (monitoredTasks) {
//
//            if (monitoredTasks.contains(taskId)){
//
//                return;
//            }
//            // if not - add
//            monitoredTasks.add(taskId);
//        }
//    }
//
//    @Override
//    public void removedObservedContainer(String taskId) {
//        synchronized (monitoredTasks) {
//            if(taskServiceMapping.containsKey(taskId)) {
//                String serviceId = taskServiceMapping.get(taskId);
//                serviceImageMapping.remove(serviceId);
//                if (displayedLogsLength.containsKey(serviceId))
//                    displayedLogsLength.remove(serviceId);
//                taskServiceMapping.remove(taskId);
//            }
//            monitoredTasks.remove(taskId);
//        }
//    }
//
//    @Override
//    public List<String> getObservedContainers() {
//        synchronized (monitoredTasks) {
//            return new ArrayList<String>(monitoredTasks);
//        }
//    }
//}