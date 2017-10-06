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
import java.util.List;

@XmlRootElement
public class ChallengeTaskBean extends NamedEntityBean {

    private BenchmarkBean benchmark;

    private List<ConfigurationParamValueBean> configurationParams;


    public List<ConfigurationParamValueBean> getConfigurationParams() {
        return configurationParams;
    }

    public void setConfigurationParams(List<ConfigurationParamValueBean> configurationParams) {
        this.configurationParams = configurationParams;
    }

    public BenchmarkBean getBenchmark() {
        return benchmark;
    }

    public void setBenchmark(BenchmarkBean benchmark) {
        this.benchmark = benchmark;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
