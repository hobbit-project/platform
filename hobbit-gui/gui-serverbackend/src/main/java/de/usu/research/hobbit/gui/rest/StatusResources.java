package de.usu.research.hobbit.gui.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.GUIBackendException;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClient;
import de.usu.research.hobbit.gui.rabbitmq.PlatformControllerClientSingleton;

@Path("status")
public class StatusResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusResources.class);

    @GET
    public String getStatus() throws Exception {
        LOGGER.info("Get status ...");
        PlatformControllerClient client = PlatformControllerClientSingleton.getInstance();
        if (client == null) {
            throw new GUIBackendException("Couldn't connect to platform controller.");
        }
        String status = client.requestStatus();
        return status;
    }

}