package org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources;

import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.List;

public class Ipam implements KubernetesResource {

    private String type;
    private String subnet;
    private String rangeStart;
    private String rangeEnd;
    private List<Routes>  routes;
    private String gateway;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(String rangeStart) {
        this.rangeStart = rangeStart;
    }

    public String getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(String rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public List<Routes> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Routes> routes) {
        this.routes = routes;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    @Override
    public String toString() {
        return "Ipam{" +
            "type='" + type + '\'' +
            ", subnet='" + subnet + '\'' +
            ", rangeStart='" + rangeStart + '\'' +
            ", rangeEnd='" + rangeEnd + '\'' +
            ", routes=" + routes +
            ", gateway='" + gateway + '\'' +
            '}';
    }
}
