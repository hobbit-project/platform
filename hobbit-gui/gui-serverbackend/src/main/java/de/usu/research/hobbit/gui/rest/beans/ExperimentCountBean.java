package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExperimentCountBean {

	private NamedEntityBean challengeTask;
	private int count;

    public ExperimentCountBean() {
    }

    public ExperimentCountBean(NamedEntityBean challengeTask, int count) {
        this.challengeTask = challengeTask;
        this.count = count;
    }

    public NamedEntityBean getChallengeTask() {
        return challengeTask;
    }

    public void setChallengeTask(NamedEntityBean challengeTask) {
        this.challengeTask = challengeTask;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
