package de.usu.research.hobbit.gui.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.GUIBackendException;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;

@Path("benchmarks")
public class BenchmarksResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarksResources.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<BenchmarkBean> listAll() throws Exception {
        LOGGER.info("List benchmarks ...");
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        if (client == null) {
            throw new GUIBackendException("Couldn't connect to platform controller.");
        }
        List<BenchmarkBean> benchmarks = client.requestBenchmarks();
        return benchmarks;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BenchmarkBean getById(@PathParam("id") String id) throws Exception {
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        if (client == null) {
            throw new GUIBackendException("Couldn't connect to platform controller.");
        }
        BenchmarkBean benchmarkDetails = client.requestBenchmarkDetails(id);
        return benchmarkDetails;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SubmitResponseBean submitBenchmark(SubmitModelBean model) throws Exception {
        LOGGER.info("Submit benchmark id = " + model.benchmark);
        LOGGER.info("Submit system id = " + model.system);
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        if (client == null) {
            throw new GUIBackendException("Couldn't connect to platform controller.");
        }
        String id = client.submitBenchmark(model);
        return new SubmitResponseBean(id);
    }
}