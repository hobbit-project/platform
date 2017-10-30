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

<<<<<<< HEAD
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
||||||| merged common ancestors
import java.util.*;
=======
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
>>>>>>> master
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

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

import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;
import de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
<<<<<<< HEAD
import de.usu.research.hobbit.gui.rest.beans.ConfiguredBenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentCountBean;
import de.usu.research.hobbit.gui.rest.beans.NamedEntityBean;
import de.usu.research.hobbit.gui.rest.beans.SystemBean;
import de.usu.research.hobbit.gui.rest.beans.UserInfoBean;
||||||| merged common ancestors
=======
import de.usu.research.hobbit.gui.rest.beans.ConfiguredBenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentBean;
import de.usu.research.hobbit.gui.rest.beans.ExperimentCountBean;
import de.usu.research.hobbit.gui.rest.beans.NamedEntityBean;
import de.usu.research.hobbit.gui.rest.beans.UserInfoBean;
>>>>>>> master

@Path("experiments")
public class ExperimentsResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentsResources.class);

    private static final String UNKNOWN_EXP_ERROR_MSG = "Could not find results for this experiments. Either the experiment has not been finished or it does not exist.";

<<<<<<< HEAD
    private UserInfoBean getUserInfo(SecurityContext sc) {
        // get user
        UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
        return userInfo;
    }

    private Set<String> getUserSystemIds(UserInfoBean userInfo) {
        if (userInfo.hasRole("system-provider") || userInfo.hasRole("challenge-organiser")) {
            List<SystemBean> userSystems = PlatformControllerClientSingleton.getInstance()
                    .requestSystemsOfUser(userInfo.getEmail());
            // create set of user owned system ids
            String[] sysIds = userSystems.stream().map(s -> s.getId()).toArray(String[]::new);
            Set<String> userOwnedSystemIds = new HashSet<>(Arrays.asList(sysIds));
            return userOwnedSystemIds;
        } else {
            // Guests don't have systems
            return new TreeSet<>();
        }
    }

    private Set<String> getUserSystemIds(SecurityContext sc) {
        UserInfoBean userInfo = getUserInfo(sc);
        Set<String> userOwnedSystemIds = getUserSystemIds(userInfo);
        return userOwnedSystemIds;
    }
||||||| merged common ancestors
    private UserInfoBean getUserInfo(SecurityContext sc) {
        // get user
        UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
        return userInfo;
    }

    private Set<String> getUserSystemIds(UserInfoBean userInfo) {
        List<SystemBean> userSystems = PlatformControllerClientSingleton.getInstance().requestSystemsOfUser(userInfo.getPreferredUsername());
        // create set of user owned system ids
        String[] sysIds = userSystems.stream().map(s -> s.getId()).toArray(String[]::new);
        Set<String> userOwnedSystemIds = new HashSet<>(Arrays.asList(sysIds));
        return userOwnedSystemIds;
    }

    private Set<String> getUserSystemIds(SecurityContext sc) {
        UserInfoBean userInfo = getUserInfo(sc);
        Set<String> userOwnedSystemIds = getUserSystemIds(userInfo);
        return userOwnedSystemIds;
    }
=======
//    private UserInfoBean getUserInfo(SecurityContext sc) {
//        // get user
//        UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
//        return userInfo;
//    }
>>>>>>> master

    @GET
    @Path("query")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ExperimentBean> query(@QueryParam("id") String idsCommaSep,
            @QueryParam("challenge-task-id") String challengeTaskId, @Context SecurityContext sc) throws Exception {
        List<ExperimentBean> results = null;
        String[] ids = null;
        if (idsCommaSep != null) {
            ids = idsCommaSep.split(",");
        }

        if (Application.isUsingDevDb()) {
            return getDevDb().queryExperiments(ids, challengeTaskId);
        } else {

            if (ids != null) {
                LOGGER.debug("Querying experiment results for " + Arrays.toString(ids));
                results = new ArrayList<>(ids.length);
                for (String id : ids) {
                    // create experiment URI
                    String experimentUri = Constants.EXPERIMENT_URI_NS + id;
                    // construct public query
                    String query = SparqlQueries.getExperimentGraphQuery(experimentUri,
                            Constants.PUBLIC_RESULT_GRAPH_URI);
                    // get public experiment
                    Model model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
                    if (model != null && model.size() > 0) {
                        results.add(RdfModelHelper.createExperimentBean(model, model.getResource(experimentUri)));
                    } else {
                        // if public experiment is not found
                        // try requesting model from private graph
                        query = SparqlQueries.getExperimentGraphQuery(experimentUri,
                                Constants.PRIVATE_RESULT_GRAPH_URI);
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
                                    results.add(RdfModelHelper.createExperimentBean(model,
                                            model.getResource(experimentUri)));
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
                String query = SparqlQueries.getExperimentOfTaskQuery(null, challengeTaskId,
                        Constants.PUBLIC_RESULT_GRAPH_URI);
                // get public experiment
                Model model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
                // if model is public and available - go with it
                if (model != null && model.size() > 0) {
                    results = RdfModelHelper.createExperimentBeans(model);
                } else {
                    // otherwise try to look in private graph
                    query = SparqlQueries.getExperimentOfTaskQuery(null, challengeTaskId,
                            Constants.PRIVATE_RESULT_GRAPH_URI);
                    model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
                    if (model != null && model.size() > 0) {
                        // get challenge organizer
                        Resource challengeTask = model.getResource(challengeTaskId);
                        // get challenge info
                        String challengeQuery = SparqlQueries.getChallengeTaskOrganizer(challengeTaskId, null);
                        Model challengeModel = StorageServiceClientSingleton.getInstance()
                                .sendConstructQuery(challengeQuery);
                        Resource challenge = RdfHelper.getObjectResource(challengeModel, challengeTask,
                                HOBBIT.isTaskOf);
                        if (challenge != null) {
                            String organizer = RdfHelper.getStringValue(challengeModel, challenge, HOBBIT.organizer);
                            UserInfoBean userInfo = getUserInfo(sc);
                            if (organizer != null) {
                                // check if organizer is user
                                // return whole thing if he is
                                if (organizer.equals(userInfo.getPreferredUsername())) {
                                    results = RdfModelHelper.createExperimentBeans(model);
                                }
                            } else {
                                LOGGER.error(
                                        "Couldn't get organizer for task {}. Falling back to retrieving experiments that can be seen by the user in his role as system owner.",
                                        challengeTaskId);
                            }
                            if (results == null) {
                                // if the user is not the organizer, iterate over the beans and remove all beans
                                // that are not owned by the user
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

        return results != null ? results : new ArrayList<>(0);
    }

    @GET
    @Path("count-by-challenge/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ExperimentCountBean> countByChallengeTaskIds(@PathParam("id") String challengeId) throws Exception {
        if (Application.isUsingDevDb()) {
            return getDevDb().countByChallengeTaskIds(challengeId);
        } else {
            /*
             * 1. retrieve the tasks of the given challenge
             *
             * 2. count the experiments for the single tasks
             *
             * Note that we do not use a single, large query that selects all at once
             * because the tasks and the experiment results can be located in different
             * graphs.
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
            return counts;
        }
    }

    // /**
    // * Adds benchmark and system labels and descriptions to the given
    // experiment
    // * beans.
    // *
    // * @param experiments
    // * the experiments beans that should be updated with the
    // * retrieved information
    // * @throws Exception
    // * if the communication with the platform controller does not
    // * work
    // */
    // protected void addInfoFromController(List<ExperimentBean> experiments)
    // throws Exception {
    // PlatformControllerClient client =
    // PlatformControllerClientSingleton.getInstance();
    // if (client != null) {
    // // Go through the list of experiments and sort them regarding their
    // // benchmark and system info
    // Map<String, List<ExperimentBean>> benchmarksToExperiments = new
    // HashMap<>();
    // Map<String, List<ExperimentBean>> systemsToExperiments = new HashMap<>();
    // List<ExperimentBean> experiments;
    // for (ExperimentBean experiment : experiments) {
    // if (benchmarksToExperiments.containsKey(experiment.benchmark.id)) {
    // experiments = benchmarksToExperiments.get(experiment.benchmark.id);
    // } else {
    // experiments = new ArrayList<>();
    // benchmarksToExperiments.put(experiment.benchmark.id, beans);
    // }
    // experiments.add(experiment);
    // if (systemsToExperiments.containsKey(experiment.system.id)) {
    // experiments = systemsToExperiments.get(experiment.system.id);
    // } else {
    // experiments = new ArrayList<>();
    // systemsToExperiments.put(experiment.system.id, beans);
    // }
    // experiments.add(experiment);
    // }
    // BenchmarkBean requestedBenchmarkInfo;
    // for (String benchmarkUri : benchmarksToExperiments.keySet()) {
    // requestedBenchmarkInfo = client.requestBenchmarkDetails(benchmarkUri,
    // null);
    // // replace the benchmarks with the retrieved info
    // experiments = benchmarksToExperiments.get(benchmarkUri);
    // for (ExperimentBean experiment : experiments) {
    // experiment.setBenchmark(requestedBenchmarkInfo);
    // }
    // }
    // } else {
    // throw new Exception("Couldn't get platform controller client.");
    // }
    // }

    private DevInMemoryDb getDevDb() {
        return DevInMemoryDb.theInstance;
    }
}
