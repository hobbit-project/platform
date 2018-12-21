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

import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.sun.org.apache.xpath.internal.operations.Mod;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractComponent;
import org.hobbit.core.data.RabbitQueue;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitExperiments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.QueueingConsumer;

import weka.attributeSelection.*;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LinearRegression;
import weka.clusterers.SimpleKMeans;
import weka.core.*;


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
    protected StorageServiceClient storage;


    @Override
    public void init() throws Exception {
        super.init();
        //initialize the controller_to_analysis queue
        controller2AnalysisQueue =  incomingDataQueueFactory.createDefaultRabbitQueue(Constants.CONTROLLER_2_ANALYSIS_QUEUE_NAME);
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
            if (delivery != null) {
                LOGGER.info("Received a request. Processing...");
                String expUri = RabbitMQUtils.readString(delivery.getBody());
                handleRequest(expUri);
            }
        }

    }

    protected void handleRequest(String expUri) {
        Model updatedModel = null;
        try{
            LinkedHashMap<String, Map<String, Map<String, Float>>> mappings = null;
            String benchmarkUri = null;
            String systemUri = null;
            String expURI = "";
            //retrieve data from storage for the specific experiment Uri
            LOGGER.info("Retrieving Data...");
            LOGGER.info(SparqlQueries.getExperimentGraphQuery(expUri, null));
            experimentModel = storage.sendConstructQuery(SparqlQueries.getExperimentGraphQuery(expUri, null));

            Instances predictionDataset = null;
            Instances clusterDataset = null;
            Instances igDataset = null;
            Instances currentData = null;
            DataProcessor dp = new DataProcessor();
            DataProcessor dpForCurrent = new DataProcessor();

            NodeIterator benchmarkUris = experimentModel.listObjectsOfProperty(HOBBIT.involvesBenchmark);
            if (benchmarkUris.hasNext()) {
                benchmarkUri = benchmarkUris.next().toString();
                LOGGER.debug("Benchmark: {}", benchmarkUri);
            } else {
                LOGGER.error("Did not get URI of the benchmark");
            }

            Property parametersURI = HOBBIT.involvesSystemInstance;
            // select all resources that are of type
            NodeIterator systemUris = experimentModel.listObjectsOfProperty(parametersURI);
            if (systemUris.hasNext()) {
                systemUri = systemUris.next().toString();
                LOGGER.debug("System: {}", systemUri);
            } else {
                LOGGER.error("Did not get URI of the system");
            }

            if (benchmarkUri != null && systemUri != null) {
                LOGGER.info("Retrieving Experiments Data from storage...");
                QueryFormatter qf = new QueryFormatter(this.storage);
                Model paramsModel = qf.getParametersOfAllSystemExps(benchmarkUri, systemUri);
                Model kpisModel = qf.getAllKpisOfAllSystemExps(benchmarkUri, systemUri);

                if (!paramsModel.isEmpty() && !kpisModel.isEmpty()) {
                    LOGGER.info("Preprocessing data - Converting to datasets...");
                    List<Model> models = Arrays.asList(paramsModel, kpisModel);
                    dp.getParametersFromRdfModel(models);

                    igDataset = dp.getInstancesDatasetForIG();

                    clusterDataset = dp.getInstancesDatasetForClustering();

                    predictionDataset = dp.getInstancesDatasetForPrediction();

                    mappings = dp.getMappings();

                    ResIterator expURIs = experimentModel.listSubjectsWithProperty(parametersURI);
                    List expUris = expURIs.toList();
                    expURI = expUris.get(0).toString();
                    LinkedHashMap<String, Map<String, Map<String, Float>>> current = new LinkedHashMap<>();
                    current.put(expURI, mappings.get(expURI));
                    currentData = dp.buildDatasetFromMappingsForCurrent(current);

                }
                else{
                    LOGGER.error("Did not find any models available!");
                }
            }
            else{
                LOGGER.error("Wrong format of RDF. Cannot find system URI. Setting to default and aborting...");
                systemUri = "None";
            }

            if (clusterDataset==null && predictionDataset==null){
                LOGGER.info("No data to analyze! Aborting analysis...");
            }
            else{
                AnalysisModel model = new AnalysisModel(clusterDataset, igDataset, predictionDataset, experimentModel, expURI);

                try{
                    LOGGER.info("Starting analysis on retrieved data...");

                    LOGGER.info("Calculating clusters...");
                    model.computeClustersOfSystems();

                    LOGGER.info("Assigning cluster to current experiment...");
                    model.assignClusterToInstance(currentData.get(0));

                    LOGGER.info("Calculating importance of parameters/features...");
                    model.computeImportanceOfFeatures(mappings);

                    LOGGER.info("Calculating prediction model...");
                    model.predictPerformanceOfSystem();

                    LOGGER.info("Analysis performed successfully!");

                    LOGGER.info("Enhancing experiment model with results...");
                    model.enhanceExperimentModel();
                    updatedModel = model.getUpdatedModel();
                }
                catch (Exception e){
                    LOGGER.error("Error in analyzing data. Have to abort analysis...", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in pre-processing. ", e);
        }
        if (updatedModel != null) {
            try {
                LOGGER.info("Updating model...");
                String sparqlUpdateQuery = null;
                //TODO:: handle null exception for sparql queries
                sparqlUpdateQuery = SparqlQueries.getUpdateQueryFromDiff(experimentModel,
                        updatedModel,
                        GRAPH_URI);
                LOGGER.info("Sending the enhanced model to storage...");
                storage.sendUpdateQuery(sparqlUpdateQuery);
            } catch (Exception e) {
                LOGGER.error("Error when updating model.", e);
            }
        } else {
            LOGGER.error("Model did not update properly! No result model from the analysis.");
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(controller2AnalysisQueue);
        //IOUtils.closeQuietly(analysisQueue);
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

    /**
     private AnalysisModel analyseExperiment(Model experimentModel, String expUri){
     AnalysisModel analysisModel = new AnalysisModel(experimentModel, expUri);
     analysisModel.analyse();
     return analysisModel;
     }*/

    private void notifyQueue(){
        //TODO:: return the status of component

    }

    /**
     * This class implements the functionality of the Analysis Model which
     * processes the experiment models.
     */
    protected static class AnalysisModel {

        //Update Properties
        // FIXME: "http://w3id.org/bench#" should not be used, but already is in data
        // FIXME: move property URIs to the HOBBIT core library
        private static final String BELONGS_TO_CLUSTER = "http://w3id.org/bench#belongsToCluster";
        private static final String MODEL_PREDICTION = "http://w3id.org/bench#modelPrediction";
        private static final String IMPORTANT_FEATURES = "http://w3id.org/bench#importantFeatures";

        private String belongsToCluster = "none";
        private double modelPrediction = 0;
        private String importantFeatures = "no result";

        // define a cluster model for the analysis
        private SimpleKMeans clusterModel = new SimpleKMeans();
        private ArrayList<Integer> clustersSorted = new ArrayList<>();
        private String[] clusterLabels = {"high", "medium", "low"};

        private Instances clusterDataset = null;
        private Instances igDataset = null;
        private Instances predictionDataset = null;
        private Model experimentModel = null;
        private String expUri = null;
        private Model updatedModel = null;
        private Boolean aboveBaseline = false;

        protected AnalysisModel(Instances clusterDataset, Instances igDataset, Instances predictionDataset, Model experimentModel, String expUri) {
            this.clusterDataset = clusterDataset;
            this.igDataset = igDataset;
            this.predictionDataset = predictionDataset;
            this.experimentModel = experimentModel;
            this.updatedModel = experimentModel.difference(ModelFactory.createDefaultModel());
            this.expUri = expUri;
        }

        /**
         * Updates the experiment model with new properties and values
         */
        private void enhanceExperimentModel(){
            //TODO:: refactor into a generic design
            Resource expResource = updatedModel.getResource(expUri);

            //add properties to the model
            Property cluster = updatedModel.createProperty(BELONGS_TO_CLUSTER);
            Property importantFeatures = updatedModel.createProperty(IMPORTANT_FEATURES);
            Property prediction = updatedModel.createProperty(MODEL_PREDICTION); // if chosen randomly make sure we know the name of the literal/prediction
            updatedModel.addLiteral(expResource, cluster, this.belongsToCluster);
            updatedModel.addLiteral(expResource, importantFeatures, this.importantFeatures);
            updatedModel.addLiteral(expResource, prediction, this.modelPrediction);
        }

        /**
         * Returns the resulted model after the analysis.
         * @return Returns the analyzed model
         */
        public Model getUpdatedModel() { return updatedModel;}


        /**
         * Assign instance to a specific cluster based on the created cluster model
         */
        private int assignClusterToInstance(Instance instance){
            int clusterNumber = 100;
            String clusterLabel = "low";
            try {
                clusterNumber = this.clusterModel.clusterInstance(instance);
                clusterLabel = clusterLabels[clustersSorted.indexOf(clusterNumber)];
                this.belongsToCluster = clusterLabel;
            } catch (Exception e) {
                LOGGER.error("Error while assigning cluster to instance.", e);
            }
            return clusterNumber;
        }


        /**
         * Computes clusters based on the performance of the benchmarks
         */
        private void computeClustersOfSystems() {
            try {
                this.clusterModel.setPreserveInstancesOrder(true);
                this.clusterModel.setNumClusters(3);
                this.clusterModel.setDisplayStdDevs(true);
            } catch (Exception e) {
                LOGGER.error("Error while setting cluster model.", e);
            }
            //set distance function
            //model.setDistanceFunction(new weka.core.ManhattanDistance());
            //build the clusterer
            try {
                this.clusterModel.buildClusterer(this.clusterDataset);
                int[] assignments = this.clusterModel.getAssignments();

                Instances standDevs = this.clusterModel.getClusterStandardDevs();

                ArrayList<Double> scoresCluster = new ArrayList();

                for (Instance clusterssNum: standDevs) {
                    scoresCluster.add(sum(clusterssNum.toDoubleArray()));
                }
                ArrayList<Double> notSortedScoresCluster = new ArrayList(scoresCluster);
                Collections.sort(scoresCluster, Collections.reverseOrder());

                ArrayList<Integer> sortedClusters = new ArrayList();
                for (double score : scoresCluster) {
                    sortedClusters.add(notSortedScoresCluster.indexOf(score));
                }
                clustersSorted = sortedClusters;
            }
            catch (Exception e) {
                LOGGER.error("Error while building clusters.", e);
            }

        }

        private static double sum(double...values) {
            double result = 0;
            for (double value:values)
                result += value;
            return result;
        }

        /**
         * Computes information gain in order to decide which features are more important
         */
        private void computeImportanceOfFeatures(LinkedHashMap<String, Map<String, Map<String, Float>>> mappings){

            AttributeSelection attsel = new AttributeSelection();  // package weka.attributeSelection!
            CfsSubsetEval eval = new CfsSubsetEval();
            GreedyStepwise search = new GreedyStepwise();
            search.setSearchBackwards(true);
            attsel.setEvaluator(eval);
            attsel.setSearch(search);
            ArrayList<String> importantFeaturesNames = new ArrayList<>();

            Object[] keys = mappings.keySet().toArray();

            try {
                attsel.SelectAttributes(this.igDataset);
                int[] indices = attsel.selectedAttributes();
                for (int f : indices){
                    importantFeaturesNames.add((String)mappings.get(keys[0]).get("params").keySet().toArray()[f]);
                }
                String namesString = "";
                for (String s : importantFeaturesNames)
                {
                    namesString += s + ", ";
                }
                this.importantFeatures = namesString;
            }
            catch (Exception e) {
                LOGGER.error("Error during feature analysis.", e);
            }

        }

        /**
         * Predicts the performance of a system based on the features/parameters assigned
         */
        private void predictPerformanceOfSystem(){

            LinearRegression regressionModel = new LinearRegression();
            try {
                //should set an attribute as class indicator
                this.predictionDataset.setClassIndex(this.predictionDataset.numAttributes() - 1);
                //train a model
                regressionModel.buildClassifier(this.predictionDataset);
                //test an instance
                this.modelPrediction = regressionModel.classifyInstance(this.predictionDataset.get(0));
            } catch (Exception e) {
                LOGGER.error("Error while predicting performance.", e);
            }


        }

        /**
         * Predicts the performance of a system based on the features/parameters assigned
         */
        private void predictParticipationOfSystem(){

        }

    }

    public static class KpiAttribute extends Attribute {
        public KpiAttribute(String attributeName) {
            super(attributeName);
        }
    }

    public static class ParamAttribute extends Attribute {
        public ParamAttribute(String attributeName) {
            super(attributeName);
        }
    }

    /**
     * Perform a series of statistics on a RDF dataset in order to feed them to
     * to a prediction model. Scope of this procedure is to make a prediction based on
     * the features/statisics of the dataset (f.i. if a benchmark will perform good or bad).
     */
    protected static class DatasetAnalytics {

        protected DatasetAnalytics() {

        }
    }


    /**
     * Perform a series of processing to handle Model and transform it to instances datasets
     */
    protected static class DataProcessor {

        protected LinkedHashMap<String, Map<String, Map<String, Float>>> outer = new LinkedHashMap<String, Map<String, Map<String, Float>>>();
        protected ArrayList<String> parametersNames;

        protected DataProcessor() {
        }

        /**
         *
         */
        protected void getParametersFromRdfModel(List<Model> models) {

            for (int i = 0; i<models.size(); i++){
                Model model = models.get(i);
                parametersNames = new ArrayList<>();
                ArrayList<ArrayList> allExpParameters = new ArrayList<>();
                Property typeURI = RDF.type;
                Resource parametersURI = HOBBIT.Experiment;
                // select all resources that are of type
                ResIterator blockIt = model.listResourcesWithProperty(typeURI, parametersURI);
                while (blockIt.hasNext()) {
                    ArrayList parameters = new ArrayList();
                    Resource currentParameter = blockIt.next();
                    //build the inner map for data
                    Map<String, Map<String, Float>> inner = outer.get(currentParameter.toString());
                    LinkedHashMap<String, Float> valueSet = new LinkedHashMap<String, Float>();
                    if(inner == null){
                        inner = new HashMap<String, Map<String, Float>>();
                        if (i==0){
                            inner.put("params", new HashMap<String, Float>());
                        }
                        else{
                            inner.put("kpis", new HashMap<String, Float>());
                        }
                        outer.put(currentParameter.toString(), inner);
                    }
                    StmtIterator it = model.listStatements(currentParameter, null, (RDFNode) null);
                    while (it.hasNext()) {
                        // all triples of current Parameter
                        Statement nextLiteral = it.next();
                        RDFNode objectSt = nextLiteral.getObject();
                        if (objectSt.isLiteral()) {
                            RDFDatatype literalType = nextLiteral.getLiteral().getDatatype();
                            if (!literalType.equals(XSDDatatype.XSDstring) && !literalType.equals(XSDDatatype.XSDdateTime)
                                    && (!literalType.toString().contains("tring"))) {
                                float literalValue = nextLiteral.getLiteral().getFloat();
                                String literalName = nextLiteral.getPredicate().toString();
                                parameters.add(literalValue);
                                //add parameter to map
                                if (i==0){
                                    valueSet.put(literalName, literalValue);
                                    inner.put("params", valueSet);
                                }
                                else{
                                    valueSet.put(literalName, literalValue);
                                    inner.put("kpis", valueSet);
                                }

                                //check if parameters name are properly added
                                //(case where different number of parameters exist in different benchmarks)
                                if (!parametersNames.contains(literalName)){
                                    parametersNames.add(literalName);
                                }
                            }
                        }
                    }
                    allExpParameters.add(parameters);
                }
            }
        }

        /**
         *
         */
        private  Instances buildDatasetFromMappingsForCurrent(LinkedHashMap<String, Map<String, Map<String, Float>>> map){
            ArrayList<Attribute> atts = new ArrayList<Attribute>();
            List<Instance> instances = new ArrayList<Instance>();
            int numInstances = map.size();
            int numAtts = 0;

            for (String key : map.keySet()) {
                int newSize = (map.get(key).get("kpis")).size();
                if ( newSize > numAtts){
                    numAtts = newSize;
                }
            }

            Object[] keys = map.keySet().toArray();

            for(int obj = 0; obj < numAtts; obj++)
            {
                Attribute current = new Attribute("Attribute" + obj, obj);
                atts.add(current);
            }

            for(int obj = 0; obj < numInstances; obj++)
            {
                instances.add(new SparseInstance(numAtts));
            }

            for(int obj = 0; obj < numInstances; obj++)
            {
                for(int dim = 0; dim < numAtts; dim++ ){

                    // check if there are missing parameters from instances and fill them
                    if (dim < (map.get(keys[obj]).get("kpis")).size()) {
                        instances.get(obj).setValue(atts.get(dim), (float)(map.get(keys[obj]).get("kpis").
                                values().toArray()[dim]));
                    }
                    else {
                        instances.get(obj).setValue(atts.get(dim), 0);
                    }
                }
            }

            Instances newDataset = new Instances("Dataset", atts, instances.size());

            for(Instance inst : instances)
                newDataset.add(inst);

            return newDataset;
        }


        private  Instances buildDatasetFromMappingsForClustering(){
            ArrayList<Attribute> atts = new ArrayList<Attribute>();
            List<Instance> instances = new ArrayList<Instance>();

            int numInstances = outer.size();

            int numAtts = 0;

            for (String key : outer.keySet()) {
                int newSize = (outer.get(key).get("kpis")).size();
                if ( newSize > numAtts){
                    numAtts = newSize;
                }
            }

            Object[] keys = outer.keySet().toArray();

            for(int obj = 0; obj < numAtts; obj++)
            {
                Attribute current = new Attribute("Attribute" + obj, obj);
                atts.add(current);
            }

            for(int obj = 0; obj < numInstances; obj++)
            {
                instances.add(new SparseInstance(numAtts));
            }

            for(int obj = 0; obj < numInstances; obj++)
            {
                for(int dim = 0; dim < numAtts; dim++ ){

                    // check if there are missing parameters from instances and fill them
                    if (dim < (outer.get(keys[obj]).get("kpis")).size()) {
                        instances.get(obj).setValue(atts.get(dim), (float)(outer.get(keys[obj]).get("kpis").values().toArray()[dim]));
                    }
                    else {
                        instances.get(obj).setValue(atts.get(dim), 0);
                    }
                }

            }

            Instances newDataset = new Instances("Dataset", atts, instances.size());

            for(Instance inst : instances)
                newDataset.add(inst);

            return newDataset;
        }


        private  Instances buildDatasetFromMappingsForIG(){
            ArrayList<Attribute> atts = new ArrayList<Attribute>();
            List<Instance> instances = new ArrayList<Instance>();

            int numInstances = outer.size();
            int numAtts = 0;

            for (String key : outer.keySet()) {
                int newSize = (outer.get(key).get("params")).size();
                if ( newSize > numAtts){
                    numAtts = newSize;
                }
            }

            Object[] keys = outer.keySet().toArray();

            for(int obj = 0; obj < numAtts; obj++)
            {
                Attribute current = new Attribute("Attribute" + obj, obj);
                atts.add(current);
            }

            for(int obj = 0; obj < numInstances; obj++)
            {
                instances.add(new SparseInstance(numAtts));
            }

            for(int obj = 0; obj < numInstances; obj++)
            {
                for(int dim = 0; dim < numAtts; dim++ ){

                    // check if there are missing parameters from instances and fill them
                    if (dim < (outer.get(keys[obj]).get("params")).size()) {
                        instances.get(obj).setValue(atts.get(dim), (float)(outer.get(keys[obj]).get("params").values().toArray()[dim]));
                    }
                    else {
                        instances.get(obj).setValue(atts.get(dim), 0);
                    }
                }

            }

            Instances newDataset = new Instances("Dataset", atts, instances.size());

            for(Instance inst : instances)
                newDataset.add(inst);

            return newDataset;
        }


        /**
         *
         */
        private  Instances buildDatasetFromMappingsForPrediction(){
            ArrayList<Attribute> atts = new ArrayList<Attribute>();
            List<Instance> instances = new ArrayList<Instance>();

            int numInstances = outer.size();
            int numAtts = 0;

            for (String key : outer.keySet()) {
                int newSize = (outer.get(key).get("params")).size();
                if ( newSize > numAtts){
                    numAtts = newSize;
                }
            }
            int minNumKpis = 1000;

            for (String key : outer.keySet()) {
                int newSize = (outer.get(key).get("kpis")).size();
                if ( newSize < minNumKpis){
                    minNumKpis = newSize;
                }
            }

            //add one more to be the prediction value retrieved from kpis!
            numAtts +=1;

            Object[] keys = outer.keySet().toArray();

            for(int obj = 0; obj < numAtts; obj++)
            {
                Attribute current = new Attribute("Attribute" + obj, obj);
                atts.add(current);
            }

            for(int obj = 0; obj < numInstances; obj++)
            {
                instances.add(new SparseInstance(numAtts));
            }

            for(int obj = 0; obj < numInstances; obj++)
            {
                for(int dim = 0; dim < numAtts; dim++ ){

                    // check if there are missing parameters from instances and fill them
                    if (dim < (outer.get(keys[obj]).get("params")).size()) {
                        instances.get(obj).setValue(atts.get(dim), (float)(outer.get(keys[obj]).get("params").values().toArray()[dim]));
                    }
                    else if (dim == numAtts-1){ //add a kpi as a class prediction
                        instances.get(obj).setValue(atts.get(dim), (float)(outer.get(keys[obj]).get("kpis").values().toArray()[minNumKpis-1]));
                    }
                    else{
                        instances.get(obj).setValue(atts.get(dim), 0);
                    }

                }

            }

            Instances newDataset = new Instances("Dataset", atts, instances.size());

            for(Instance inst : instances)
                newDataset.add(inst);

            return newDataset;
        }

        private Instances buildDatasetFromMappingsForCorrelation(){
            LOGGER.debug("Building dataset for correlation...");
            Set<String> kpis = outer.values().stream()
                    .flatMap(map -> map.get("kpis").keySet().stream())
                    .collect(Collectors.toCollection(TreeSet::new));
            LOGGER.debug("KPIs:\n{}", kpis.stream()
                    .map(str -> "- " + str).collect(Collectors.joining("\n")));

            Set<String> params = outer.values().stream()
                    .flatMap(map -> map.get("params").keySet().stream())
                    .collect(Collectors.toCollection(TreeSet::new));
            LOGGER.debug("Params:\n{}", params.stream()
                    .map(str -> "- " + str).collect(Collectors.joining("\n")));

            ArrayList<Attribute> attInfo = Stream.concat(
                        kpis.stream().map(KpiAttribute::new),
                        params.stream().map(ParamAttribute::new))
                    .collect(Collectors.toCollection(ArrayList::new));

            List<Instance> instances = outer.values().stream()
                .map(instance -> new DenseInstance(
                    1,
                    attInfo.stream()
                        .map(
                            att -> Optional.ofNullable(instance.get("params").get(att.name()))
                            .orElseGet(() -> instance.get("kpis").get(att.name()))
                        )
                        .mapToDouble(value -> Optional.ofNullable(value).orElse(0f))
                        .toArray()))
                .collect(Collectors.toList());

            Instances dataset = new Instances("Correlation dataset", attInfo, instances.size());
            for (Instance i : instances) {
                dataset.add(i);
            }
            LOGGER.debug("Dataset for correlation: {}", dataset);
            return dataset;
        }

        /**
         *
         */
        private Instances getInstancesDatasetForClustering(){
            if (outer.size() > 0){
                Instances instancesDataset = this.buildDatasetFromMappingsForClustering();
                return instancesDataset;
            }
            else{
                LOGGER.error("No results found for the system. Aborting...");
                return null;
            }
        }
        private Instances getInstancesDatasetForPrediction(){
            if (outer.size() > 0){
                Instances instancesDataset = this.buildDatasetFromMappingsForPrediction();
                return instancesDataset;
            }
            else{
                LOGGER.error("No results found for the system. Aborting...");
                return null;
            }
        }

        private Instances getInstancesDatasetForIG(){
            if (outer.size() > 0){
                Instances instancesDataset = this.buildDatasetFromMappingsForIG();
                return instancesDataset;
            }
            else{
                LOGGER.error("No results found for the system. Aborting...");
                return null;
            }
        }

        protected Instances getInstancesDatasetForCorrelation() {
            if (outer.size() > 0){
                Instances instancesDataset = this.buildDatasetFromMappingsForCorrelation();
                return instancesDataset;
            } else {
                LOGGER.error("No results found for the system. Aborting...");
                return null;
            }
        }

        protected LinkedHashMap<String, Map<String, Map<String, Float>>> getMappings(){
            return this.outer;
        }

    }

    /**
     * A class to hold all necessary sparql queries for analysis component.
     * FIXME: move queries to the HOBBIT core library.
     */
    protected static class QueryFormatter {
        private StorageServiceClient storage;

        private String queryForParametersOfAllSystemExps = "prefix hobbit: <" + HobbitExperiments.getURI() + ">\n" +
                "prefix ns: <" + HOBBIT.getURI() + ">\n" +
                "prefix xsd: <" + XSD.getURI() + ">\n" +
                "construct {?aa a ns:Experiment . ?aa ?param ?o}  where {?aa a ns:Experiment .\n" +
                "?aa ns:involvesBenchmark %1$s .\n" +
                "?aa ns:involvesSystemInstance %2$s .\n" +
                "minus {?aa ns:terminatedWithError ?err} .\n" +
                "?aa ns:involvesBenchmark ?ben .\n" +
                "?ben ns:hasParameter ?param .\n" +
                "?aa ?param ?o . filter (datatype(?o) != xsd:string && datatype(?o) != xsd:boolean)}";
        private String queryForKPIsOfAllSystemExps = "prefix hobbit: <" + HobbitExperiments.getURI() + ">\n" +
                "prefix ns: <" + HOBBIT.getURI() + ">\n" +
                "prefix xsd: <" + XSD.getURI() + ">\n" +
                "construct {?aa a ns:Experiment . ?aa ?kpi ?o}  where {?aa a ns:Experiment .\n" +
                "?aa ns:involvesBenchmark %1$s .\n" +
                "?aa ns:involvesSystemInstance %2$s .\n" +
                "minus {?aa ns:terminatedWithError ?err} .\n" +
                "?aa ns:involvesBenchmark ?ben .\n" +
                "?ben ns:measuresKPI ?kpi .\n" +
                "?aa ?kpi ?o . filter (datatype(?o) != xsd:string && datatype(?o) != xsd:boolean) }";
        private String queryForParamsAndKPIsOfSystemExps = "prefix hobbit: <" + HobbitExperiments.getURI() + ">\n" +
                "prefix ns: <" + HOBBIT.getURI() + ">\n" +
                "prefix xsd: <" + XSD.getURI() + ">\n" +
                "construct {?aa a ns:Experiment . ?aa ?kpi ?o}  where {?aa a ns:Experiment .\n" +
                "?aa ns:involvesBenchmark %1$s .\n" +
                "?aa ns:involvesSystemInstance %2$s .\n" +
                "minus {?aa ns:terminatedWithError ?err} .\n" +
                "?aa ns:involvesBenchmark ?ben .\n" +
                "?ben ns:measuresKPI|ns:hasParameter ?kpi .\n" +
                "?aa ?kpi ?o . filter (datatype(?o) != xsd:string && datatype(?o) != xsd:boolean) }";
        private String queryForAllSystemExps =
                "prefix hobbit: <" + HOBBIT.getURI() + ">\n" +
                "construct {?a ?a ?a} where {?a a hobbit:Experiment ;\n" +
                "hobbit:involvesBenchmark %1$s ;\n" +
                "hobbit:involvesSystemInstance %2$s . }";

        /*
        private String queryBioasq = "prefix ns: <http://bioasq.org/onto_counts.owl#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "construct {?aa a owl:NamedIndividual . ?aa ?param ?o} where {?aa a owl:NamedIndividual .\n" +
                "?aa ?param ?o . filter (datatype(?o) != xsd:string)}";
                */

        //FOR TESTING
        protected QueryFormatter(StorageServiceClient storage){
            this.storage = storage;

        }

        //FOR PRODUCTION:
        //protected QueryFormatter(StorageServiceClient storage){
        //    this.storage = storage;

        //}


        protected Model sendSparqlQueryToStorage(String query){
            //LOGGER.info("Retrieving all experiment runs of system...: " + query);
            Model queryResultModel = null;
            try{
                queryResultModel = this.storage.sendConstructQuery(query);
            }
            catch (Exception e){
                LOGGER.error("Error when sending sparql query to storage.", e);
            }

            return queryResultModel;
        }

        protected Model getParametersOfAllSystemExps(String benchmarkUri, String systemUri){

            return sendSparqlQueryToStorage(String.format(queryForParametersOfAllSystemExps, "<" + benchmarkUri + ">", "<" + systemUri + ">"));
        }

        protected Model getAllExperimentsOfSystem(String benchmarkUri, String systemUri){
            return sendSparqlQueryToStorage(String.format(queryForAllSystemExps, "<" + benchmarkUri + ">", "<" + systemUri + ">"));
        }

        protected Model getAllKpisOfAllSystemExps(String benchmarkUri, String systemUri){
            return sendSparqlQueryToStorage(String.format(queryForKPIsOfAllSystemExps, "<" + benchmarkUri + ">", "<" + systemUri + ">"));
        }

        protected  Model getParamsAndKPIsOfAllSystemExps(String benchmarkUri, String systemUri){
            return sendSparqlQueryToStorage(String.format(queryForParamsAndKPIsOfSystemExps, "<" + benchmarkUri + ">", "<" + systemUri + ">"));

        }

    }

}
