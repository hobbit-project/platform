package de.usu.research.hobbit.gui.rest;

import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;

@Path("submissions")
public class SubmissionsResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionsResources.class);

    @GET
    @Path("{id}")
    public String getSubmissionDetails(@PathParam("id") String id) throws Exception {
        LOGGER.info("Retrieve result for " + id);
        StorageServiceClient client = StorageServiceClientSingleton.getInstance();
        String query = SparqlQueries.getExperimentGraphQuery(Constants.EXPERIMENT_URI_NS + id,
                Constants.PUBLIC_RESULT_GRAPH_URI);
        if ((client != null) && (query != null)) {
            LOGGER.info("Sendting SPARQL query to storage service...");

            Model resultModel = client.sendConstructQuery(query);
            StringWriter writer = new StringWriter();
            resultModel.write(writer, "TTL");
            return writer.toString();
        } else {
            return "ERROR";
        }
    }

}
