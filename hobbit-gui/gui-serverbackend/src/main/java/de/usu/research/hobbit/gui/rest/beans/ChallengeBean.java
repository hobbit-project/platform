package de.usu.research.hobbit.gui.rest.beans;

import java.time.OffsetDateTime;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.builder.ToStringBuilder;

import de.usu.research.hobbit.gui.util.OffsetDateTimeAdapter;

@XmlRootElement
public class ChallengeBean extends NamedEntityBean {
  private String organizer;

  @XmlJavaTypeAdapter(value = OffsetDateTimeAdapter.class)
  private OffsetDateTime publishDate;
  
  @XmlJavaTypeAdapter(value = OffsetDateTimeAdapter.class)
  private OffsetDateTime executionDate;

  private boolean visible;

  private boolean closed;

  private List<ChallengeTaskBean> tasks;

  public String getOrganizer() {
    return organizer;
  }

  public void setOrganizer(String organizer) {
    this.organizer = organizer;
  }

  
  public OffsetDateTime getPublishDate() {
    return publishDate;
  }

  public void setPublishDate(OffsetDateTime publishDate) {
    this.publishDate = publishDate;
  }

  public OffsetDateTime getExecutionDate() {
    return executionDate;
  }

  public void setExecutionDate(OffsetDateTime executionDate) {
    this.executionDate = executionDate;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public boolean isClosed() {
    return closed;
  }

  public void setClosed(boolean closed) {
    this.closed = closed;
  }

  public List<ChallengeTaskBean> getTasks() {
    return tasks;
  }

  public void setTasks(List<ChallengeTaskBean> tasks) {
    this.tasks = tasks;
  }

   @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}