package de.usu.research.hobbit.gui.rest;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class TaskRegistrationBean {
  String challengeId;
  String taskId;
  String systemId;
  
  public String getChallengeId() {
    return challengeId;
  }
  public void setChallengeId(String challengeId) {
    this.challengeId = challengeId;
  }
  public String getTaskId() {
    return taskId;
  }
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }
  public String getSystemId() {
    return systemId;
  }
  public void setSystemId(String systemId) {
    this.systemId = systemId;
  }
  
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
