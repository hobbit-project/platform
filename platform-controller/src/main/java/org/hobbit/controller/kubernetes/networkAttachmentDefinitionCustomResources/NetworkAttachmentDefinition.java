package org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;


public class NetworkAttachmentDefinition extends CustomResource {

    private Spec spec;

    public Spec getSpec() {
        return spec;
    }

    public void setSpec(Spec spec) {
        this.spec = spec;
    }

    @Override
    public String getKind() {
        return super.getKind();
    }

    @Override
    public String getApiVersion() {
        return "k8s.cni.cncf.io/v1";
    }

    @Override
    public ObjectMeta getMetadata() {
        return super.getMetadata();
    }


    @Override
    public String toString() {
        return "NetworkAttachmentDefinition{" +
            "apiVersion='" + getApiVersion() + "'" +
            ", metadata=" + getMetadata() +
            ", spec=" + spec +
            //", status=" + status +
            "}";
    }
}
