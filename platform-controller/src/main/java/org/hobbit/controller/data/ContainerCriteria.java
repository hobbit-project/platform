package org.hobbit.controller.data;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents criteria used to identify or filter containers, based on container ID,
 * container name, and labels. This class is typically used in scenarios where specific
 * container characteristics need to be matched, such as during orchestration or monitoring
 * in containerized environments.
 *
 * <p>Instances of this class are immutable and should be created using the
 * {@link ContainerCriteria.Builder}.</p>
 */
public class ContainerCriteria {

    /**
     * The unique identifier of the container.
     */
    protected String containerId;

    /**
     * The name of the container.
     */
    protected String containerName;

    /**
     * A map of labels associated with the container.
     */
    protected Map<String, String> labels;

    /**
     * Builder class for constructing {@link ContainerCriteria} instances in a flexible and
     * readable manner.
     */
    public static class Builder {
        private String containerId;
        private String containerName;
        private Map<String, String> labels = new HashMap<>();

        /**
         * Sets the container ID.
         *
         * @param containerId the unique container ID
         * @return the builder instance
         */
        public Builder withContainerId(String containerId) {
            this.containerId = containerId;
            return this;
        }

        /**
         * Sets the container name.
         *
         * @param containerName the name of the container
         * @return the builder instance
         */
        public Builder withContainerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        /**
         * Adds a map of labels to the container criteria.
         *
         * @param labels a map of label key-value pairs
         * @return the builder instance
         */
        public Builder withLabels(Map<String, String> labels) {
            this.labels.putAll(labels);
            return this;
        }

        /**
         * Adds a single label key-value pair to the container criteria.
         *
         * @param key the label key
         * @param value the label value
         * @return the builder instance
         */
        public Builder withLabel(String key, String value) {
            this.labels.put(key, value);
            return this;
        }

        /**
         * Builds and returns a {@link ContainerCriteria} instance with the configured properties.
         *
         * @return a new {@link ContainerCriteria} instance
         */
        public ContainerCriteria build() {
            ContainerCriteria criteria = new ContainerCriteria();
            criteria.containerId = this.containerId;
            criteria.containerName = this.containerName;
            criteria.labels = new HashMap<>(this.labels); // Create a copy to prevent external modification
            return criteria;
        }
    }

    /**
     * Returns the container ID.
     *
     * @return the container ID, or {@code null} if not set
     */
    public String getContainerId() {
        return containerId;
    }

    /**
     * Returns the container name.
     *
     * @return the container name, or {@code null} if not set
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * Returns the labels associated with the container.
     *
     * @return a map of label key-value pairs
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Creates a new {@link Builder} instance to construct {@link ContainerCriteria} objects.
     *
     * @return a new {@link Builder}
     */
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
