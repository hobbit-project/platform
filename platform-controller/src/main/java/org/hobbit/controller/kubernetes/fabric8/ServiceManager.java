package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;

public interface ServiceManager {

    Service getService(String yaml_file);

    Service getService(String namespace, String name);

    Service createService(String serviceName, String protocol, String portName,
                          int port, int targetPort,String type, String IP, String namespace);

    ServiceList getServices();

    ServiceList getServices(String namespace, String label1, String label2);

    Boolean deleteService(String namespace, String name);

}
