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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import de.usu.research.hobbit.gui.rest.beans.InfoBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.GUIBackendException;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.BenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.SubmitModelBean;
import de.usu.research.hobbit.gui.rest.beans.SubmitResponseBean;
import de.usu.research.hobbit.gui.rest.beans.UserInfoBean;

@Path("benchmarks")
public class BenchmarksResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarksResources.class);

    private DevInMemoryDb getDevDb() {
        return DevInMemoryDb.theInstance;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAll() {
        LOGGER.info("List benchmarks ...");
        List<BenchmarkBean> benchmarks;
        if (Application.isUsingDevDb()) {
            benchmarks = getDevDb().getBenchmarks();
        } else {
            try {
                PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
                if (client == null) {
                    throw new GUIBackendException("Couldn't connect to platform controller.");
                }
                benchmarks = client.requestBenchmarks();
            } catch (Exception ex) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(InfoBean.withMessage(ex.getMessage())).build();
            }
        }
        return Response.ok(new GenericEntity<List<BenchmarkBean>>(benchmarks) {
        }).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@Context SecurityContext sc, @PathParam("id") String id) {
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        BenchmarkBean benchmarkDetails;
        List<BenchmarkBean> benchmarks;

        if (Application.isUsingDevDb()) {
            benchmarks = getDevDb().getBenchmarks();
            for (BenchmarkBean benchmarkBean : benchmarks) {
                if (benchmarkBean.getId().equals(id))
                    return Response.ok(benchmarkBean).build();
            }
            return null;
        } else {
            try {
                if (client == null) {
                    throw new GUIBackendException("Couldn't connect to platform controller.");
                }
                UserInfoBean user = InternalResources.getUserInfoBean(sc);
                benchmarkDetails = client.requestBenchmarkDetails(id, user);
                // If a Guest is requesting details, he shouldn't see any systems
                if ((!user.hasRole("system-provider")) && (!user.hasRole("challenge-organiser"))) {
                    benchmarkDetails.setSystems(new ArrayList<>(0));
                }
                return Response.ok(benchmarkDetails).build();
            } catch (Exception ex) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(InfoBean.withMessage(ex.getMessage())).build();
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response submitBenchmark(@Context SecurityContext sc, SubmitModelBean model) {
        try {
            LOGGER.info("Submit benchmark id = " + model.getBenchmark());
            LOGGER.info("Submit system id = " + model.getSystem());
            UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
            PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
            if (client == null) {
                throw new GUIBackendException("Couldn't connect to platform controller.");
            }
            String id = client.submitBenchmark(model, userInfo.getPreferredUsername());
            return Response.ok(new SubmitResponseBean(id)).build();
        } catch (Exception e) {
            LOGGER.warn("Failed to submit benchmark: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(InfoBean.withMessage(e.getMessage()))
                    .build();
        }
    }

    // TODO: we should also accept RDF data to start an experiment
//    @POST
//    @Consumes("text/turtle")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response submitBenchmark(@Context SecurityContext sc, String modelString) {
//        try {
//            UserInfoBean userInfo = InternalResources.getUserInfoBean(sc);
//            PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
//            if (client == null) {
//                throw new GUIBackendException("Couldn't connect to platform controller.");
//            }
//            String id = client.submitBenchmark(model, userInfo.getPreferredUsername());
//            return Response.ok(new SubmitResponseBean(id)).build();
//        } catch (Exception e) {
//            LOGGER.warn("Failed to submit benchmark: " + e.getMessage());
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(InfoBean.withMessage(e.getMessage())).build();
//        }
//    }

}
