package de.usu.research.hobbit.gui.rest.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class ExperimentsListBean {
	private List<ExperimentBean> experiments;
	
	public ExperimentsListBean() {		
	}

	public List<ExperimentBean> getExperiments() {
		return experiments;
	}

	public void setExperiments(List<ExperimentBean> experiments) {
		this.experiments = experiments;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
