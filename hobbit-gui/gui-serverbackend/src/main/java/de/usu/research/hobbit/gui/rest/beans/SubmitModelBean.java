package de.usu.research.hobbit.gui.rest.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SubmitModelBean {
	private String benchmark;
	private String system;
	
	private List<ConfigurationParamValueBean> configurationParams;
	

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
