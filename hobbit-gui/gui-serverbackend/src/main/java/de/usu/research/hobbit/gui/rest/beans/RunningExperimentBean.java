package de.usu.research.hobbit.gui.rest.beans;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hobbit.core.data.status.RunningExperiment;

import de.usu.research.hobbit.gui.util.OffsetDateTimeAdapter;

/**
 * This extension of a {@link QueuedExperimentBean} represents an experiment
 * that is currently executed and may have additional information, e.g., the
 * status of the execution.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
@XmlRootElement
public class RunningExperimentBean extends QueuedExperimentBean {
    /**
     * The status of the execution of this experiment.
     */
    private String status;
    /**
     * The point in time at which the experiment has been started.
     */
    @XmlJavaTypeAdapter(value = OffsetDateTimeAdapter.class)
    private OffsetDateTime startTimestamp;
    /**
     * The point in time until the experiment will have to be finished. If it is
     * {@code null}, this time has not been set until now.
     */
    @XmlJavaTypeAdapter(value = OffsetDateTimeAdapter.class)
    private OffsetDateTime latestDateToFinish;

    public RunningExperimentBean() {
    }

    public RunningExperimentBean(RunningExperiment experiment) {
        super(experiment);
        this.status = experiment.getStatus();
        // We assume that we have UTC configured in the backend
        Calendar temp = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        temp.setTimeInMillis(experiment.getStartTimestamp());
        this.startTimestamp = OffsetDateTime.of(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH) + 1,
                temp.get(Calendar.DAY_OF_MONTH), temp.get(Calendar.HOUR_OF_DAY), temp.get(Calendar.MINUTE),
                temp.get(Calendar.SECOND), 0, ZoneOffset.UTC);
        if (experiment.getTimestampOfAbortion() > 0) {
            temp.setTimeInMillis(experiment.getTimestampOfAbortion());
            this.latestDateToFinish = OffsetDateTime.of(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH) + 1,
                    temp.get(Calendar.DAY_OF_MONTH), temp.get(Calendar.HOUR_OF_DAY), temp.get(Calendar.MINUTE),
                    temp.get(Calendar.SECOND), 0, ZoneOffset.UTC);
        }
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the latestDateToFinish
     */
    public OffsetDateTime getLatestDateToFinish() {
        return latestDateToFinish;
    }

    /**
     * @param latestDateToFinish
     *            the latestDateToFinish to set
     */
    public void setLatestDateToFinish(OffsetDateTime latestDateToFinish) {
        this.latestDateToFinish = latestDateToFinish;
    }

}
