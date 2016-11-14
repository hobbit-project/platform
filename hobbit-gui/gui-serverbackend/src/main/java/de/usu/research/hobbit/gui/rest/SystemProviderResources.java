package de.usu.research.hobbit.gui.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("system-provider")
public class SystemProviderResources {
  private static final Logger LOGGER = LoggerFactory.getLogger(SystemProviderResources.class);

  private static final Map<String, Map<String, List<TaskRegistrationBean>>> registrations = new ConcurrentHashMap<>();

  @GET()
  @RolesAllowed("system-provider")
  @Path("systems")
  @Produces(MediaType.APPLICATION_JSON)
  public List<SystemBean> getSystems(@Context SecurityContext sc) {
    UserInfoBean bean = InternalResources.getUserInfoBean(sc);
    LOGGER.info("getSystems for " + bean.getPreferredUsername());

    ArrayList<SystemBean> list = new ArrayList<>();
    if (bean.getPreferredUsername().equals("system-provider")) {
      SystemBean bean1 = new SystemBean();
      bean1.id = "system1";
      bean1.name = "System No.1";
      bean1.description = "one";
      list.add(bean1);

      SystemBean bean2 = new SystemBean();
      bean2.id = "system2";
      bean2.name = "System No.2";
      bean2.description = "two";
      list.add(bean2);
    } else if (bean.getPreferredUsername().equals("admin")) {
      SystemBean bean3 = new SystemBean();
      bean3.id = "system3";
      bean3.name = "System No.3";
      bean3.description = "three";
      list.add(bean3);
    } else {
      LOGGER.info("unknown system provider");
    }
    return list;
  }

  @GET
  @RolesAllowed("challenge-organiser")
  @Path("challenge-all-registrations/{challengeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public List<TaskRegistrationBean> getAllChallengeRegistrations(@PathParam("challengeId") String challengeId) {
    ArrayList<TaskRegistrationBean> list = new ArrayList<>();
    for (Map<String, List<TaskRegistrationBean>> map : registrations.values()) {
      synchronized (map) {
        List<TaskRegistrationBean> sublist = map.get(challengeId);
        if (sublist != null) {
          list.addAll(sublist);
        }
      }
    }

    return list;
  }

  @GET
  @RolesAllowed("system-provider")
  @Path("challenge-registrations/{challengeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public List<TaskRegistrationBean> getChallengeRegistrations(@Context SecurityContext sc,
      @PathParam("challengeId") String challengeId) {
    Map<String, List<TaskRegistrationBean>> map = getProviderRegistrations(sc, false);

    ArrayList<TaskRegistrationBean> list = new ArrayList<>();
    if (map != null) {
      synchronized (map) {
        List<TaskRegistrationBean> sublist = map.get(challengeId);
        if (sublist != null) {
          list.addAll(sublist);
        }
      }
    }

    return list;
  }

  @PUT
  @RolesAllowed("system-provider")
  @Path("challenge-registrations/{challengeId}")
  @Consumes(MediaType.APPLICATION_JSON)
  public void updateChallengeRegistrations(@Context SecurityContext sc, @PathParam("challengeId") String challengeId,
      List<TaskRegistrationBean> list) {
    Map<String, List<TaskRegistrationBean>> map = getProviderRegistrations(sc, true);

    synchronized (map) {
      map.put(challengeId, list);
    }
  }

  @PUT
  @RolesAllowed("system-provider")
  @Path("challenge-registrations/{challengeId}/{taskId}")
  @Consumes(MediaType.APPLICATION_JSON)
  public void updateChallengeTaskRegistrations(@Context SecurityContext sc,
      @PathParam("challengeId") String challengeId, @PathParam("taskId") String taskId,
      List<TaskRegistrationBean> list) {
    Map<String, List<TaskRegistrationBean>> map = getProviderRegistrations(sc, true);

    synchronized (map) {
      List<TaskRegistrationBean> oldList = map.get(challengeId);
      ArrayList<TaskRegistrationBean> newList = new ArrayList<>();
      if (oldList != null) {
        for (TaskRegistrationBean t : oldList) {
          if (!t.taskId.equals(taskId)) {
            newList.add(t);
          }
        }
      }
      newList.addAll(list);
      map.put(challengeId, newList);
    }
  }

  private Map<String, List<TaskRegistrationBean>> getProviderRegistrations(SecurityContext sc, boolean create) {
    UserInfoBean bean = InternalResources.getUserInfoBean(sc);
    String name = bean.getPreferredUsername();

    Map<String, List<TaskRegistrationBean>> map = registrations.get(name);
    if (map == null && create) {
      map = new HashMap<String, List<TaskRegistrationBean>>();
      registrations.put(name, map);
    }
    return map;
  }
}