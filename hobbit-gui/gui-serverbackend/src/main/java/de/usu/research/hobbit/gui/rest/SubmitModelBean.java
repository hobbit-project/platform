package de.usu.research.hobbit.gui.rest;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SubmitModelBean {
	public String benchmark;
	public String system;
	
	public List<ConfigurationParamValueBean> configurationParams;
	

	public List<ConfigurationParamValueBean> getConfigurationParams() {
		return configurationParams;
	}
	public void setConfigurationParams(List<ConfigurationParamValueBean> configurationParams) {
		this.configurationParams = configurationParams;
	}
	public String getBenchmark() {
		return benchmark;
	}
	public void setBenchmark(String benchmark) {
		this.benchmark = benchmark;
	}
	public String getSystem() {
		return system;
	}
	public void setSystem(String system) {
		this.system = system;
	}
}
