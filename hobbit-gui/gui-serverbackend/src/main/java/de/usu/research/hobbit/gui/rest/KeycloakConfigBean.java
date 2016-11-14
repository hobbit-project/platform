package de.usu.research.hobbit.gui.rest;

public class KeycloakConfigBean {
  public String realm;
  public String url;
  public String clientId;
  
  @Override
  public String toString() {
    return "realm=" + realm + ", url=" + url + ", clientId=" + clientId;
  }
}
