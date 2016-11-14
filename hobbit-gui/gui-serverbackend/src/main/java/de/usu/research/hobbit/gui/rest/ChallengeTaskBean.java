package de.usu.research.hobbit.gui.rest;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class ChallengeTaskBean {
  /**
   * The challenge task id or in OWL speak URI
   */
  public String id;

  /**
   * The challenge task name/ rdfs:label
   */
  public String name;

  /**
   * The description of the challenge task / rdfs:comment
   */
  public String description;

  public BenchmarkBean benchmark;

  public List<ConfigurationParamValueBean> configurationParams;
  

  public List<ConfigurationParamValueBean> getConfigurationParams() {
      return configurationParams;
  }
  public void setConfigurationParams(List<ConfigurationParamValueBean> configurationParams) {
      this.configurationParams = configurationParams;
  }
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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