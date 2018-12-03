package org.hobbit.controller.queue;

import org.hobbit.controller.ExperimentManager;
import org.hobbit.controller.data.ExperimentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */
public class CloudBasedExperimentQueue extends ExperimentQueueImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudBasedExperimentQueue.class);
    String runningConfig = "";

    @Override
    public ExperimentConfiguration getNextExperiment() {
        List<ExperimentConfiguration> all = listAll();
        if(all.size()==0)
            runningConfig="";
        else {
            LOGGER.info("Experiments in the queue: {}", all.size());
            Map<String, List<ExperimentConfiguration>> grouped = all.stream().collect(Collectors.groupingBy(e -> ExperimentManager.getClusterConfiguration(e)));
            if (!grouped.containsKey(runningConfig))
                runningConfig = grouped.keySet().iterator().next();
            LOGGER.info("Getting 1/{} experiments with cluster config={}", grouped.get(runningConfig).size(), runningConfig);
            return grouped.get(runningConfig).get(0);
        }
        return null;
    }

}
