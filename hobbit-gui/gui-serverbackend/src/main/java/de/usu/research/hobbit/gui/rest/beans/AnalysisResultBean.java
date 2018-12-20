package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AnalysisResultBean {

    protected String parameterUri;
    protected String kpiUri;
    protected NamedEntityBean algorithm;
    
    public AnalysisResultBean(String parameterUri, String kpiUri,
            NamedEntityBean algorithm) {
        this.parameterUri = parameterUri;
        this.kpiUri = kpiUri;
        this.algorithm = algorithm;
    }

    /**
     * @return the parameterUri
     */
    public String getParameterUri() {
        return parameterUri;
    }


    /**
     * @param parameterUri the parameterUri to set
     */
    public void setParameterUri(String parameterUri) {
        this.parameterUri = parameterUri;
    }


    /**
     * @return the kpiUri
     */
    public String getKpiUri() {
        return kpiUri;
    }


    /**
     * @param kpiUri the kpiUri to set
     */
    public void setKpiUri(String kpiUri) {
        this.kpiUri = kpiUri;
    }


    /**
     * @return the algorithm
     */
    public NamedEntityBean getAlgorithm() {
        return algorithm;
    }

    /**
     * @param algorithm the algorithm to set
     */
    public void setAlgorithm(NamedEntityBean algorithm) {
        this.algorithm = algorithm;
    }
}
