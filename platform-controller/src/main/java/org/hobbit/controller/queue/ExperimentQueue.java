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
package org.hobbit.controller.queue;

import java.util.List;

import org.hobbit.controller.data.ExperimentConfiguration;

/**
 * This is the interface of a queue containing experiment configurations that
 * should be executed.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 */
public interface ExperimentQueue {

    /**
     * Returns the experiment that should be executed next.
     *
     * @return the experiment that should be executed next
     */
    public ExperimentConfiguration getNextExperiment();

    /**
     * Adds the given experiment to the queue.
     *
     * @param experiment
     *            the experiment that should be added
     */
    public void add(ExperimentConfiguration experiment);

    /**
     * Removes the experiment from the queue.
     *
     * @param experiment
     *            the experiment that should be removed from the queue
     * @return {@code true} if the given experiment has been removed
     */
    public boolean remove(ExperimentConfiguration experiment);

    /**
     * Returns the list of all experiments waiting in this queue.
     *
     * @return the list of all experiments waiting in this queue
     */
    public List<ExperimentConfiguration> listAll();

    /**
     * Retrieves the experiment configuration with the given experiment id.
     * 
     * @param experimentId
     *            the id of the experiment that should be retrieved
     * @return the experiment configuration or {@code null} if such an experiment
     *         can not be found
     */
    public ExperimentConfiguration getExperiment(String experimentId);
}
