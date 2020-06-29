package org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;


public class Config implements KubernetesResource {

    private String cniVersion;
    private String type;
    private String master;
    private String mode;
    private Ipam ipam;

    public String getCniVersion() {
        return cniVersion;
    }

    public void setCniVersion(String cniVersion) {
        this.cniVersion = cniVersion;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Ipam getIpam() {
        return ipam;
    }

    public void setIpam(Ipam ipam) {
        this.ipam = ipam;
    }


    @Override
    public String toString() {
        return "Config{" +
            "cniVersion='" + cniVersion + '\'' +
            ", type='" + type + '\'' +
            ", master='" + master + '\'' +
            ", mode='" + mode + '\'' +
            ", ipam=" + ipam +
            '}';
    }
}
