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
package de.usu.research.hobbit.gui.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import org.apache.commons.collections.SetUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hobbit.core.Constants;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;
import de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.BenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeTaskBean;
import de.usu.research.hobbit.gui.rest.beans.ExtendedTaskRegistrationBean;
import de.usu.research.hobbit.gui.rest.beans.InfoBean;
import de.usu.research.hobbit.gui.rest.beans.SystemBean;
import de.usu.research.hobbit.gui.rest.beans.TaskRegistrationBean;
import de.usu.research.hobbit.gui.rest.beans.UserInfoBean;

@Path("system-provider")
public class SystemProviderResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemProviderResources.class);

    // private static final Map<String, Map<String, List<TaskRegistrationBean>>>
    // registrations = new ConcurrentHashMap<>();

    @GET()
    @RolesAllowed("system-provider") // Guests can not access this method
    @Path("systems")
    @Produces(MediaType.APPLICATION_JSON)
    public Response processSystems(@Context SecurityContext sc) {
        List<SystemBean> list = InternalResources.getUserSystemBeans(sc);
        return Response.ok(new GenericEntity<List<SystemBean>>(list) {
        }).build();
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
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(InfoBean.withMessage("Challenge " + challengeId + " not found")).build();

        StorageServiceClient storage = StorageServiceClientSingleton.getInstance();
        Model challengeModel = storage.sendConstructQuery(
                SparqlQueries.getChallengeGraphQuery(challengeId, Constants.CHALLENGE_DEFINITION_GRAPH_URI));
        List<TaskRegistrationBean> result = RdfModelHelper.listRegisteredSystems(challengeModel);
        // make sure that the user is the owner of the challenge
        if (!userInfo.getPreferredUsername().equals(challenge.getOrganizer())) {
            // if not, iterate over the beans and remove all beans that are not owned by the
            // current user
            if (result != null) {
                Set<String> userOwnedSystemIds = InternalResources.getUserSystemIds(userInfo);
                result = result.stream().filter(reg -> userOwnedSystemIds.contains(reg.getSystemId()))
                        .collect(Collectors.toList());
            }
        }
        return Response.ok(new GenericEntity<List<TaskRegistrationBean>>(result) {
        }).build();
    }

    @GET
    @RolesAllowed("system-provider")
    @Path("challenge-registrations/{challengeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response processChallengeRegistrations(@Context SecurityContext sc,
            @PathParam("challengeId") String challengeId) {
        return Response
                .ok(new GenericEntity<List<ExtendedTaskRegistrationBean>>(getChallengeRegistrations(sc, challengeId)) {
                }).build();
    }

    @SuppressWarnings("unchecked")
    private List<ExtendedTaskRegistrationBean> getChallengeRegistrations(SecurityContext sc, String challengeId) {
        // Get the list of registered systems
        StorageServiceClient storage = StorageServiceClientSingleton.getInstance();
        Model challengeModel = storage.sendConstructQuery(
                SparqlQueries.getChallengeGraphQuery(challengeId, Constants.CHALLENGE_DEFINITION_GRAPH_URI));
        List<TaskRegistrationBean> registrations = RdfModelHelper.listRegisteredSystems(challengeModel);
        Map<String, List<TaskRegistrationBean>> registrationsPerTask = registrations.stream()
                .collect(Collectors.groupingBy(r -> r.getTaskId()));
        // Get the list of systems that would fit to the single task registrations
        List<ChallengeTaskBean> challengeTasks = RdfModelHelper.listChallengeTasks(challengeModel,
                challengeModel.getResource(challengeId));
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        UserInfoBean user = InternalResources.getUserInfoBean(sc);
        List<ExtendedTaskRegistrationBean> visibleRegistrations = new ArrayList<>();
        for (ChallengeTaskBean task : challengeTasks) {
            Set<String> registeredSystemUris;
            if (registrationsPerTask.containsKey(task.getId())) {
                registeredSystemUris = registrationsPerTask.get(task.getId()).stream().map(r -> r.getSystemId())
                        .collect(Collectors.toSet());
            } else {
                registeredSystemUris = SetUtils.EMPTY_SET;
            }
            // get the list of available systems
            try {
                BenchmarkBean benchmark = client.requestBenchmarkDetails(task.getBenchmark().getId(), user);
                benchmark.getSystems().stream()
                        // Create task registration beans
                        .map(s -> new ExtendedTaskRegistrationBean(challengeId, task.getId(), s,
                                registeredSystemUris.contains(s.getId())))
                        // Add all registrations to the list of visible registrations
                        .forEach(r -> visibleRegistrations.add(r));
            } catch (Exception e) {
                LOGGER.error("Exception while requesting benchmark details.", e);
            }
        }
        return visibleRegistrations;
    }

    // Removed the method since nobody seems to use it
    // @PUT
    // @RolesAllowed("system-provider")
    // @Path("challenge-registrations/{challengeId}")
    // @Consumes(MediaType.APPLICATION_JSON)
    // public Response updateChallengeRegistrations(@Context SecurityContext sc,
    // @PathParam("challengeId") String challengeId, List<TaskRegistrationBean>
    // list) {
    // // tasdId == null is not allowed, anymore!
    // doUpdateChallengeRegistrations(sc, challengeId, list, null);
    // return Response.status(Response.Status.NO_CONTENT).build();
    // }

    private void doUpdateChallengeRegistrations(@Context SecurityContext sc,
            @PathParam("challengeId") String challengeId, List<TaskRegistrationBean> registrations, String taskId) {
        // check whether the user is allowed to change the given registrations,
        // i.e., whether he is allowed to see the systems.
        Set<String> visibleSystems = InternalResources.getUserSystemIds(sc);
        Model newSystemTaskMappingModel = ModelFactory.createDefaultModel();
        Model oldSystemTaskMappingModel = ModelFactory.createDefaultModel();
        registrations.stream().filter(r -> visibleSystems.contains(r.getSystemId())).forEach(r -> {
            Model model = r.isRegistered() ? newSystemTaskMappingModel : oldSystemTaskMappingModel;
            model.add(model.getResource(r.getTaskId()), HOBBIT.involvesSystemInstance,
                    model.getResource(r.getSystemId()));
        });

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
