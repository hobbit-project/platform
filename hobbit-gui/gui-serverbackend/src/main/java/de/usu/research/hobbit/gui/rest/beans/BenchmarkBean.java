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

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class BenchmarkBean extends NamedEntityBean {

    /**
     * The systems linked to the benchmark
     */
    private List<SystemBean> systems;

    /**
     * The configuration parameters for this benchmark
     */
    private List<ConfigurationParamBean> configurationParams;

    private List<String> configurationParamNames = new ArrayList<>();

    private List<KeyPerformanceIndicatorBean> kpis;

    public BenchmarkBean() {
    }

    public BenchmarkBean(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public BenchmarkBean(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public List<SystemBean> getSystems() {
        return systems;
    }

    public void setSystems(List<SystemBean> systems) {
        this.systems = systems;
    }

    public List<ConfigurationParamBean> getConfigurationParams() {
        return configurationParams;
    }

    public void setConfigurationParams(List<ConfigurationParamBean> configurationParams) {
        this.configurationParams = configurationParams;
    }

    public List<String> getConfigurationParamNames() {
        return configurationParamNames;
    }

    public void setConfigurationParamNames(List<String> configurationParamNames) {
        this.configurationParamNames = configurationParamNames;
    }

    public List<KeyPerformanceIndicatorBean> getKpis() {
        return kpis;
    }

    public void setKpis(List<KeyPerformanceIndicatorBean> kpis) {
        this.kpis = kpis;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
