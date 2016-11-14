package de.usu.research.hobbit.gui.rest;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class ConfigurationParamBean {
	public String id;
	public String name;
	public String description;
	public Datatype datatype;
	public String range;
	public String defaultValue;
	public List<SelectOptionBean> options;
	
	public boolean isFeature = false;

	public boolean required;
	public Integer min;
	public Integer max;
	
	public ConfigurationParamBean() {		
	}
	
	public ConfigurationParamBean(String name, Datatype datatype) {
		this(name, datatype, false, null);
	}
	
	public ConfigurationParamBean(String name, Datatype datatype, boolean required, String defaultValue) {
		this.name = name;
		this.datatype = datatype;
		this.required = required;
		this.defaultValue = defaultValue;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Datatype getDatatype() {
		return datatype;
	}

	public void setDatatype(Datatype datatype) {
		this.datatype = datatype;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Integer getMin() {
		return min;
	}

	public void setMin(Integer min) {
		this.min = min;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	public List<SelectOptionBean> getOptions() {
		return options;
	}

	public void setOptions(List<SelectOptionBean> options) {
		this.options = options;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	public String getRange() {
		return range;
	}

	public void setRange(String range) {
		this.range = range;
	}


}
