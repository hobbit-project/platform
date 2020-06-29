package org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize(
    using = JsonDeserializer.None.class
)
public class Spec implements KubernetesResource {
    private Config config;

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "Spec{" +
            "config=" + config +
            '}';
    }
}
