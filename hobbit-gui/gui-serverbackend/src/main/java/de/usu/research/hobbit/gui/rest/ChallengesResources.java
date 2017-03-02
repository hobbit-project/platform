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
import java.util.UUID;

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
import javax.ws.rs.core.MediaType;
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

    private DevInMemoryDb getDevDb() {
        return DevInMemoryDb.theInstance;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ChallengeBean> listAll(@Context SecurityContext sc) throws Exception {
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
                for (ChallengeBean b : challenges) {
                    if (b.isVisible()) {
                        list.add(b);
                    }
                }
            }
        }

        return list;
    }

    /**
     * Adds benchmark and system labels and descriptions to the given challenge
     * bean. The given user info is used to check the access of the user
     * regarding this information.
     * 
     * @param challenge
     *            the challenge bean that should be updated with the retrieved
     *            information
     * @param userInfo
     *            the information about the requesting user
     * @throws Exception
     *             if the communication with the platform controller does not
     *             work
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
    public ChallengeBean getById(@PathParam("id") String id, @Context SecurityContext sc) throws Exception {
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
    public IdBean add(ChallengeBean challenge, @Context SecurityContext sc) throws Exception {
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
        return new IdBean(challenge.getId());
    }

    @PUT
    @Path("operation/close/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "challenge-organiser" })
    public InfoBean close(@PathParam("id") String id, EmptyBean dummy) throws Exception {
        if (Application.isUsingDevDb()) {
            Boolean justClosed = getDevDb().closeChallenge(id);
            if (justClosed != null) {
                if (justClosed) {
                    return new InfoBean("Challenge has been closed");
                } else {
                    return new InfoBean("Challenge was already closed");
                }
            } else {
                throw new Exception("Challenge " + id + " not found");
            }
        } else {
            PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
            if (client != null) {
                client.closeChallenge(id);
                return new InfoBean("Challenge has been closed");
            } else {
                throw new Exception("Couldn't get platform controller client.");
            }
        }
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "challenge-organiser" })
    public IdBean update(@PathParam("id") String id, ChallengeBean challenge) throws Exception {
        // UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
        challenge.setId(id);
        if (Application.isUsingDevDb()) {
            String updatedId = getDevDb().updateChallenge(challenge);
            if (updatedId != null) {
                return new IdBean(updatedId);
            }
        } else {
            StorageServiceClient storageClient = StorageServiceClientSingleton.getInstance();
            if (storageClient != null) {
                // get challenge from storage
                Model oldModel = storageClient.sendConstructQuery(
                        SparqlQueries.getChallengeGraphQuery(id, Constants.CHALLENGE_DEFINITION_GRAPH_URI));
                // update the model only if it has been found
                if (oldModel != null) {
                    Model newModel = RdfModelCreationHelper.createNewModel();
                    RdfModelCreationHelper.addChallenge(challenge, newModel);
                    // Create update query from difference
                    storageClient.sendUpdateQuery(SparqlQueries.getUpdateQueryFromDiff(oldModel, newModel,
                            Constants.CHALLENGE_DEFINITION_GRAPH_URI));
                    return new IdBean(id);
                } else {
                    LOGGER.error(
                            "Couldn't update the challenge {} because it couldn't be loaded from storage. Update will be ignored.",
                            id);
                }
            }
            return new IdBean(id);
        }

        throw new Exception("Challenge " + id + " not found");
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "challenge-organiser" })
    public IdBean delete(@PathParam("id") String id) throws Exception {
        if (Application.isUsingDevDb()) {
            String deletedId = getDevDb().deleteChallenge(id);
            if (deletedId != null) {
                return new IdBean(deletedId);
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
                        return new IdBean(id);
                    }
                }
            }
        }

        throw new Exception("Challenge " + id + " not found");
    }
}