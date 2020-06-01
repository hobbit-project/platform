package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.Service;

public interface ServiceManager {

    Service getService(String yaml_file);

    Service getService(String namespace, String name);

    Service createService(String serviceName, String protocol, String portName,
                          int port, int targetPort,String type, String IP, String namespace);



}
