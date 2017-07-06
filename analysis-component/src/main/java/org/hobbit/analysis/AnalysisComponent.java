/**
 * This file is part of analysis-component.
 *
 * analysis-component is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * analysis-component is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with analysis-component.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.analysis;

import java.io.IOException;
import java.util.List;

import org.apache.jena.rdf.model.*;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractComponent;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.QueueingConsumer;

/**
 * This class implements the functionality for the Analysis Component
 * TODO:: !!REFACTOR INTO A MORE GENERIC DESIGN!!
 */
public class AnalysisComponent extends AbstractComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisComponent.class);
    private static final String GRAPH_URI = Constants.PUBLIC_RESULT_GRAPH_URI;
    protected RabbitQueue controller2AnalysisQueue;
//    protected RabbitQueue analysisQueue;
    protected QueueingConsumer consumer;

    private Model experimentModel = null;
    private StorageServiceClient storage;


    @Override
    public void init() throws Exception {
        super.init();
        //initialize the controller_to_analysis queue
        controller2AnalysisQueue =  incomingDataQueueFactory.createDefaultRabbitQueue(Constants.RABBIT_MQ_HOST_NAME_KEY);
//        analysisQueue = createDefaultRabbitQueue(Constants.CONTROLLER_2_ANALYSIS_QUEUE_NAME);
        consumer = new QueueingConsumer(controller2AnalysisQueue.channel);
        controller2AnalysisQueue.channel.basicConsume(controller2AnalysisQueue.name, false, consumer);
        controller2AnalysisQueue.channel.basicConsume(Constants.CONTROLLER_2_ANALYSIS_QUEUE_NAME, false, consumer);
        storage = StorageServiceClient.create(outgoingDataQueuefactory.getConnection());
        LOGGER.debug("Analysis Component Initialized!");
    }

    @Override
    public void run() throws Exception {
        LOGGER.info("Awaiting requests");
        QueueingConsumer.Delivery delivery;
        while (true) {
            delivery = consumer.nextDelivery();
            AnalysisModel analysis;
            Model updatedModel = null;
            if (delivery != null) {
                LOGGER.info("Received a request. Processing...");
                String expUri = RabbitMQUtils.readString(delivery.getBody());
                try{
                    //retrieve data from storage for the specific experiment Uri
                    LOGGER.info("Retrieving Data...");
                    experimentModel = storage.sendConstructQuery(SparqlQueries.getExperimentGraphQuery(expUri, null));
                    //analyse the experiment
                    analysis = analyseExperiment(experimentModel, expUri);
                    //get analysed model
                    updatedModel = analysis.getUpdatedModel();
                    System.out.println(updatedModel);

                } catch (Exception e) {
                    LOGGER.error("Error: " + e.toString());
                }
                if (updatedModel != null) {
                    try {
                        String sparqlUpdateQuery = null;
                        //TODO:: handle null exception for sparql queries
                        sparqlUpdateQuery = SparqlQueries.getUpdateQueryFromDiff(experimentModel,
                                                                                 updatedModel,
                                                                                 GRAPH_URI);
                        LOGGER.info("Updating model...");
                        storage.sendUpdateQuery(sparqlUpdateQuery);
                    } catch (Exception e) {
                        LOGGER.error("Error: " + e.toString());
                    }
                } else {
                    LOGGER.error("No result model from the analysis.");
                }
            }
        }

    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(controller2AnalysisQueue);
//        IOUtils.closeQuietly(analysisQueue);
        IOUtils.closeQuietly(storage);
        super.close();
    }

    /**
     * Creates an Analysis Model for the given experiment.
     *
     * @param experimentModel
     *            The model of the experiment
     * @return Returns the analyzed model
     */
    private AnalysisModel analyseExperiment(Model experimentModel, String expUri){
        AnalysisModel analysisModel = new AnalysisModel(experimentModel, expUri);
        analysisModel.analyse();
        return analysisModel;
    }

    private void notifyQueue(){
        //TODO:: return the status of component

    }

    /**
     * This class implements the functionality of the Analysis Model which
     * processes the experiment models.
     */
    protected static class AnalysisModel {

        //Resources - Properties which will be used for Analysis are categorized
        //in Internal or External. Below we give the URIs for each. This is needed
        //if we ask for triples programmatically (through the .model)
        //TODO:: Write this in a better way/design

        //Internal Resources - Properties
        private static final String FMEASURE = "http://w3id.org/bench#fmeasure";
        private static final String PRECISION = "http://w3id.org/bench#precision";
        private static final String RECALL = "http://w3id.org/bench#recall";
        private static final String PARAMETERS = "http://w3id.org/hobbit/vocab#hasParameter";

        //External Resources - Properties
        private static final String HAS_HARDWARE = "http://w3id.org/hobbit/vocab#hasHardware";
        private static final String MEMORY = "http://w3id.org/hobbit/vocab#hasMemory";
        private static final String RAM = "http://w3id.org/hobbit/vocab#hasRAM";
        private static final String CPU_TYPE = "http://w3id.org/hobbit/vocab#hasCPUTypeCount";

        //Update Properties
        private static final String ABOVE_BASELINE = "http://w3id.org/bench#aboveBaseline";

        private double performanceThreshold = 0.4;
        private double experimentPerformance = 0.0;
        private Model experimentModel = null;
        private String expUri = null;
        private Model updatedModel = null;
        private Boolean aboveBaseline = false;

        // maybe experimentModel is not a good variable name
        protected AnalysisModel(Model experimentModel, String expUri) {
            this.experimentModel = experimentModel;
            this.updatedModel = experimentModel.difference(ModelFactory.createDefaultModel());
            this.expUri = expUri;
        }

        /**
         * Analyses the internal features of an experiment including measures and parameters
         */
        private void analyseInternalFeats(){
            //Ask for triples through the model
            //TODO:: refactor
            Resource expResource = experimentModel.getResource(expUri);
            Property fmeasure = experimentModel.createProperty(FMEASURE);
            Property precision = experimentModel.createProperty(PRECISION);
            Property recall = experimentModel.createProperty(RECALL);

            Float fmeasureValue = expResource.getProperty(fmeasure).getObject().asLiteral().getFloat();
            Float precisionValue = expResource.getProperty(precision).getObject().asLiteral().getFloat();
            Float recallValue = expResource.getProperty(recall).getObject().asLiteral().getFloat();
            this.experimentPerformance = fmeasureValue;

            //Ask for triples with SPARQL query
            //String internalFeatsQuery = "select ?x {?x <"+FMEASURE+"> <"+expUri+">}";
            //QueryExecutionFactory.create(internalFeatsQuery, experimentModel).execSelect();
        }

        /**
         * Analyses the external features of an experiment including cpu,ram etc.
         */
        private void analyseExternalFeats(){
            Resource expHardware = experimentModel.getResource(HAS_HARDWARE);
            Property mem = experimentModel.createProperty(MEMORY);
        }

        private void analyseFeatures(){
            analyseInternalFeats();
            analyseExternalFeats();
        }

        /**
         * Calculates the Spearman Correlation between two lists.
         *
         * @param x
         *            A list of values
         * @param y
         *            A list of values
         * @return Returns the correlation score [-1,1]
         */
        private double calculateCorrelation(List x, List y){
            /* Calculate Spearman Correlation of two lists
             * example: (x,y) = 0.9
             */
            return 0.0;
        }

        /**
         * Compute the performance gap between the current model and others.
         * TODO:: enhance with more functionalities.
         */
        private void computeGapPerformance(){
            // At the moment it justs checks if the score of an experiment is above a given threshold.
            if (this.performanceThreshold < this.experimentPerformance) this.aboveBaseline = true;
        }

        /**
         * Tracks the performance of a model (compared to itself and to others)
         * TODO:: enhance
         */
        private void trackPerformance(){

        }

        /**
         * Updates the experiment model with new properties and values
         * TODO:: enhance
         */
        private void enhanceExperimentModel(){
            //TODO:: refactor into a generic design
            Resource expResource = updatedModel.getResource(expUri);
            Property baseline = updatedModel.createProperty(ABOVE_BASELINE);
            updatedModel.addLiteral(expResource, baseline, this.aboveBaseline);
        }

        public void analyse(){
            analyseFeatures();
            computeGapPerformance();
            enhanceExperimentModel();
        }

        /**
         * Returns the analysis Result.
         * @return Returns a boolean
         */
        public Boolean getAnalysisResult(){
            return this.aboveBaseline;
        }

        /**
         * Returns the resulted model after the analysis.
         * @return Returns the analyzed model
         */
        public Model getUpdatedModel() { return updatedModel;}


    }
}
