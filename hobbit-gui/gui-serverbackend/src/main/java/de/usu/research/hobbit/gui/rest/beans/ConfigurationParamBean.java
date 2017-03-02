package de.usu.research.hobbit.gui.rest.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

import de.usu.research.hobbit.gui.rest.Datatype;

@XmlRootElement
public class ConfigurationParamBean extends NamedEntityBean {
	private Datatype datatype;
	private String range;
	private String defaultValue;
	private List<SelectOptionBean> options;
	
	private boolean isFeature = false;

	private boolean required;
	private Integer min;
	private Integer max;
	
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

    public boolean isFeature() {
        return isFeature;
    }

    public void setFeature(boolean isFeature) {
        this.isFeature = isFeature;
    }
}
