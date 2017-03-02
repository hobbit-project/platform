package de.usu.research.hobbit.gui.rest.beans;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

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
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}