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

import de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.ConfiguredBenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentCountBean;
import de.usu.research.hobbit.gui.rest.beans.NamedEntityBean;
import de.usu.research.hobbit.gui.rest.beans.UserInfoBean;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.hobbit.core.Constants;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("experiments")
public class ExperimentsResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentsResources.class);

    private static final String UNKNOWN_EXP_ERROR_MSG = "Could not find results for this experiments. Either the experiment has not been finished or it does not exist.";

    @GET
    @Path("query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(@QueryParam("id") String idsCommaSep,
                          @QueryParam("challenge-task-id") String challengeTaskId,
                          @Context SecurityContext sc) throws Exception {
        List<ExperimentBean> results = null;
        String[] ids = null;
        if (idsCommaSep != null) {
            ids = idsCommaSep.split(",");
        }

        if (Application.isUsingDevDb()) {
            return Response.ok(getDevDb().queryExperiments(ids, challengeTaskId)).build();
        } else {
            if (ids != null) {
                LOGGER.debug("Querying experiment results for " + Arrays.toString(ids));
                results = new ArrayList<>(ids.length);
                for (String id : ids) {
                    // create experiment URI
                    String experimentUri = Constants.EXPERIMENT_URI_NS + id;
                    // construct public query
                    String query = SparqlQueries.getExperimentGraphQuery(experimentUri, Constants.PUBLIC_RESULT_GRAPH_URI);
                    // get public experiment
                    Model model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
                    if (model != null && model.size() > 0) {
                        results.add(RdfModelHelper.createExperimentBean(model, model.getResource(experimentUri)));
                    } else {
                        // if public experiment is not found
                        // try requesting model from private graph
                        query = SparqlQueries.getExperimentGraphQuery(experimentUri, Constants.PRIVATE_RESULT_GRAPH_URI);
                        model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
                        if (model != null && model.size() > 0) {
                            // get current experiment system
                            Resource subj = model.getResource(experimentUri);
                            Resource system = RdfHelper.getObjectResource(model, subj, HOBBIT.involvesSystemInstance);
                            if (system != null) {
                                String systemURI = system.getURI();
                                Set<String> userOwnedSystemIds = InternalResources.getUserSystemIds(sc);
                                // check if it's owned by user
                                if (userOwnedSystemIds.contains(systemURI)) {
                                    results.add(RdfModelHelper.createExperimentBean(model, model.getResource(experimentUri)));
                                }
                            }
                        } else {
                            ExperimentBean exp = new ExperimentBean();
                            exp.setId(id);
                            exp.setError(UNKNOWN_EXP_ERROR_MSG);
                            ConfiguredBenchmarkBean benchmark = new ConfiguredBenchmarkBean();
                            benchmark.setConfigurationParamValues(new ArrayList<>());
                            exp.setBenchmark(benchmark);
                            exp.setKpis(new ArrayList<>());
                            results.add(exp);
                        }
                    }
                }
            } else if (challengeTaskId != null) {
                LOGGER.debug("Querying experiment results for challenge task " + challengeTaskId);
                // create experiment URI from public results graph
                String query = SparqlQueries.getExperimentOfTaskQuery(null, challengeTaskId, Constants.PUBLIC_RESULT_GRAPH_URI);
                // get public experiment
                Model model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
                // if model is public and available - go with it
                if (model != null && model.size() > 0) {
                    results = RdfModelHelper.createExperimentBeans(model);
                } else {
                    // otherwise try to look in private graph
                    query = SparqlQueries.getExperimentOfTaskQuery(null, challengeTaskId, Constants.PRIVATE_RESULT_GRAPH_URI);
                    model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
                    if (model != null && model.size() > 0) {
                        // get challenge organizer
                        Resource challengeTask = model.getResource(challengeTaskId);
                        // get challenge info
                        String challengeQuery = SparqlQueries.getChallengeTaskOrganizer(challengeTaskId, null);
                        Model challengeModel = StorageServiceClientSingleton.getInstance().sendConstructQuery(challengeQuery);

                        Resource challenge = RdfHelper.getObjectResource(challengeModel, challengeTask, HOBBIT.isTaskOf);
                        if(challenge != null) {
                            String organizer = RdfHelper.getStringValue(challengeModel, challenge, HOBBIT.organizer);
                            UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
                            if (organizer != null) {
                                // check if organizer is user
                                // return whole thing if he is
                                if (organizer.equals(userInfo.getPreferredUsername())) {
                                    results = RdfModelHelper.createExperimentBeans(model);
                                }
                            } else {
                                LOGGER.error("Couldn't get organizer for task {}. Falling back to retrieving experiments that can be seen by the user in his role as system owner.",
                                        challengeTaskId);
                            }
                            if(results == null) {
                                // if the user is not the organizer, iterate over the beans and remove all beans that are not owned by the user
                                List<ExperimentBean> experiments = RdfModelHelper.createExperimentBeans(model);
                                if (experiments != null) {
                                    Set<String> userOwnedSystemIds = InternalResources.getUserSystemIds(userInfo);
                                    results = experiments.stream()
                                        .filter(exp -> userOwnedSystemIds.contains(exp.getSystem().getId()))
                                        .collect(Collectors.toList());
                                }
                            }
                        } else {
                            LOGGER.error("Couldn't find the challenge of challenge task {}.", challengeTaskId);
                        }
                    } else {
                        LOGGER.info("Couldn't find experiments for task {}. Returning empty list.", challengeTaskId);
                        results = new ArrayList<>(0);
                    }
                }
            } else {
                // TODO make sure that the user is allowed to see the
                // experiment!
                results = RdfModelHelper.createExperimentBeans(StorageServiceClientSingleton.getInstance()
                    .sendConstructQuery(SparqlQueries.getShallowExperimentGraphQuery(null, null)));
            }
        }
        if (results == null)
            results = new ArrayList<>(0);
        return Response.ok(new GenericEntity<List<ExperimentBean>>(results) {
        }).build();
    }

    @GET
    @Path("count-by-challenge/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response countByChallengeTaskIds(@PathParam("id") String challengeId) {
        if (Application.isUsingDevDb()) {
            return Response.ok(getDevDb().countByChallengeTaskIds(challengeId)).build();
        } else {
            /*
             * 1. retrieve the tasks of the given challenge
             *
             * 2. count the experiments for the single tasks
             *
             * Note that we do not use a single, large query that selects all at
             * once because the tasks and the experiment results can be located
             * in different graphs.
             */
            List<ExperimentCountBean> counts = new ArrayList<>();
            String query = SparqlQueries.getChallengeTasksQuery(challengeId, null);
            StorageServiceClient storageClient = StorageServiceClientSingleton.getInstance();
            if (query != null) {
                try {
                    Model challengeTasksModel = storageClient.sendConstructQuery(query);
                    ResIterator iterator = challengeTasksModel.listSubjectsWithProperty(HOBBIT.isTaskOf,
                        challengeTasksModel.getResource(challengeId));
                    Resource taskResource;
                    while (iterator.hasNext()) {
                        taskResource = iterator.next();
                        query = SparqlQueries.countExperimentsOfTaskQuery(taskResource.getURI(), null);
                        ResultSet results = storageClient.sendSelectQuery(query);
                        while (results.hasNext()) {
                            QuerySolution solution = results.next();
                            counts.add(new ExperimentCountBean(
                                new NamedEntityBean(taskResource.getURI(),
                                    RdfHelper.getLabel(challengeTasksModel, taskResource),
                                    RdfHelper.getDescription(challengeTasksModel, taskResource)),
                                solution.getLiteral("count").getInt()));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception while executing ");
                }
            }

            return Response.ok(new GenericEntity<List<ExperimentCountBean>>(counts) {
            }).build();
        }
    }

    private DevInMemoryDb getDevDb() {
        return DevInMemoryDb.theInstance;
    }
}
