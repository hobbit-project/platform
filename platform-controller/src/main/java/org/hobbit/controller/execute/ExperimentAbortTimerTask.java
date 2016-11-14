package org.hobbit.controller.execute;

import java.util.TimerTask;

import org.hobbit.controller.PlatformController;
import org.hobbit.controller.data.ExperimentStatus;
import org.hobbit.controller.data.ExperimentStatus.States;
import org.hobbit.vocab.HobbitErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This timer task uses the given {@link PlatformController} to terminate the
 * benchmark container of the given experiment.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ExperimentAbortTimerTask extends TimerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentAbortTimerTask.class);

    private PlatformController controller;
    private ExperimentStatus experiment;

    public ExperimentAbortTimerTask(PlatformController controller, ExperimentStatus experiment) {
        this.controller = controller;
        this.experiment = experiment;
    }

    @Override
    public void run() {
        if (experiment.getState() != States.STOPPED) {
            LOGGER.error("The experiment {} took too much time. Forcing termination.", experiment.experimentUri);
            experiment.addError(HobbitErrors.ExperimentTookTooMuchTime);
            controller.stopContainer(experiment.getBenchmarkContainer());
        }
    }

    public void setExperiment(ExperimentStatus experiment) {
        this.experiment = experiment;
    }

}
