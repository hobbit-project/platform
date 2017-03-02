package de.usu.research.hobbit.gui.rest.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

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