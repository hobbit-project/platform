package org.hobbit.controller.docker;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;

public class ImageManagerFacade implements ImageManager {

    private List<ImageManager> managers;

    @Override
    public List<BenchmarkMetaData> getBenchmarks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<SystemMetaData> getSystems() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<SystemMetaData> getSystemsOfUser(String userName) {
        // TODO Auto-generated method stub
        return null;
    }
}
