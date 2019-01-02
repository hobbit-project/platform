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

import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.hobbit.storage.queries.SparqlQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.AnalysisResultSetBean;
import de.usu.research.hobbit.gui.rest.beans.InfoBean;

@Path("analysis")
public class AnalysisResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisResources.class);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAnalysisResults(@Context SecurityContext sc, @PathParam("id") String benchmarkUri) {
        @SuppressWarnings("unchecked")
        List<AnalysisResultSetBean> results = Collections.EMPTY_LIST;
        try {
            // construct public query
            String query = SparqlQueries.getAnalysisResultsOfBenchmark(benchmarkUri,
                    Constants.PUBLIC_RESULT_GRAPH_URI);
            // get public experiment
            Model model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
            if (model != null && model.size() > 0) {
                LOGGER.trace("Got result for {} from the public graph.", benchmarkUri);
                results = RdfModelHelper.createAnalysisResultSetBeans(model);
                LOGGER.trace("Added result bean of {} to the list of results.", benchmarkUri);
            } else {
                
            }
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(InfoBean.withMessage(ex.getMessage()))
                    .build();
        }
        return Response.ok(new GenericEntity<List<AnalysisResultSetBean>>(results) {
        }).build();
    }
}
