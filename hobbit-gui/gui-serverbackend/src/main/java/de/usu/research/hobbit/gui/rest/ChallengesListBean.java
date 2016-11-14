package de.usu.research.hobbit.gui.rest;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class ChallengesListBean {
	public List<ChallengeBean> challenges;
	
	public ChallengesListBean() {		
	}

	public List<ChallengeBean> getChallenges() {
		return challenges;
	}

	public void setChallenges(List<ChallengeBean> challenges) {
		this.challenges = challenges;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
