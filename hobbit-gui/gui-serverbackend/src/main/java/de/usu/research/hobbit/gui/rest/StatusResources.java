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
import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hobbit.core.data.status.ControllerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.GUIBackendException;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.QueuedExperimentBean;
import de.usu.research.hobbit.gui.rest.beans.RunningExperimentBean;
import de.usu.research.hobbit.gui.rest.beans.StatusBean;

@Path("status")
public class StatusResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusResources.class);

    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getStatus() throws Exception {
        LOGGER.info("Get status ...");
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        if (client == null) {
            throw new GUIBackendException("Couldn't connect to platform controller.");
        }
        ControllerStatus status = client.requestStatus();
        Objects.requireNonNull(status, "Couldn't get status of platform.");

        List<QueuedExperimentBean> queueContent = new ArrayList<>(status.queuedExperiments.length);
        for (int i = 0; i < status.queuedExperiments.length; ++i) {
            queueContent.add(new QueuedExperimentBean(status.queuedExperiments[i]));
        }

        RunningExperimentBean runningExperimentBean = null;
        if (status.experiment != null)
            runningExperimentBean = new RunningExperimentBean(status.experiment);
        StatusBean statusBean = new StatusBean(runningExperimentBean, queueContent);

        return Response.ok(new GenericEntity<StatusBean>(statusBean){}).build();
    }

}
