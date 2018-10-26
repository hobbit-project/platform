/**
 * This file is part of platform-controller.
 *
 * platform-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * platform-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with platform-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.controller.docker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.hobbit.core.data.BenchmarkMetaData;
import org.hobbit.core.data.SystemMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBasedImageManager implements ImageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedImageManager.class);

    private static final String DEFAULT_DEF_FOLDER = "metadata";
    private static final Date DEFAULT_DATE = new Date(0);

    private final String inputFolder;
    private Timer timer;
    private int repeatInterval = 60 * 1000; // every 1 min
    @SuppressWarnings("unchecked")
    private List<BenchmarkMetaData> benchmarks = Collections.EMPTY_LIST;
    @SuppressWarnings("unchecked")
    private List<SystemMetaData> systems = Collections.EMPTY_LIST;

    public FileBasedImageManager() {
        this(DEFAULT_DEF_FOLDER);
    }

    public FileBasedImageManager(String inputFolder) {
        this.inputFolder = inputFolder;
        timer = new Timer();
        startFetchingFiles();
    }

    protected void startFetchingFiles() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<BenchmarkMetaData> newBenchmarks = new ArrayList<>();
                List<SystemMetaData> newSystems = new ArrayList<>();

                try {
                    // Get a list of *.ttl files
                    Collection<File> files = FileUtils.listFiles(new File(inputFolder), new String[] { "ttl" }, false);
                    // Go through the list and try to read the files
                    for (File f : files) {
                        readFile(f, newBenchmarks, newSystems);
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception while reading ", e);
                }

                if (benchmarks == null) {
                    // This is the first fetching of projects -> we might have
                    // to notify threads that are waiting for that
                    benchmarks = newBenchmarks;
                    systems = newSystems;
                    synchronized (this) {
                        this.notifyAll();
                    }
                } else {
                    // update cached version
                    benchmarks = newBenchmarks;
                    systems = newSystems;
                }
            }
        }, 0, repeatInterval);
    }

    protected void readFile(File f, List<BenchmarkMetaData> newBenchmarks, List<SystemMetaData> newSystems) {
        byte[] data = null;
        try {
            data = FileUtils.readFileToByteArray(f);
        } catch (IOException e) {
            LOGGER.error("Couldn't read {}. It will be ignored.", f.getAbsolutePath());
            return;
        }
        Model model = null;
        try {
            model = MetaDataFactory.byteArrayToModel(data, "TTL");
        } catch (Exception e) {
            LOGGER.error("Couldn't parse " + f.getAbsolutePath() + ". It will be ignored.", e);
            return;
        }
        // Add all benchmarks found in a copy of the model that does not contain any
        // systems
        newBenchmarks.addAll(MetaDataFactory.modelToBenchmarkMetaData(
                MetaDataFactory.getModelWithUniqueSystem(model, ""), f.getAbsolutePath(), DEFAULT_DATE));
        // Add all systems found in a copy of the model that does not contain any
        // benchmarks
        newSystems.addAll(MetaDataFactory.modelToSystemMetaData(MetaDataFactory.getModelWithUniqueBenchmark(model, ""),
                f.getAbsolutePath(), DEFAULT_DATE));
    }

    @Override
    public List<BenchmarkMetaData> getBenchmarks() {
        return new ArrayList<>(benchmarks);
    }

    @Override
    public List<SystemMetaData> getSystems() {
        return new ArrayList<>(systems);
    }

}
