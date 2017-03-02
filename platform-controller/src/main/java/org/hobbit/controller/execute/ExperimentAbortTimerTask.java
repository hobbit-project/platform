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
