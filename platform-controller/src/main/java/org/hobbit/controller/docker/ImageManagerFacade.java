package org.hobbit.controller.docker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.ImageMetaData;
import org.hobbit.core.data.SystemMetaData;

/**
 * This Facade is able to manage one or more {@link ImageManager} instances and
 * handles the problem of benchmarks or systems sharing the same URI (see
 * https://github.com/hobbit-project/platform/issues/136).
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class ImageManagerFacade extends AbstactImageManager implements ImageManager {

    private List<ImageManager> managers;

    public ImageManagerFacade(ImageManager... managers) {
        this(Arrays.asList(managers));
    }

    public ImageManagerFacade(List<ImageManager> managers) {
        super();
        this.managers = managers;
    }

    @Override
    protected List<BenchmarkMetaData> getUncheckedBenchmarks() {
        return getImages(true);
    }

    @Override
    protected List<SystemMetaData> getUncheckedSystems() {
        return getImages(false);
    }

    @SuppressWarnings("unchecked")
    protected <T extends ImageMetaData> List<T> getImages(boolean retrieveBenchmarks) {
        List<T> result = new ArrayList<T>();
        for (ImageManager manager : managers) {
            result.addAll((List<? extends T>) (retrieveBenchmarks ? manager.getBenchmarks() : manager.getSystems()));
        }
        markDuplicates(result);
        return result;
    }

    public void addManager(ImageManager manager) {
        this.managers.add(manager);
    }
}
