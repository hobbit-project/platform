package org.hobbit.controller.data;

import java.util.Map;
import java.util.HashMap;

public class ContainerCriteria {
    protected String containerId;
    protected String containerName;
    protected Map<String, String> labels;

    public static class Builder {
        private String containerId;
        private String containerName;
        private Map<String, String> labels = new HashMap<>();

        public Builder withContainerId(String containerId) {
            this.containerId = containerId;
            return this;
        }

        public Builder withContainerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public Builder withLabels(Map<String, String> labels) {
            this.labels.putAll(labels);
            return this;
        }

        public Builder withLabel(String key, String value) {
            this.labels.put(key, value);
            return this;
        }


        public ContainerCriteria build() {
            ContainerCriteria criteria = new ContainerCriteria();
            criteria.containerId = this.containerId;
            criteria.containerName = this.containerName;
            criteria.labels = new HashMap<>(this.labels); // Create a copy to prevent external modification
            return criteria;
        }
    }


    public String getContainerId() {
        return containerId;
    }

    public String getContainerName() {
        return containerName;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public static Builder builder() {
        return new Builder();
    }


    @Override
    public String toString() {
        return "ContainerCriteria{" +
            "containerId='" + containerId + '\'' +
            ", containerName='" + containerName + '\'' +
            ", labels=" + labels +
            '}';
    }
}

