package org.hobbit.controller.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.NamespaceList;

public interface NamespaceManager {

    static final String APP_CATEGORY_KEY = "app_category";
    static final String APP_CATEGORY_VALUE = "platform_controller";

    NamespaceList getNamespaces();

    Boolean deleteNamespaceResources();

}
