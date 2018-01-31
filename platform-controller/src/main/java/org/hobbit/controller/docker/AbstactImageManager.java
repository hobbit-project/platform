package org.hobbit.controller.docker;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.ImageMetaData;
import org.hobbit.core.data.SystemMetaData;

/**
 * An abstract implementation of the {@link ImageManager} interface which takes
 * care of systems or benchmarks which have the same URI.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public abstract class AbstactImageManager implements ImageManager {

    protected Comparator<ImageMetaData> comparator = new DateBasedImageMetaDataComparator();

    @Override
    public List<BenchmarkMetaData> getBenchmarks() {
        return markDuplicates(getUncheckedBenchmarks());
    }

    /**
     * A method that returns a list of unchecked benchmarks (they may have
     * overlapping URIs).
     * 
     * @return a list of known benchmarks
     */
    protected abstract List<BenchmarkMetaData> getUncheckedBenchmarks();

    @Override
    public List<SystemMetaData> getSystems() {
        return markDuplicates(getUncheckedSystems());
    }

    /**
     * A method that returns a list of unchecked systems (they may have overlapping
     * URIs).
     * 
     * @return a list of known systems
     */
    protected abstract List<SystemMetaData> getUncheckedSystems();

    /**
     * Retrieves the RDF model of the benchmark with the given URI. If multiple
     * benchmarks with this URI are defined, the one that is not marked as duplicate
     * is returned.
     *
     * @param benchmarkUri
     *            the URI of the chosen benchmark
     * @return the RDF model of the chosen benchmark
     */
    public BenchmarkMetaData getBenchmark(String benchmarkUri) {
        if (benchmarkUri == null) {
            return null;
        }
        return getUncheckedBenchmarks().parallelStream().filter(b -> benchmarkUri.equals(b.uri)).sorted(comparator)
                .findFirst().orElse(null);
    }

    /**
     * Retrieves the RDF model of the system with the given URI. If multiple systems
     * with this URI are defined, the one that is not marked as duplicate is
     * returned.
     *
     * @param systemUri
     *            the URI of the chosen system
     * @return the RDF model of the chosen system
     */
    public SystemMetaData getSystem(String systemUri) {
        if (systemUri == null) {
            return null;
        }
        return getUncheckedSystems().parallelStream().filter(s -> systemUri.equals(s.uri)).sorted(comparator)
                .findFirst().orElse(null);
    }

    /**
     * Identifies duplicates and marks them using the
     * {@link #addErrorToDuplicates(List)} method.
     * 
     * @param images
     *            a list of images that may contain duplicates
     * @return the same list
     */
    protected <T extends ImageMetaData> List<T> markDuplicates(List<T> images) {
        createListPerUri(images).filter(l -> l.size() > 1).forEach(l -> addErrorToDuplicates(l));
        return images;
    }

    /**
     * Sets the {@link ImageMetaData#defError} attribute of all meta data elements
     * that are identified as duplicates.
     * 
     * @param images
     *            the list of image meta data objects that have the same URI
     */
    protected <T extends ImageMetaData> void addErrorToDuplicates(List<T> images) {
        if (images.size() > 1) {
            Collections.sort(images, comparator);
            String errorMsg = null;
            for (T image : images) {
                if (errorMsg == null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("This image has the same URI as ");
                    builder.append(image.name);
                    builder.append(" (");
                    builder.append(image.uri);
                    builder.append(") from ");
                    builder.append(image.source);
                    builder.append(".");
                    errorMsg = builder.toString();
                } else {
                    image.defError = errorMsg;
                }
            }
        }
    }

    /**
     * Creates a stream of lists each containing all image meta data instances that
     * are sharing the same URI.
     * 
     * @param images
     *            a list of image meta data instances
     * @return a stream of lists grouped by the image meta data URI
     */
    protected static <T extends ImageMetaData> Stream<List<T>> createListPerUri(List<T> images) {
        return images.parallelStream()
                .collect(Collectors.groupingBy(ImageMetaData::getUri, Collectors.mapping(i -> i, Collectors.toList())))
                .values().parallelStream();
    }

    /**
     * Returns a filtered list of systems based on the given filter. This method
     * makes sure that the correct {@link #getSystems()} method is used (i.e., the
     * method that adds additional information to the systems which can not be added
     * later on, after the filtering).
     * 
     * @param filter
     *            the filter that is used. Should be thread safe.
     * @return the filtered list
     */
    protected List<SystemMetaData> getFilteredSystems(Predicate<? super ImageMetaData> filter) {
        return getSystems().parallelStream().filter(filter).collect(Collectors.toList());
    }

    /**
     * @param comparator
     *            the comparator to set
     */
    public void setComparator(Comparator<ImageMetaData> comparator) {
        this.comparator = comparator;
    }

    public static class DateBasedImageMetaDataComparator implements Comparator<ImageMetaData> {

        @Override
        public int compare(ImageMetaData o1, ImageMetaData o2) {
            if (o1 == null) {
                if (o2 == null) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (o2 == null) {
                    return -1;
                } else {
                    return o1.date.compareTo(o2.date);
                }
            }
        }

    }
}
