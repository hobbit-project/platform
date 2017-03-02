package de.usu.research.hobbit.gui.rest.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class ConfiguredBenchmarkBean extends BenchmarkBean {
	private List<ConfigurationParamValueBean> configurationParamValues;
	
	public List<ConfigurationParamValueBean> getConfigurationParamValues() {
		return configurationParamValues;
	}
	
	public void setConfigurationParamValues(List<ConfigurationParamValueBean> configurationParamValues) {
		this.configurationParamValues = configurationParamValues;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}