package de.usu.research.hobbit.gui.rabbitmq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.hobbit.core.Constants;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rest.BenchmarkBean;
import de.usu.research.hobbit.gui.rest.ChallengeBean;
import de.usu.research.hobbit.gui.rest.ChallengeTaskBean;
import de.usu.research.hobbit.gui.rest.ConfigurationParamBean;
import de.usu.research.hobbit.gui.rest.ConfigurationParamValueBean;
import de.usu.research.hobbit.gui.rest.Datatype;
import de.usu.research.hobbit.gui.rest.SelectOptionBean;

/**
 * Implements simple methods to create beans for the front end based on given
 * RDF Models.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class RdfModelHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdfModelHelper.class);

    /**
     * Creates a {@link BenchmarkBean} from the given RDF model by searching for
     * an instance of {@link HOBBIT#Benchmark}. If such an instance can be
     * found, the label and description of this resource are derived. Otherwise,
     * <code>null</code> is returned.
     *
     * @param model
     *            the RDF model containing the benchmark model
     * @return a {@link BenchmarkBean} or <code>null</code> if there is no
     *         benchmark inside the given model
     */
    public static BenchmarkBean createBenchmarkBean(Model model) {
        // Get the benchmark node
        ResIterator resourceIterator = model.listSubjectsWithProperty(RDF.type, HOBBIT.Benchmark);
        if (!resourceIterator.hasNext()) {
            LOGGER.error("Benchmark model does not have a Benchmark. Returning null.");
            return null;
        }
        Resource benchmarkResource = resourceIterator.next();
        if (resourceIterator.hasNext()) {
            LOGGER.warn(
                    "Benchmark model defines more than one Benchmark. Only the first will be used while all others are ignored.");
        }
        return createBenchmarkBean(model, benchmarkResource);
    }

    /**
     * Creates a {@link BenchmarkBean} from the given RDF model by collecting
     * all benchmark-relevant information found for the given benchmark
     * {@link Resource}.
     *
     * @param model
     *            the RDF model containing the benchmark model
     * @param benchmarkResource
     *            the {@link Resource} representing the benchmark
     * @return a {@link BenchmarkBean} containing the found information
     */
    public static BenchmarkBean createBenchmarkBean(Model model, Resource benchmarkResource) {
        String label = getLabel(model, benchmarkResource);
        if (label == null) {
            label = benchmarkResource.getURI();
            LOGGER.warn("Benchmark {} model does not have a label.", label);
        }
        String description = getDescription(model, benchmarkResource);
        if (description == null) {
            LOGGER.warn("Benchmark {} model does not have a description.", benchmarkResource.getURI());
        }

        BenchmarkBean benchmarkBean = new BenchmarkBean(benchmarkResource.getURI(), label, description);
        parseBenchmarkParameters(model, benchmarkResource, benchmarkBean);
        return benchmarkBean;
    }

    /**
     * Retrieves benchmark parameters of the given benchmark from the given RDF
     * model and adds them to the given {@link BenchmarkBean}.
     *
     * @param model
     *            the RDF model containing the benchmark parameters
     * @param benchmark
     *            the {@link Resource} representing the benchmark
     * @param benchmarkBean
     *            the {@link BenchmarkBean} to which the parameters should be
     *            added
     */
    public static void parseBenchmarkParameters(Model model, Resource benchmark, BenchmarkBean benchmarkBean) {
        NodeIterator nodeIterator = model.listObjectsOfProperty(benchmark, HOBBIT.hasParameter);
        RDFNode node;
        if (nodeIterator.hasNext()) {
            benchmarkBean.configurationParams = new ArrayList<>();
            benchmarkBean.configurationParamNames = new ArrayList<>();
        }
        while (nodeIterator.hasNext()) {
            node = nodeIterator.next();
            if (node.isResource()) {
                parseBenchmarkParameter(model, node.asResource(), benchmarkBean);
            }
        }
    }

    /**
     * Parses the given parameter from the given RDFmodel and adds it to the
     * given {@link BenchmarkBean}.
     *
     * @param model
     *            the RDF model containing the parameter
     * @param parameter
     *            the {@link Resource} representing the parameter
     * @param benchmarkBean
     *            the {@link BenchmarkBean} to which the parameter should be
     *            added
     */
    public static void parseBenchmarkParameter(Model model, Resource parameter, BenchmarkBean benchmarkBean) {
        // If this parameter can be configured
        if (model.contains(parameter, RDF.type, HOBBIT.ConfigurableParameter)) {
            ConfigurationParamBean configParam = new ConfigurationParamBean();
            configParam.id = parameter.getURI();
            configParam.name = getLabel(model, parameter);
            if (configParam.name == null) {
                configParam.name = parameter.getURI();
                LOGGER.warn("The benchmark paremeter {} does not have a label.", parameter.getURI());
            }
            configParam.description = getDescription(model, parameter);
            if (configParam.description == null) {
                LOGGER.warn("The benchmark paremeter {} does not have a description.", parameter.getURI());
            }
            configParam.defaultValue = getStringValue(model, parameter, HOBBIT.defaultValue);
            configParam.isFeature = model.contains(parameter, RDF.type, HOBBIT.FeatureParameter);

            NodeIterator nodeIterator = model.listObjectsOfProperty(parameter, RDFS.range);
            RDFNode node;
            if (nodeIterator.hasNext()) {
                node = nodeIterator.next();
                if (node.isResource()) {
                    Resource typeResource = node.asResource();
                    configParam.range = typeResource.getURI();
                    // If this is an XSD resource
                    if (XSD.getURI().equals(typeResource.getNameSpace())) {
                        configParam.datatype = parseXsdType(typeResource);
                    } else if (model.contains(typeResource, RDF.type, RDFS.Class)
                            || model.contains(typeResource, RDF.type, OWL.Class)) {
                        // Maybe this parameter has a set of predefined enum
                        // values
                        configParam.options = listOptions(model, typeResource);
                    }
                }
            }
            // If the datatype couldn't be found and there is no list of options
            if ((configParam.datatype == null) && (configParam.options == null)) {
                configParam.datatype = Datatype.STRING;
                LOGGER.warn("Couldn't find datatype of parameter {}. Using String as default.", parameter.getURI());

            }
            benchmarkBean.configurationParamNames.add(configParam.name);
            benchmarkBean.configurationParams.add(configParam);
        }
    }

    /**
     * Derives a list of options that are connected to the given parameter
     * resource via owl:oneOf predicates or <code>null</code> if no such
     * resource could be found.
     *
     * @param model
     *            the RDF model containing the options
     * @param parameter
     *            the parameter for which the options are possible values
     * @return a list of options or <code>null</code> if no option could be
     *         found
     */
    public static List<SelectOptionBean> listOptions(Model model, Resource typeResource) {
        NodeIterator nodeIterator = model.listObjectsOfProperty(typeResource, OWL.oneOf);
        RDFNode node;
        Resource option;
        String optionLabel;
        List<SelectOptionBean> options = new ArrayList<>();
        if (nodeIterator.hasNext()) {
            node = nodeIterator.next();
            if (node.isURIResource()) {
                option = node.asResource();
                optionLabel = getLabel(model, option);
                options.add(new SelectOptionBean(optionLabel != null ? optionLabel : option.getURI(), option.getURI()));
            } else if (node.isAnon()) {
                // if this is an array of resources
                RDFNode arrayNode = node;
                NodeIterator tempIter;
                while (!RDF.nil.equals(arrayNode)) {
                    // get the value of this array cell
                    tempIter = model.listObjectsOfProperty(arrayNode.asResource(), RDF.first);
                    if (tempIter.hasNext()) {
                        option = tempIter.next().asResource();
                        optionLabel = getLabel(model, option);
                        options.add(new SelectOptionBean(optionLabel != null ? optionLabel : option.getURI(),
                                option.getURI()));
                    }
                    // move to the next array cell
                    tempIter = model.listObjectsOfProperty(arrayNode.asResource(), RDF.rest);
                    if (tempIter.hasNext()) {
                        arrayNode = tempIter.next();
                    } else {
                        arrayNode = RDF.nil;
                    }
                }
            } else {
                LOGGER.error("Unhandled option RDFNode {}. It will be ignored.", node.toString());
            }
        }
        if (options.size() > 0) {
            return options;
        } else {
            return null;
        }
    }

    /**
     * Returns the {@link Datatype} instance fitting the given XSD datatype or
     * {@link Datatype#STRING} if no valid type can be found.
     *
     *
     * @param typeResource
     *            XSD resource
     * @return the datatype for this resource
     */
    public static Datatype parseXsdType(Resource typeResource) {
        if (XSD.xstring.equals(typeResource)) {
            return Datatype.STRING;
        } else if (XSD.xboolean.equals(typeResource)) {
            return Datatype.BOOLEAN;
        } else if (XSD.decimal.equals(typeResource)) {
            return Datatype.DECIMAL;
        } else if (XSD.xint.equals(typeResource)) {
            return Datatype.INTEGER;
        } else if (XSD.unsignedInt.equals(typeResource)) {
            return Datatype.UNSIGNED_INT;
        } else if (XSD.xdouble.equals(typeResource)) {
            return Datatype.DOUBLE;
        } else if (XSD.xfloat.equals(typeResource)) {
            return Datatype.FLOAT;
        } else {
            LOGGER.warn("Got an unsupported parameter type: {}. It will be handled as String.", typeResource.getURI());
            return Datatype.STRING;
        }
    }

    /**
     * Returns the label of the given {@link Resource} if it is present in the
     * given {@link Model}.
     *
     * @param model
     *            the model that should contain the label
     * @param resource
     *            the resource for which the label is requested
     * @return the label of the resource or <code>null</code> if such a label
     *         does not exist
     */
    public static String getLabel(Model model, Resource resource) {
        return getStringValue(model, resource, RDFS.label);
    }

    /**
     * Returns the description, i.e., the value of rdfs:comment, of the given
     * {@link Resource} if it is present in the given {@link Model}.
     *
     * @param model
     *            the model that should contain the label
     * @param resource
     *            the resource for which the label is requested
     * @return the description of the resource or <code>null</code> if such a
     *         label does not exist
     */
    public static String getDescription(Model model, Resource resource) {
        return getStringValue(model, resource, RDFS.comment);
    }

    /**
     * Returns the object as String of the first triple that has the given
     * subject and predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple
     * @param predicate
     *            the predicate of the triple
     * @return object of the triple as String or <code>null</code> if such a
     *         triple couldn't be found
     */
    public static String getStringValue(Model model, Resource subject, Property predicate) {
        NodeIterator nodeIterator = model.listObjectsOfProperty(subject, predicate);
        if (nodeIterator.hasNext()) {
            RDFNode node = nodeIterator.next();
            if (node.isLiteral()) {
                return node.asLiteral().getString();
            } else {
                return node.toString();
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the first triple literal that has the given subject and predicate
     * and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple
     * @param predicate
     *            the predicate of the triple
     * @return literal of the triple or <code>null</code> if such a literal
     *         couldn't be found
     */
    public static Literal getLiteral(Model model, Resource subject, Property predicate) {
        NodeIterator nodeIterator = model.listObjectsOfProperty(subject, predicate);
        while (nodeIterator.hasNext()) {
            RDFNode node = nodeIterator.next();
            if (node.isLiteral()) {
                return node.asLiteral();
            }
        }
        return null;
    }

    /**
     * Returns the object as {@link Resource} of the first triple that has the
     * given subject and predicate and that can be found in the given model.
     *
     * @param model
     *            the model that should contain the triple
     * @param subject
     *            the subject of the triple
     * @param predicate
     *            the predicate of the triple
     * @return object of the triple as {@link Resource} or <code>null</code> if
     *         such a triple couldn't be found
     */
    public static Resource getObjectResource(Model model, Resource subject, Property predicate) {
        NodeIterator nodeIterator = model.listObjectsOfProperty(subject, predicate);
        while (nodeIterator.hasNext()) {
            RDFNode node = nodeIterator.next();
            if (node.isResource()) {
                return node.asResource();
            }
        }
        return null;
    }

    public static List<ChallengeBean> listChallenges(Model model) {
        List<ChallengeBean> challengeBeans = new ArrayList<>();
        if (model == null) {
            return challengeBeans;
        }
        // iterate over all challenges
        ResIterator challengeIterator = model.listResourcesWithProperty(RDF.type, HOBBIT.Challenge);
        Resource challengeResource;
        while (challengeIterator.hasNext()) {
            challengeResource = challengeIterator.next();
            ChallengeBean challenge = getChallengeBean(model, challengeResource);
            if (challenge != null) {
                challengeBeans.add(challenge);
            }
        }

        return challengeBeans;
    }

    public static ChallengeBean getChallengeBean(Model model, Resource challengeResource) {
        if (model == null) {
            return null;
        }
        ChallengeBean challenge = new ChallengeBean();
        challenge.id = getChallengeId(challengeResource.getURI());
        challenge.name = getLabel(model, challengeResource);
        challenge.description = getDescription(model, challengeResource);
        challenge.organizer = getStringValue(model, challengeResource, HOBBIT.organizer);
        Literal literal = getLiteral(model, challengeResource, HOBBIT.closed);
        if (literal != null) {
            challenge.closed = literal.getBoolean();
        }
        // TODO challenge.visible ?
        // TODO challenge.executionDate ?
        // TODO add publicationDate
        challenge.tasks = listChallengeTasks(model, challengeResource);

        return challenge;
    }

    public static List<ChallengeTaskBean> listChallengeTasks(Model model, Resource challengeResource) {
        List<ChallengeTaskBean> challengeTasks = new ArrayList<>();
        if (model == null) {
            return challengeTasks;
        }
        // iterate over all tasks
        ResIterator taskIterator = model.listResourcesWithProperty(HOBBIT.isTaskOf, challengeResource);
        Resource taskResource;
        while (taskIterator.hasNext()) {
            taskResource = taskIterator.next();
            ChallengeTaskBean task = getChallengeTask(model, taskResource);
            if (task != null) {
                challengeTasks.add(task);
            }
        }

        return challengeTasks;
    }

    public static ChallengeTaskBean getChallengeTask(Model model, Resource taskResource) {
        if (model == null) {
            return null;
        }
        ChallengeTaskBean task = new ChallengeTaskBean();
        task.name = getLabel(model, taskResource);
        task.description = getDescription(model, taskResource);
        Resource benchmarkResource = getObjectResource(model, taskResource, HOBBIT.involvesBenchmark);
        if (benchmarkResource != null) {
            task.benchmark = createBenchmarkBean(model, benchmarkResource);
        }
        task.configurationParams = createParamValueBeans(model, taskResource);
        task.id = getChallengeId(taskResource.getURI());
        return task;
    }

    public static List<ConfigurationParamValueBean> createParamValueBeans(Model model, Resource taskResource) {
        if ((model == null) || (taskResource == null)) {
            return new ArrayList<>(0);
        }
        Map<String, ConfigurationParamValueBean> parameters = new HashMap<String, ConfigurationParamValueBean>();
        createParamValueBeans(model, taskResource,
                model.listResourcesWithProperty(RDF.type, HOBBIT.ConfigurableParameter), parameters);
        createParamValueBeans(model, taskResource, model.listResourcesWithProperty(RDF.type, HOBBIT.Parameter),
                parameters);
        return new ArrayList<>(parameters.values());
    }

    private static void createParamValueBeans(Model model, Resource taskResource, ResIterator parameterIterator,
            Map<String, ConfigurationParamValueBean> parameters) {
        Resource parameter;
        Property paraProp;
        String parameterUri;
        while (parameterIterator.hasNext()) {
            parameter = parameterIterator.next();
            parameterUri = parameter.getURI();
            paraProp = model.getProperty(parameterUri);
            if (model.contains(taskResource, paraProp) && !parameters.containsKey(parameterUri)) {
                ConfigurationParamValueBean paramBean = new ConfigurationParamValueBean();
                paramBean.id = parameterUri;
                paramBean.value = getStringValue(model, taskResource, paraProp);
            }
        }
    }

    public static String getChallengeId(String uri) {
        if (uri.startsWith(Constants.CHALLENGE_URI_NS)) {
            uri = uri.substring(Constants.CHALLENGE_URI_NS.length());
        }
        return uri;
    }

    public static String getChallengeUri(String id) {
        return Constants.CHALLENGE_URI_NS + id;
    }
}
