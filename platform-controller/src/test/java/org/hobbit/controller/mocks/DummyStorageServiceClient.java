package org.hobbit.controller.mocks;

import org.apache.jena.rdf.model.Model;
import org.hobbit.storage.client.StorageServiceClient;

public class DummyStorageServiceClient extends StorageServiceClient {

    public Model insertedModel;

    public DummyStorageServiceClient() {
        super(null);
    }

    @Override
    public boolean sendInsertQuery(Model model, String graphURI) {
        insertedModel = model;
        return true;
    }

}
