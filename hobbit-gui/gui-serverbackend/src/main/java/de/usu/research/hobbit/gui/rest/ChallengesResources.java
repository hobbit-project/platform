package de.usu.research.hobbit.gui.rest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.RdfModelCreationHelper;
import de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;

@Path("challenges")
public class ChallengesResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChallengesResources.class);

    private static AtomicInteger nextId = new AtomicInteger();
    // TODO please remove tmpdb when integrating into the platform
    @Deprecated
    private static List<ChallengeBean> tmpdb = new ArrayList<>();
    // TODO please remove USE_TMP_DB when integrating into the platform
    @Deprecated
    private static boolean USE_TMP_DB = true;

    @GET
    @RolesAllowed({"challenge-organiser", "system-provider"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<ChallengeBean> listAll(@Context SecurityContext sc) throws Exception {
        loadIfNeeded();
        
        UserInfoBean bean = InternalResources.getUserInfoBean(sc);
        LOGGER.info("List challenges for " + bean.getPreferredUsername() + " ...");
        List<ChallengeBean> challenges = null;

        if (USE_TMP_DB) {
            challenges = tmpdb;
        } else {
            String query = SparqlQueries.getChallengeGraphQuery(null, Constants.CHALLENGE_DEFINITION_GRAPH_URI);
            if (query != null) {
                StorageServiceClient storageClient = StorageServiceClientSingleton.getInstance();
                if (storageClient != null) {
                    Model model = storageClient.sendConstructQuery(query);
                    challenges = RdfModelHelper.listChallenges(model);
                }
            }
        }
        List<ChallengeBean> list = new ArrayList<>();
        if(challenges != null) {
            if (bean.hasRole("challenge-organiser")) {
              list.addAll(challenges);
            } else {
              for (ChallengeBean b: challenges) {
                if (b.isPublished()) {
                  list.add(b);
                }
              }
            }
        }

        return list;
    }
    
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ChallengeBean getById(@PathParam("id") String id) throws Exception {
        loadIfNeeded();
        if (USE_TMP_DB) {
            for (ChallengeBean item : tmpdb) {
                if (item.getId().equals(id)) {
                    return item;
                }
            }
        } else {
            String query = SparqlQueries.getChallengeGraphQuery(null, Constants.CHALLENGE_DEFINITION_GRAPH_URI);
            if (query != null) {
                StorageServiceClient storageClient = StorageServiceClientSingleton.getInstance();
                if (storageClient != null) {
                    Model model = storageClient.sendConstructQuery(query);
                    for (ChallengeBean item : RdfModelHelper.listChallenges(model)) {
                        if (item.getId().equals(id)) {
                            return item;
                        }
                    }
                }
            }
        }
        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public IdBean add(ChallengeBean challenge) throws Exception {
        challenge.id = "" + challenge.name + "_" + nextId.incrementAndGet();
        if (USE_TMP_DB) {
            tmpdb.add(challenge);
        } else {
            Model model = RdfModelCreationHelper.createNewModel();
            RdfModelCreationHelper.addChallenge(challenge, model);
            StorageServiceClientSingleton.getInstance().sendInsertQuery(model,
                    Constants.CHALLENGE_DEFINITION_GRAPH_URI);
        }

        return new IdBean(challenge.id);
    }

    private void loadIfNeeded() throws JAXBException {
      synchronized(tmpdb) {
        if (tmpdb.isEmpty()) {
          List<ChallengeBean> challenges = loadChallenges();
          tmpdb.addAll(challenges);
        }
      }
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public IdBean update(@PathParam("id") String id, ChallengeBean challenge) throws Exception {
        challenge.id = id;
        if (USE_TMP_DB) {
            for (int i = 0; i < tmpdb.size(); i++) {
                ChallengeBean item = tmpdb.get(i);
                if (item.getId().equals(id)) {
                    tmpdb.set(i, challenge);
                    return new IdBean(id);
                }
            }
        } else {
            // TODO insert challenge to storage
        }

        throw new Exception("Challenge " + id + " not found");
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public IdBean delete(@PathParam("id") String id) throws Exception {
        if (USE_TMP_DB) {
            for (int i = 0; i < tmpdb.size(); i++) {
                ChallengeBean item = tmpdb.get(i);
                if (item.getId().equals(id)) {
                    tmpdb.remove(i);
                    return new IdBean(id);
                }
            }
        } else {
            // TODO insert challenge to storage
        }

        throw new Exception("Challenge " + id + " not found");
    }

    private List<ChallengeBean> loadChallenges() throws JAXBException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("/sample/challenges.json");

        JAXBElement<ChallengesListBean> elem = MarshallerUtil.unmarshall(input, ChallengesListBean.class);
        List<ChallengeBean> challenges = elem.getValue().getChallenges();
        return challenges;
    }
}