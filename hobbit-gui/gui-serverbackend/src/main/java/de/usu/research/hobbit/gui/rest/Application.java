package de.usu.research.hobbit.gui.rest;

import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

/**
 * Created on 28/10/2016.
 */
@ApplicationPath("rest")
public class Application extends ResourceConfig {

  /**
   * see web.xml:  there is cuurently no API to get these declared name dynamically...
   <security-role>
   <role-name>benchmark-provider</role-name>
   </security-role>
   <security-role>
   <role-name>system-provider</role-name>
   </security-role>
   <security-role>
   <role-name>guest</role-name>
   </security-role>
   <security-role>
   <role-name>challenge-organiser</role-name>
   </security-role>
   */

  public static final String[] ROLE_NAMES = new String[] { "benchmark-provider" , "system-provider", "challenge-organiser", "guest"};

  public Application() {
    packages("de.usu.research.hobbit.gui.rest", "de.usu.research.hobbit.gui.rest.securedexamples");
    // enable security-annotations, see https://jersey.java.net/documentation/latest/security.html#annotation-based-security
    register(RolesAllowedDynamicFeature.class);
  }

  public static String[] getRoleNames(SecurityContext sc) {
    Stream<String> stringStream = Stream.of(ROLE_NAMES);
    return stringStream.filter(x -> sc.isUserInRole(x)).toArray(String[]::new);
  }
}
