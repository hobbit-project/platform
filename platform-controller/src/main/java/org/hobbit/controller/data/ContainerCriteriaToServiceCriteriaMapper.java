package org.hobbit.controller.data;

import com.spotify.docker.client.messages.swarm.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Maps Kubernetes-style {@link ContainerCriteria} to Docker Swarm {@link Service.Criteria}
 * in order to enable service discovery or filtering across container orchestration platforms.
 *
 * <p>This is particularly useful in hybrid environments or migration setups where systems
 * originally designed for Kubernetes need to operate within a Docker Swarm context. It allows
 * interoperability by translating container selection logic into the format understood by Docker clients.</p>
 *
 * <h2>Mapping Logic:</h2>
 * <ul>
 *   <li>{@code containerId} → {@code serviceId}</li>
 *   <li>{@code containerName} → {@code serviceName}</li>
 *   <li>{@code labels} → {@code labels}</li>
 * </ul>
 *
 * <p><strong>Note:</strong> This class logs an error and returns {@code null} if the input
 * {@code ContainerCriteria} is {@code null}.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * ContainerCriteria criteria = ContainerCriteria.builder()
 *     .withContainerId("12345")
 *     .withContainerName("my-service")
 *     .withLabel("app", "my-app")
 *     .build();
 *
 * Service.Criteria serviceCriteria = ContainerCriteriaToServiceCriteriaMapper.map(criteria);
 * List<Service> services = dockerClient.listServices(serviceCriteria);
 * }</pre>
 * @author Farshad Afshari farshad.afshari@uni-paderborn.de
 */

public class ContainerCriteriaToServiceCriteriaMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerCriteriaToServiceCriteriaMapper.class);
    public static Service.Criteria map(ContainerCriteria containerCriteria) {
        if (containerCriteria == null) {
            LOGGER.error("containerCriteria is null can not map it to ServiceCriteria");
            return null;
        }

        Service.Criteria.Builder builder = Service.Criteria.builder();

        if (containerCriteria.getContainerId() != null) {
            // map container ID to service
            builder.serviceId(containerCriteria.getContainerId());
            // also is possible to use labels like bellow
            //builder.labels(Collections.singletonMap("container_id", containerCriteria.getContainerId()));
        }

        if (containerCriteria.getContainerName() != null) {
            //map container ID to service ID
            builder.serviceName(containerCriteria.getContainerName());
            //same as ID we can use label if need
            //builder.labels(Collections.singletonMap("container_name", containerCriteria.getContainerName()));
        }

        if (containerCriteria.getLabels() != null && !containerCriteria.getLabels().isEmpty()) {
            builder.labels(containerCriteria.getLabels());
        }

        return builder.build();
    }


    public static void main(String[] args) {
        // Example Usage
        ContainerCriteria criteria = ContainerCriteria.builder()
            .withContainerId("12345")
            .withContainerName("my-service")
            .withLabel("app", "my-app")
            .build();

        Service.Criteria serviceCriteria = map(criteria);

        if (serviceCriteria != null) {
            System.out.println(serviceCriteria.labels()); // Print the labels for verification
        }

        ContainerCriteria criteria2 = ContainerCriteria.builder()
            .withLabels(Collections.singletonMap("env", "prod"))
            .build();

        Service.Criteria serviceCriteria2 = map(criteria2);

        if (serviceCriteria2 != null) {
            System.out.println(serviceCriteria2.labels()); // Print the labels for verification
        }

    }
}
