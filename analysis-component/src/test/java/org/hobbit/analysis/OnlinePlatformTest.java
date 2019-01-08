/**
 * This file is part of analysis-component.
 *
 * analysis-component is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * analysis-component is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with analysis-component.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.analysis;

import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.hobbit.vocab.HobbitExperiments;

public class OnlinePlatformTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private AnalysisComponent analysis;

    @Before
    public void setUp() throws Exception {
        environmentVariables.set("HOBBIT_RABBIT_HOST", "localhost");

        analysis = new AnalysisComponent();
        analysis.init();
    }

    @Test
    @Ignore // FIXME make it run on different local setups and on CI
    public void test() throws Exception {
        //analysis.handleRequest(HobbitExperiments.getExperimentURI("1487245716296")); // HOBBIT Platform Benchmark
        analysis.handleRequest(HobbitExperiments.getExperimentURI("1527145015098")); // ODIN v2
    }

}
