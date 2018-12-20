package de.usu.research.hobbit.gui.rest.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AnalysisResultSetBean {

    private String uri;
    private BenchmarkBean benchmark;
    private SystemBean system;
    private List<AnalysisResultBean> results;
    
    public AnalysisResultSetBean(String uri,BenchmarkBean benchmark, SystemBean system, List<AnalysisResultBean> results) {
        this.uri = uri;
        this.benchmark = benchmark;
        this.system = system;
        this.results = results;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the benchmark
     */
    public BenchmarkBean getBenchmark() {
        return benchmark;
    }

    /**
     * @param benchmark the benchmark to set
     */
    public void setBenchmark(BenchmarkBean benchmark) {
        this.benchmark = benchmark;
    }

    /**
     * @return the system
     */
    public SystemBean getSystem() {
        return system;
    }

    /**
     * @param system the system to set
     */
    public void setSystem(SystemBean system) {
        this.system = system;
    }

    /**
     * @return the results
     */
    public List<AnalysisResultBean> getResults() {
        return results;
    }

    /**
     * @param results the results to set
     */
    public void setResults(List<AnalysisResultBean> results) {
        this.results = results;
    }
}
