package de.usu.research.hobbit.gui.rest;

import java.time.LocalDate;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.builder.ToStringBuilder;

import de.usu.research.hobbit.gui.util.LocalDateAdapter;

@XmlRootElement
public class ChallengeBean {
  /**
   * The challenge id or in OWL speak URI
   */
  public String id;

  /**
   * The challenge name/ rdfs:label
   */
  public String name;

  /**
   * The description of the challenge / rdfs:comment
   */
  public String description;

  public String organizer;

  @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
  public LocalDate publishDate;
  
  @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
  public LocalDate executionDate;

  public boolean published;

  public boolean closed;

  public List<ChallengeTaskBean> tasks;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOrganizer() {
    return organizer;
  }

  public void setOrganizer(String organizer) {
    this.organizer = organizer;
  }

  
  public LocalDate getPublishDate() {
    return publishDate;
  }

  public void setPublishDate(LocalDate publishDate) {
    this.publishDate = publishDate;
  }

  public LocalDate getExecutionDate() {
    return executionDate;
  }

  public void setExecutionDate(LocalDate executionDate) {
    this.executionDate = executionDate;
  }

  public boolean isPublished() {
    return published;
  }

  public void setPublished(boolean published) {
    this.published = published;
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