package de.usu.research.hobbit.gui.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

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
	public List<BenchmarkBean> listAll() throws Exception {
		LOGGER.info("List benchmarks ...");
		List<BenchmarkBean> benchmarks;
		if (Application.isUsingDevDb()) {
			benchmarks = getDevDb().getBenchmarks();
		} else {

			PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
			if (client == null) {
				throw new GUIBackendException("Couldn't connect to platform controller.");
			}
			benchmarks = client.requestBenchmarks();
		}
		return benchmarks;
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public BenchmarkBean getById(@Context SecurityContext sc, @PathParam("id") String id) throws Exception {
		PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
		BenchmarkBean benchmarkDetails;
		List<BenchmarkBean> benchmarks;

		if (Application.isUsingDevDb()) {
			benchmarks = getDevDb().getBenchmarks();
			for (BenchmarkBean benchmarkBean : benchmarks) {
				if (benchmarkBean.getId().equals(id))
					return benchmarkBean;
			}
			return null;
		} else {
			if (client == null) {
				throw new GUIBackendException("Couldn't connect to platform controller.");
			}
			UserInfoBean user = InternalResources.getUserInfoBean(sc);
			benchmarkDetails = client.requestBenchmarkDetails(id, user);
			return benchmarkDetails;
		}

	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public SubmitResponseBean submitBenchmark(SubmitModelBean model) throws Exception {
		LOGGER.info("Submit benchmark id = " + model.getBenchmark());
		LOGGER.info("Submit system id = " + model.getSystem());
		PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
		if (client == null) {
			throw new GUIBackendException("Couldn't connect to platform controller.");
		}
		String id = client.submitBenchmark(model);
		return new SubmitResponseBean(id);
	}
}