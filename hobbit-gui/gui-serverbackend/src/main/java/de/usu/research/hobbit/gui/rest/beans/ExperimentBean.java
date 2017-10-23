/**
 * This file is part of gui-serverbackend.
 * <p>
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class ExperimentBean {
    private String id;

    private List<KeyPerformanceIndicatorBean> kpis;

    private ConfiguredBenchmarkBean benchmark;

    private NamedEntityBean system;

    private NamedEntityBean challengeTask;

    private String error;

    public List<KeyPerformanceIndicatorBean> getKpis() {
        return kpis;
    }

    public void setKpis(List<KeyPerformanceIndicatorBean> kpis) {
        this.kpis = kpis;
    }

    public ConfiguredBenchmarkBean getBenchmark() {
        return benchmark;
    }

    public void setBenchmark(ConfiguredBenchmarkBean benchmark) {
        this.benchmark = benchmark;
    }

    public NamedEntityBean getSystem() {
        return system;
    }

    public void setSystem(NamedEntityBean system) {
        this.system = system;
    }

    public NamedEntityBean getChallengeTask() {
        return challengeTask;
    }

    public void setChallengeTask(NamedEntityBean challengeTask) {
        this.challengeTask = challengeTask;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
