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
package org.hobbit.controller.data;

import java.util.Calendar;

/**
 * This data structure contains the information about a planned experiment.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ExperimentConfiguration {

    public String id;
    public Calendar executionDate;
    public String benchmarkUri;
    public String benchmarkName;
    public String serializedBenchParams;
    public String systemUri;
    public String challengeUri;
    public String challengeTaskUri;

    public ExperimentConfiguration() {
    }

    public ExperimentConfiguration(String id, String benchmarkUri, String serializedBenchParams, String systemUri) {
        this.id = id;
        this.benchmarkUri = benchmarkUri;
        this.serializedBenchParams = serializedBenchParams;
        this.systemUri = systemUri;
    }

    public ExperimentConfiguration(String id, String benchmarkUri, String serializedBenchParams, String systemUri,
            String challengeUri, String challengeTaskUri, Calendar executionDate) {
        this.id = id;
        this.executionDate = executionDate;
        this.benchmarkUri = benchmarkUri;
        this.serializedBenchParams = serializedBenchParams;
        this.systemUri = systemUri;
        this.challengeUri = challengeUri;
        this.challengeTaskUri = challengeTaskUri;
    }

}
