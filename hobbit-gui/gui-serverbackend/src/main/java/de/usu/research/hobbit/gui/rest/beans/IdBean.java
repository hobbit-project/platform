package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IdBean {
  /**
   * The challenge id or in OWL speak URI
   */
  private String id;
  
  public IdBean() {    
  }
  
  public IdBean(String id) {
    this.id = id;
  }
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return id;
  }
}
