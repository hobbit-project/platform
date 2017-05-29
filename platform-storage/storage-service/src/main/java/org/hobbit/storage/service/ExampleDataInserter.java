/**
 * This file is part of storage-service.
 *
 * storage-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * storage-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with storage-service.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.storage.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.storage.client.StorageServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start the storage service docker container, connect to the container using
 * <pre>
 * {@code docker exec -it <container-id> bash }
 * </pre>
 * and start this class as
 * simple main with the graph name as first and the file name as second
 * argument.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ExampleDataInserter extends AbstractCommandReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleDataInserter.class);

    private StorageServiceClient storage = null;
    private String graphName = null;
    private String inputFile = null;

    @Override
    public void init() throws Exception {
        super.init();
        storage = StorageServiceClient.create(incomingDataQueueFactory.getConnection());
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // nothing to do
    }

    public static void main(String[] args) throws Exception {
        ExampleDataInserter inserter = new ExampleDataInserter();
        try {
            inserter.init();
            inserter.setGraphName(args[0]);
            inserter.setInputFile(args[1]);
            inserter.run();
        } finally {
            inserter.close();
        }
    }

    @Override
    public void run() throws Exception {
        if ((inputFile == null) || (graphName == null)) {
            LOGGER.error("File or graph name are null. Aborting.");
            return;
        }
        LOGGER.info("Loading {} to insert it to {}.", inputFile, graphName);
        LOGGER.info("Response: " + storage.sendInsertQuery(readModel(inputFile), graphName));
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    private static Model readModel(String benchmarkModelFile) {
        Model model = ModelFactory.createDefaultModel();
        InputStream in = null;
        try {
            in = new FileInputStream(benchmarkModelFile);
            model.read(in, null, "TTL");
        } catch (IOException e) {
            LOGGER.error("Error while reading model.", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return model;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(storage);
        super.close();
    }

}
