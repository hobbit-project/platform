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

import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.StringWriter;

@Path("submissions")
public class SubmissionsResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionsResources.class);

    @GET
    @Path("{id}")
    public Response getSubmissionDetails(@PathParam("id") String id) throws Exception {
        LOGGER.info("Retrieve result for " + id);
        StorageServiceClient client = StorageServiceClientSingleton.getInstance();
        String query = SparqlQueries.getExperimentGraphQuery(Constants.EXPERIMENT_URI_NS + id,
            Constants.PUBLIC_RESULT_GRAPH_URI);
        if ((client != null) && (query != null)) {
            LOGGER.info("Sendting SPARQL query to storage service...");

            Model resultModel = client.sendConstructQuery(query);
            StringWriter writer = new StringWriter();
            resultModel.write(writer, "TTL");
            return Response.ok(writer.toString()).build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST).entity("ERROR").build();
        }
    }

}
