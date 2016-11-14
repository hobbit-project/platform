package org.hobbit.controller.data;

import java.util.Date;

/**
 * This data structure contains the information about a planned experiment.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ExperimentConfiguration {

    public String id;
    public Date ExecutionDate;
    public String benchmarkUri;
    public String benchmarkName;
    public String serializedBenchParams;
    public String systemUri;

    public ExperimentConfiguration() {
    }

    public ExperimentConfiguration(String id, String benchmarkUri, String serializedBenchParams, String systemUri) {
        this.id = id;
        this.benchmarkUri = benchmarkUri;
        this.serializedBenchParams = serializedBenchParams;
        this.systemUri = systemUri;
    }

}
