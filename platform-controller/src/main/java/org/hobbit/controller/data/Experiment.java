package org.hobbit.controller.data;

import java.util.Set;

/**
 * This class represents the information about a running experiment.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class Experiment {

	protected String hobbitSessionId;
	protected String systemMainContainer;
	protected Set<String> systemContainers;
	protected String benchmarkMainContainer;
	protected Set<String> benchmarkContainers;
	
	
}
