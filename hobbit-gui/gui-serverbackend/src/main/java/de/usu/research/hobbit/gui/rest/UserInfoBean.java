package de.usu.research.hobbit.gui.rest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class UserInfoBean {
  String userPrincipalName;
  String preferredUsername;
  String name;
  String email;
  List<String> roles;
  transient Set<String> roleSet;
  
  public String getUserPrincipalName() {
    return userPrincipalName;
  }

  public void setUserPrincipalName(String userPrincipalName) {
    this.userPrincipalName = userPrincipalName;
  }

  public String getPreferredUsername() {
    return preferredUsername;
  }

  public void setPreferredUsername(String preferredUsername) {
    this.preferredUsername = preferredUsername;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public boolean hasRole(String role) {
    if (roles != null) {
      synchronized(this) {
        if (roleSet == null) {
          roleSet = new HashSet<>(roles);
        }
      }
      return roleSet.contains(role);
    } else {
      return false;
    }
  }
}
