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
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.Constants;
import org.hobbit.core.data.status.ControllerStatus;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.CC;
import org.hobbit.vocab.XHV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.GUIBackendException;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.*;

@Path("license")
public class LicenseResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusResources.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLicense(@Context SecurityContext sc) throws Exception {
        LOGGER.info("Requesting dataset license...");
        LicenseBean info = new LicenseBean();
        String query = SparqlQueries.getLicenseOfDataset(Constants.PUBLIC_RESULT_GRAPH_URI);
        StorageServiceClient storageClient = StorageServiceClientSingleton.getInstance();
        try {
            Model model = storageClient.sendConstructQuery(query);
            ResIterator iterator = model.listSubjectsWithProperty(RDF.type, DCAT.Dataset);
            Resource dataset;
            if (iterator.hasNext()) {
                dataset = iterator.next();
                Resource license = RdfHelper.getObjectResource(model, dataset, CC.license);
                if (license != null) {
                    String iconURL = null;
                    Resource icon = RdfHelper.getObjectResource(model, license, XHV.icon);
                    if (icon != null) {
                        iconURL = icon.getURI();
                    }

                    info = new LicenseBean(
                        RdfHelper.getStringValue(model, dataset, CC.attributionURL),
                        RdfHelper.getStringValue(model, dataset, CC.attributionName),
                        license.getURI(),
                        RdfHelper.getLabel(model, license),
                        iconURL
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception while executing ");
        }
        return Response.ok(new GenericEntity<LicenseBean>(info) {
        }).build();
    }

}
