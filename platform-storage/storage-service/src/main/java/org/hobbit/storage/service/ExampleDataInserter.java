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
 * <code>docker exec -it <container-id> bash</code> and start this class as
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
        storage = StorageServiceClient.create(connection);
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
