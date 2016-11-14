package de.usu.research.hobbit.gui.rest.securedexamples;

import de.usu.research.hobbit.gui.rest.Application;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.Arrays;

@Path("secure/example")
@PermitAll
public class SecureExampleResourceResource {

  @GET
  @DenyAll
  @Produces(MediaType.TEXT_PLAIN)
  @Path("denyAll")
  public String denyAll() {
    return "denyAll";
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("permitAll")
  public String permitAll() {
    return "permitAll: @PermitAll";
  }

  @GET
  @RolesAllowed("challenge-organiser")
  @Produces(MediaType.TEXT_PLAIN)
  @Path("challengeOrganiserAllowed")
  public String challengeOrganiserAllowed() {
    return "challengeOrganiserAllowed:  @RolesAllowed(\"challenge-organiser\")";
  }

  @GET
  @RolesAllowed({"challenge-organiser", "benchmark-provider"})
  @Produces(MediaType.TEXT_PLAIN)
  @Path("someRolesAllowed")
  public String someRolesAllowed() {
    return "someRolesAllowed: @RolesAllowed({\"challenge-organiser\", \"benchmark-provider\"})";
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("roleNames")
  public String roleNames(@Context SecurityContext sc) {
    String[] roleNames = Application.getRoleNames(sc);
    return "Your roles are: " + Arrays.toString(roleNames);
  }

}
