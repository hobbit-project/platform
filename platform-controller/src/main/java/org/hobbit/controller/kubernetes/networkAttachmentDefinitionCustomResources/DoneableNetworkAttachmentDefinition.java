package org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableNetworkAttachmentDefinition extends CustomResourceDoneable<NetworkAttachmentDefinition> {

    public DoneableNetworkAttachmentDefinition(NetworkAttachmentDefinition resource, Function<NetworkAttachmentDefinition, NetworkAttachmentDefinition> function) {
        super(resource, function);
    }
}
