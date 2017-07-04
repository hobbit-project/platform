/**
 * This file is part of gui-serverbackend.
 *
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
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
  @RolesAllowed({"challenge-organiser", "system-provider"})
  @Produces(MediaType.TEXT_PLAIN)
  @Path("someRolesAllowed")
  public String someRolesAllowed() {
    return "someRolesAllowed: @RolesAllowed({\"challenge-organiser\", \"system-provider\"})";
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("roleNames")
  public String roleNames(@Context SecurityContext sc) {
    String[] roleNames = Application.getRoleNames(sc);
    return "Your roles are: " + Arrays.toString(roleNames);
  }

}
