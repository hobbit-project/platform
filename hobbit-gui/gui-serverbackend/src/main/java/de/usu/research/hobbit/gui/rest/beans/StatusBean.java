package de.usu.research.hobbit.gui.rest.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Bean that represents the status of the platform (i.e., the internal status of
 * the controller and its experiment queue).
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
@XmlRootElement
public class StatusBean {
    /**
     * The experiment that is executed at the moment.
     */
    private RunningExperimentBean runningExperiment;

    /**
     * The experiments that are waiting in the queue.
     */
    private List<QueuedExperimentBean> queuedExperiments;

    public StatusBean() {
        super();
    }

    public StatusBean(RunningExperimentBean runningExperiment, List<QueuedExperimentBean> queuedExperiments) {
        super();
        this.runningExperiment = runningExperiment;
        this.queuedExperiments = queuedExperiments;
    }

    /**
     * @return the runningExperiment
     */
    public RunningExperimentBean getRunningExperiment() {
        return runningExperiment;
    }

    /**
     * @param runningExperiment
     *            the runningExperiment to set
     */
    public void setRunningExperiment(RunningExperimentBean runningExperiment) {
        this.runningExperiment = runningExperiment;
    }

    /**
     * @return the queuedExperiments
     */
    public List<QueuedExperimentBean> getQueuedExperiments() {
        return queuedExperiments;
    }

    /**
     * @param queuedExperiments
     *            the queuedExperiments to set
     */
    public void setQueuedExperiments(List<QueuedExperimentBean> queuedExperiments) {
        this.queuedExperiments = queuedExperiments;
    }

}
