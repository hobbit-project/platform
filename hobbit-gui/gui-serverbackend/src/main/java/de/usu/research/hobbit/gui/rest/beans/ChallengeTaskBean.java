/**
 * This file is part of gui-serverbackend.
 *
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rest.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class ChallengeTaskBean extends NamedEntityBean {

  private BenchmarkBean benchmark;

  private List<ConfigurationParamValueBean> configurationParams;

  private List<String> rankingKPIs;


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

  public List<String> getRankingKPIs() {
      return rankingKPIs;
  }

  public void setRankingKPIs(List<String> rankingKPIs) {
      this.rankingKPIs = rankingKPIs;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
