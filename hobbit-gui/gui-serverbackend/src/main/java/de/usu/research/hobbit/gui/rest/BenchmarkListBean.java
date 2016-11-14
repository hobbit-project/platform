package de.usu.research.hobbit.gui.rest;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class BenchmarkListBean {
	public List<BenchmarkBean> benchmarks;
	
	public BenchmarkListBean() {		
	}

	public List<BenchmarkBean> getBenchmarks() {
		return benchmarks;
	}

	public void setBenchmarks(List<BenchmarkBean> benchmarks) {
		this.benchmarks = benchmarks;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
