package org.hobbit.controller.analyze;

import java.io.IOException;

public interface ExperimentAnalyzer {
    /**
     * Runs further analysis based on the experiment results of the given URI.
     * 
     * @param uri
     *            the URI of the experiment which should be enhanced with
     *            further analysis
     * @throws IOException
     */
    public void analyzeExperiment(String uri) throws IOException;
}
