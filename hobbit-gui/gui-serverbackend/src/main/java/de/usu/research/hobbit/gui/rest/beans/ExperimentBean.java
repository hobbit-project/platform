package de.usu.research.hobbit.gui.rest.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExperimentBean {
	private String id;
	
	private List<ConfigurationParamValueBean> kpis;

	private ConfiguredBenchmarkBean benchmark;

	private NamedEntityBean system;
	
	private NamedEntityBean challengeTask; 
	
	private String error;

	public List<ConfigurationParamValueBean> getKpis() {
		return kpis;
	}

	public void setKpis(List<ConfigurationParamValueBean> kpis) {
		this.kpis = kpis;
	}

	public ConfiguredBenchmarkBean getBenchmark() {
		return benchmark;
	}

	public void setBenchmark(ConfiguredBenchmarkBean benchmark) {
		this.benchmark = benchmark;
	}

	public NamedEntityBean getSystem() {
		return system;
	}

	public void setSystem(NamedEntityBean system) {
		this.system = system;
	}
	
	public NamedEntityBean getChallengeTask() {
		return challengeTask;
	}

	public void setChallengeTask(NamedEntityBean challengeTask) {
		this.challengeTask = challengeTask;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
