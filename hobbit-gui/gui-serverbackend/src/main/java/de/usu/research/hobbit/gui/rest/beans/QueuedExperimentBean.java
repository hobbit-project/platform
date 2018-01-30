package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

import org.hobbit.core.data.status.QueuedExperiment;

/**
 * This bean represents an experiment that is waiting in the queue to be
 * executed.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
@XmlRootElement
public class QueuedExperimentBean extends QueuedExperiment {

    public QueuedExperimentBean() {
    }

    public QueuedExperimentBean(QueuedExperiment queuedExperiment) {
        setExperimentId(queuedExperiment.getExperimentId());
        setBenchmarkName(queuedExperiment.getBenchmarkName());
        setBenchmarkUri(queuedExperiment.getBenchmarkUri());
        setSystemName(queuedExperiment.getSystemName());
        setSystemUri(queuedExperiment.getSystemUri());
        setChallengeUri(queuedExperiment.getChallengeUri());
        setChallengeTaskUri(queuedExperiment.getChallengeTaskUri());
        setCanBeCanceled(queuedExperiment.isCanBeCanceled());
    }
}
