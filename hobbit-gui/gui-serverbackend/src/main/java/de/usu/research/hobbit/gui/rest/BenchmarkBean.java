package de.usu.research.hobbit.gui.rest;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class BenchmarkBean {
	
	/**
	 * The benchmark id or in OWL speak URI
	 */
	public String id;
	
	/**
	 * The benchmark name/ rdfs:label
	 */
	public String name;
	
	/**
	 * The description of the benchmark / rdfs:comment
	 */
	public String description;
	
	/**
	 * The systems linked to the benchmark
	 */
	public List<SystemBean> systems;
	
	/**
	 * The configuration parameters for this benchmark
	 */
	public List<ConfigurationParamBean> configurationParams;
	
	public List<String> configurationParamNames = new ArrayList<>();
	
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