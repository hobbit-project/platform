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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;
import de.usu.research.hobbit.gui.rabbitmq.RdfModelCreationHelper;
import de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.BenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeTaskBean;
import de.usu.research.hobbit.gui.rest.beans.EmptyBean;
import de.usu.research.hobbit.gui.rest.beans.IdBean;
import de.usu.research.hobbit.gui.rest.beans.InfoBean;
import de.usu.research.hobbit.gui.rest.beans.UserInfoBean;

@Path("challenges")
public class ChallengesResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChallengesResources.class);

    private static DevInMemoryDb getDevDb() {
        return DevInMemoryDb.theInstance;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAll(@Context SecurityContext sc) {
        UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
        LOGGER.info("List challenges for " + userInfo.getPreferredUsername() + " ...");
        List<ChallengeBean> challenges = null;

        if (Application.isUsingDevDb()) {
            challenges = getDevDb().getChallenges();
        } else {
            // String query = SparqlQueries.getShallowChallengeGraphQuery(null,
            // Constants.CHALLENGE_DEFINITION_GRAPH_URI);
            String query = SparqlQueries.getShallowChallengeGraphQuery(null, null);
            if (query != null) {
                StorageServiceClient storageClient = StorageServiceClientSingleton.getInstance();
                if (storageClient != null) {
                    Model model = storageClient.sendConstructQuery(query);
                    challenges = RdfModelHelper.listChallenges(model);
                }
            }
        }
        List<ChallengeBean> list = new ArrayList<>();
        if (challenges != null) {
            if (userInfo.hasRole("challenge-organiser")) {
                list.addAll(challenges);
            } else {
                list.addAll(challenges.stream().filter(ChallengeBean::isVisible).collect(Collectors.toList()));
            }
        }
        return Response.ok(new GenericEntity<List<ChallengeBean>>(list) {
        }).build();
    }

    /**
     * Adds benchmark and system labels and descriptions to the given challenge
     * bean. The given user info is used to check the access of the user regarding
     * this information.
     *
     * @param challenge
     *            the challenge bean that should be updated with the retrieved
     *            information
     * @param userInfo
     *            the information about the requesting user
     * @throws Exception
     *             if the communication with the platform controller does not work
     */
    @Deprecated
    protected void addInfoFromController(ChallengeBean challenge, UserInfoBean userInfo) throws Exception {
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        if (client != null) {
            BenchmarkBean requestedBenchmarkInfo;
            for (ChallengeTaskBean task : challenge.getTasks()) {
                requestedBenchmarkInfo = client.requestBenchmarkDetails(task.getBenchmark().getId(),
                        (userInfo.getPreferredUsername().equals(challenge.getOrganizer())) ? null : userInfo);
                // remove all systems that are not registered for the task from
                // the retrieved benchmark bean
                requestedBenchmarkInfo.getSystems().retainAll(CollectionUtils
                        .intersection(task.getBenchmark().getSystems(), requestedBenchmarkInfo.getSystems()));
                // replace the benchmark bean with the retrieved one (which has
                // more information)
                task.setBenchmark(requestedBenchmarkInfo);
            }
        } else {
            throw new Exception("Couldn't get platform controller client.");
        }
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") String id, @Context SecurityContext sc) {
        ChallengeBean bean = ChallengesResources.getChallenge(id, sc);
        if (bean != null)
            return Response.ok(bean).build();
        return Response.status(Response.Status.NOT_FOUND).entity(InfoBean.withMessage("Challenge " + id + " not found"))
                .build();
    }

    static ChallengeBean getChallenge(String id, SecurityContext sc) {
        if (Application.isUsingDevDb()) {
            for (ChallengeBean item : getDevDb().getChallenges()) {
                if (item.getId().equals(id)) {
                    return item;
                }
            }
        } else {
            // UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
            String query = SparqlQueries.getChallengeGraphQuery(id, null);
            if (query != null) {
                StorageServiceClient storageClient = StorageServiceClientSingleton.getInstance();
                if (storageClient != null) {
                    Model model = storageClient.sendConstructQuery(query);
                    for (ChallengeBean item : RdfModelHelper.listChallenges(model)) {
                        if (item.getId().equals(id)) {
                            // try {
                            // addInfoFromController(item, userInfo);
                            // } catch (Exception e) {
                            // LOGGER.error("Couldn't retrieve additional
                            // information for the given challenge.", e);
                            // }
                            return item;
                        }
                    }
                }
            }
        }
        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "challenge-organiser" })
    public Response add(ChallengeBean challenge, @Context SecurityContext sc) {
        // FIXME this should be removed as soon as the gui-client sends
        // challenges containing the preferred user name as owner
        UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
        challenge.setOrganizer(userInfo.getPreferredUsername());
        if (Application.isUsingDevDb()) {
            getDevDb().addChallenge(challenge);
        } else {
            Model model = RdfModelCreationHelper.createNewModel();
            challenge.setId(Constants.CHALLENGE_URI_NS + UUID.randomUUID().toString());
            RdfModelCreationHelper.addChallenge(challenge, model);
            StorageServiceClientSingleton.getInstance().sendInsertQuery(model,
                    Constants.CHALLENGE_DEFINITION_GRAPH_URI);
        }
        return Response.ok(new IdBean(challenge.getId())).build();
    }

    @PUT
    @Path("operation/close/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "challenge-organiser" })
    public Response close(@PathParam("id") String id, EmptyBean dummy) {
        if (Application.isUsingDevDb()) {
            Boolean justClosed = getDevDb().closeChallenge(id);
            if (justClosed != null) {
                if (justClosed) {
                    return Response.ok(InfoBean.withMessage("Challenge has been closed")).build();
                } else {
                    return Response.ok(InfoBean.withMessage("Challenge was already closed")).build();
                }
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(InfoBean.withMessage("Challenge " + id + " not found")).build();
            }
        } else {
            PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
            if (client != null) {
                try {
                    client.closeChallenge(id);
                    return Response.ok(InfoBean.withMessage("Challenge has been closed")).build();
                } catch (IOException ex) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(InfoBean.withMessage(ex.getMessage())).build();
                }
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(InfoBean.withMessage("Couldn't get platform controller client.")).build();
            }
        }
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "challenge-organiser" })
    public Response update(@PathParam("id") String id, ChallengeBean challenge) {
        // UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
        challenge.setId(id);
        if (Application.isUsingDevDb()) {
            String updatedId = getDevDb().updateChallenge(challenge);
            if (updatedId != null) {
                return Response.ok(new IdBean(updatedId)).build();
            }
        } else {
            StorageServiceClient storageClient = StorageServiceClientSingleton.getInstance();
            if (storageClient != null) {
                // get challenge from storage
                Model oldModel = storageClient.sendConstructQuery(
                        SparqlQueries.getChallengeGraphQuery(id, Constants.CHALLENGE_DEFINITION_GRAPH_URI));
                // update the model only if it has been found
                if (oldModel != null) {
                    RdfModelCreationHelper.reduceModelToChallenge(oldModel, oldModel.getResource(challenge.getId()));
                    Model newModel = RdfModelCreationHelper.createNewModel();
                    RdfModelCreationHelper.addChallenge(challenge, newModel);
                    RdfModelCreationHelper.reduceModelToChallenge(newModel, newModel.getResource(challenge.getId()));
                    // Create update query from difference
                    storageClient.sendUpdateQuery(SparqlQueries.getUpdateQueryFromDiff(oldModel, newModel,
                            Constants.CHALLENGE_DEFINITION_GRAPH_URI));
                    return Response.ok(new IdBean(id)).build();
                } else {
                    LOGGER.error(
                            "Couldn't update the challenge {} because it couldn't be loaded from storage. Update will be ignored.",
                            id);
                }
            }
            return Response.ok(new IdBean(id)).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity(InfoBean.withMessage("Challenge " + id + " not found"))
                .build();
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "challenge-organiser" })
    public Response delete(@PathParam("id") String id) {
        if (Application.isUsingDevDb()) {
            String deletedId = getDevDb().deleteChallenge(id);
            if (deletedId != null) {
                return Response.ok(new IdBean(deletedId)).build();
            }
        } else {
            // FIXME check whether the user is allowed to delete this challenge
            // delete challenge from storage
            String query = SparqlQueries.deleteChallengeGraphQuery(id, Constants.CHALLENGE_DEFINITION_GRAPH_URI);
            if (query != null) {
                StorageServiceClient storageClient = StorageServiceClientSingleton.getInstance();
                if (storageClient != null) {
                    if (storageClient.sendUpdateQuery(query)) {
                        // Clean up the remaining graph
                        String queries[] = SparqlQueries
                                .cleanUpChallengeGraphQueries(Constants.CHALLENGE_DEFINITION_GRAPH_URI);
                        for (int i = 0; i < queries.length; ++i) {
                            storageClient.sendUpdateQuery(queries[i]);
                        }
                        return Response.ok(new IdBean(id)).build();
                    }
                }
            }
        }
        return Response.status(Response.Status.NOT_FOUND).entity(InfoBean.withMessage("Challenge " + id + " not found"))
                .build();
    }
}
