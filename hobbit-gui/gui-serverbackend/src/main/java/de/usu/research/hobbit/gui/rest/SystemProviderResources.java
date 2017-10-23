/**
 * This file is part of gui-serverbackend.
 * <p>
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rest;

import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;
import de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.ChallengeBean;
import de.usu.research.hobbit.gui.rest.beans.InfoBean;
import de.usu.research.hobbit.gui.rest.beans.SystemBean;
import de.usu.research.hobbit.gui.rest.beans.TaskRegistrationBean;
import de.usu.research.hobbit.gui.rest.beans.UserInfoBean;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hobbit.core.Constants;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("system-provider")
public class SystemProviderResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemProviderResources.class);

    @GET()
    @RolesAllowed("system-provider")
    @Path("systems")
    @Produces(MediaType.APPLICATION_JSON)
    public Response processSystems(@Context SecurityContext sc) {
        List<SystemBean> list = getSystems(sc);
        return Response.ok(new GenericEntity<List<SystemBean>>(list) {
        }).build();
    }

    private List<SystemBean> getSystems(SecurityContext sc) {
        UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
        LOGGER.info("getSystems for " + userInfo.getPreferredUsername());
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        if (client != null) {
            return client.requestSystemsOfUser(userInfo.getPreferredUsername());
        } else {
            LOGGER.error("Couldn't get platform controller client. Returning empty list.");
            return new LinkedList<>();
        }
    }

    @GET
    @Path("challenge-all-registrations/{challengeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllChallengeRegistrations(@Context SecurityContext sc,
                                                 @PathParam("challengeId") String challengeId) {
        UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
        LOGGER.info("get registered systems for challenge {} and user {}.", challengeId,
            userInfo.getPreferredUsername());

        // Retrieve the registrations
        ChallengeBean challenge = ChallengesResources.getChallenge(challengeId, sc);
        if (challenge == null)
            return Response.status(Response.Status.NOT_FOUND).entity(new InfoBean("Challenge does not exist")).build();

        StorageServiceClient storage = StorageServiceClientSingleton.getInstance();
        Model challengeModel = storage.sendConstructQuery(SparqlQueries.getChallengeGraphQuery(challengeId, null));
        List<TaskRegistrationBean> result = RdfModelHelper.listRegisteredSystems(challengeModel);
        // make sure that the user is the owner of the challenge
        if (!userInfo.getPreferredUsername().equals(challenge.getOrganizer())) {
            // if not, iterate over the beans and remove all beans that are not owned by the current user
            if (result != null) {
                Set<String> userOwnedSystemIds = InternalResources.getUserSystemIds(userInfo);
                result = result.stream()
                    .filter(reg -> userOwnedSystemIds.contains(reg.getSystemId()))
                    .collect(Collectors.toList());
            }
        }
        if (result == null)
            result = new ArrayList<>(0);
        return Response.ok(new GenericEntity<List<TaskRegistrationBean>>(result) {
        }).build();
    }

    @GET
    @RolesAllowed("system-provider")
    @Path("challenge-registrations/{challengeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response processChallengeRegistrations(@Context SecurityContext sc,
                                                  @PathParam("challengeId") String challengeId) {
        return Response.ok(new GenericEntity<List<TaskRegistrationBean>>(getChallengeRegistrations(sc, challengeId)) {
        }).build();
    }

    private List<TaskRegistrationBean> getChallengeRegistrations(SecurityContext sc, String challengeId) {
        // Get the list of registered systems
        StorageServiceClient storage = StorageServiceClientSingleton.getInstance();
        Model challengeModel = storage.sendConstructQuery(SparqlQueries.getChallengeGraphQuery(challengeId, null));
        List<TaskRegistrationBean> registrations = RdfModelHelper.listRegisteredSystems(challengeModel);
        // filter the list based on the systems that are visible for this user
        List<TaskRegistrationBean> visibleRegistrations = new ArrayList<>();
        List<SystemBean> systems = getSystems(sc);
        Set<String> visibleSystems = new HashSet<>();
        for (SystemBean s : systems) {
            visibleSystems.add(s.getId());
        }
        for (TaskRegistrationBean registration : registrations) {
            if (visibleSystems.contains(registration.getSystemId())) {
                visibleRegistrations.add(registration);
            }
        }
        return visibleRegistrations;
    }

    @PUT
    @RolesAllowed("system-provider")
    @Path("challenge-registrations/{challengeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateChallengeRegistrations(@Context SecurityContext sc, @PathParam("challengeId") String challengeId,
                                                 List<TaskRegistrationBean> list) {
        // who is using this method?
        doUpdateChallengeRegistrations(sc, challengeId, list, null);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private void doUpdateChallengeRegistrations(@Context SecurityContext sc, @PathParam("challengeId") String challengeId,
                                                List<TaskRegistrationBean> list, String taskIdToUpdate) {
        // check whether the user is allowed to change the given registrations,
        // i.e., whether he is allowed to see the systems.
        List<SystemBean> systems = getSystems(sc);
        Set<String> visibleSystems = new HashSet<>();
        for (SystemBean s : systems) {
            visibleSystems.add(s.getId());
        }

        // create an RDF model containing the newly defined triples
        Model newSystemTaskMappingModel = ModelFactory.createDefaultModel();
        for (TaskRegistrationBean registration : list) {
            if (visibleSystems.contains(registration.getSystemId())) {
                newSystemTaskMappingModel.add(newSystemTaskMappingModel.getResource(registration.getTaskId()),
                    HOBBIT.involvesSystemInstance,
                    newSystemTaskMappingModel.getResource(registration.getSystemId()));
            }
        }

        // get the old challenge registrations
        List<TaskRegistrationBean> oldList = getChallengeRegistrations(sc, challengeId);
        // create an RDF model containing the old triples
        Model oldSystemTaskMappingModel = ModelFactory.createDefaultModel();
        for (TaskRegistrationBean registration : oldList) {
            if (visibleSystems.contains(registration.getSystemId())) {
                if (taskIdToUpdate != null && !taskIdToUpdate.equals(registration.getTaskId())) {
                    newSystemTaskMappingModel.add(newSystemTaskMappingModel.getResource(registration.getTaskId()),
                        HOBBIT.involvesSystemInstance,
                        newSystemTaskMappingModel.getResource(registration.getSystemId()));
                }
                oldSystemTaskMappingModel.add(oldSystemTaskMappingModel.getResource(registration.getTaskId()),
                    HOBBIT.involvesSystemInstance,
                    oldSystemTaskMappingModel.getResource(registration.getSystemId()));
            }
        }

        // Update the storage
        StorageServiceClient storage = StorageServiceClientSingleton.getInstance();
        if (!storage.sendUpdateQuery(SparqlQueries.getUpdateQueryFromDiff(oldSystemTaskMappingModel,
            newSystemTaskMappingModel, Constants.CHALLENGE_DEFINITION_GRAPH_URI))) {
            LOGGER.error("A problem occurred while updating the system registrations.");
        }
    }

    @PUT
    @RolesAllowed("system-provider")
    @Path("challenge-registrations/{challengeId}/{taskId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateChallengeTaskRegistrations(@Context SecurityContext sc,
                                                     @PathParam("challengeId") String challengeId, @PathParam("taskId") String taskId,
                                                     List<TaskRegistrationBean> list) {
        List<TaskRegistrationBean> registrationsForTask = new ArrayList<>();
        for (TaskRegistrationBean registration : list) {
            if (taskId.equals(registration.getTaskId())) {
                registrationsForTask.add(registration);
            }
        }
        doUpdateChallengeRegistrations(sc, challengeId, registrationsForTask, taskId);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

}
