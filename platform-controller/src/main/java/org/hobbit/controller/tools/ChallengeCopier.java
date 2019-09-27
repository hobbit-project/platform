/**
 * This file is part of platform-controller.
 *
 * platform-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * platform-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with platform-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.controller.tools;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.Constants;
import org.hobbit.core.rabbit.DataSender;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.hobbit.utils.EnvVariables;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitChallenges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChallengeCopier extends AbstractPlatformConnectorComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChallengeCopier.class);

    protected DataSender sender2Analysis;
    protected StorageServiceClient storage;

    @Override
    public void init() throws Exception {
        super.init();

        storage = StorageServiceClient.create(outgoingDataQueuefactory.getConnection());
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // nothing to do
    }

    private Calendar parseDate(String date) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(LocalDate.parse(date, DateTimeFormatter.ISO_DATE).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000);
        return c;
    }

    @Override
    public void run() throws Exception {
        String challengeUri = EnvVariables.getString("CHALLENGE_URI", LOGGER);
        String newLabel = EnvVariables.getString("NEW_LABEL", LOGGER);
        Calendar newExecDate = parseDate(EnvVariables.getString("NEW_EXEC_DATE", LOGGER));
        Calendar newPubDate = parseDate(EnvVariables.getString("NEW_PUB_DATE", LOGGER));

        LOGGER.info("Retriving challenge {}...", challengeUri);
        String query = SparqlQueries.getChallengeGraphQuery(challengeUri, null);
        Model source = storage.sendConstructQuery(query);
        Model target = ModelFactory.createDefaultModel();

        Resource sourceChallenge = source.createResource(challengeUri);
        Resource targetChallenge = HobbitChallenges.getChallenge(UUID.randomUUID().toString());

        ResIterator iTasks = source.listSubjectsWithProperty(RDF.type, HOBBIT.ChallengeTask);
        ArrayList<String> sourceTasks = new ArrayList<>();
        while (iTasks.hasNext()) {
            sourceTasks.add(iTasks.next().getURI());
        }
        Collections.sort(sourceTasks);
        HashMap<String,Resource> taskMap = new HashMap<>();
        for (int i = 0; i < sourceTasks.size(); i++) {
            taskMap.put(sourceTasks.get(i), source.createResource(targetChallenge.getURI() + "_task" + i));
        }

        StmtIterator i = source.listStatements();
        while (i.hasNext()) {
            Statement sourceStatement = i.next();
            Resource s = sourceStatement.getSubject();
            Property p = sourceStatement.getPredicate();
            RDFNode o = sourceStatement.getObject();
            if (s.equals(sourceChallenge)) {
                s = targetChallenge;
            }
            if (o.equals(sourceChallenge)) {
                o = targetChallenge;
            }
            if (taskMap.containsKey(s.getURI())) {
                s = taskMap.get(s.getURI());
            }
            if (s.equals(targetChallenge) && p.equals(RDFS.label)) {
                o = source.createLiteral(newLabel);
            }
            if (p.equals(HOBBIT.closed)) {
                o = source.createTypedLiteral(false);
            }
            if (p.equals(HOBBIT.executionDate)) {
                o = source.createTypedLiteral(newExecDate);
            }
            if (p.equals(HOBBIT.publicationDate)) {
                o = source.createTypedLiteral(newPubDate);
            }
            System.out.println(s + " " + p + " " + o);
            target.add(s, p, o);
        }

        LOGGER.info("Saving challenge {}...", targetChallenge);
        storage.sendInsertQuery(target, Constants.CHALLENGE_DEFINITION_GRAPH_URI);
        LOGGER.info("Done.");
    }
}
