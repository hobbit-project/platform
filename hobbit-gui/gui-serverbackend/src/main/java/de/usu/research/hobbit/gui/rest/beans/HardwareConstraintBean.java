package de.usu.research.hobbit.gui.rest.beans;

import java.beans.Transient;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public class HardwareConstraintBean {

    private String iri;
    private String label;
    private int cpuCount = -1;
    private long memory = -1;

    /**
     * @return the iri
     */
    public String getIri() {
        return iri;
    }

    /**
     * @param iri the iri to set
     */
    public void setIri(String iri) {
        this.iri = iri;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the cpuCount
     */
    public int getCpuCount() {
        return cpuCount;
    }

    /**
     * @param cpuCount the cpuCount to set
     */
    public void setCpuCount(int cpuCount) {
        this.cpuCount = cpuCount;
    }

    /**
     * @return the memory
     */
    public long getMemory() {
        return memory;
    }

    /**
     * @param memory the memory to set
     */
    public void setMemory(long memory) {
        this.memory = memory;
    }

    /**
     * Returns true if this constraint is valid, i.e., if it has an IRI assigned and
     * it contains at least one requirement that can be processed by the platform.
     * 
     * @return {@code true} if the constraint is valid, otherwise {@code false}
     */
    @XmlTransient
    @Transient
    public boolean isValidConstraint() {
        return (iri != null) && (!iri.isEmpty()) && (cpuCount > 0) || (memory > 0);
    }

}
