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
package de.usu.research.hobbit.gui.rabbitmq;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.SeqImpl;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.hobbit.core.Constants;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.DataCube;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitErrors;
import org.hobbit.vocab.HobbitExperiments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rest.Datatype;
import de.usu.research.hobbit.gui.rest.beans.BenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeTaskBean;
import de.usu.research.hobbit.gui.rest.beans.ConfigurationParamBean;
import de.usu.research.hobbit.gui.rest.beans.ConfigurationParamValueBean;
import de.usu.research.hobbit.gui.rest.beans.ConfiguredBenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.DiagramBean;
import de.usu.research.hobbit.gui.rest.beans.DiagramBean.Point;
import de.usu.research.hobbit.gui.rest.beans.ExperimentBean;
import de.usu.research.hobbit.gui.rest.beans.KeyPerformanceIndicatorBean;
import de.usu.research.hobbit.gui.rest.beans.SelectOptionBean;
import de.usu.research.hobbit.gui.rest.beans.SystemBean;
import de.usu.research.hobbit.gui.rest.beans.TaskRegistrationBean;

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
     * Creates a {@link BenchmarkBean} from the given RDF model by searching for an
     * instance of {@link HOBBIT#Benchmark}. If such an instance can be found, the
     * label and description of this resource are derived. Otherwise,
     * <code>null</code> is returned.
     *
     * @param model
     *            the RDF model containing the benchmark model
     * @return a {@link BenchmarkBean} or <code>null</code> if there is no benchmark
     *         inside the given model
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

    public static BenchmarkBean createBenchmarkBean(Model model, Resource benchmarkResource) {
        BenchmarkBean bean = new BenchmarkBean();
        return createBenchmarkBean(model, benchmarkResource, bean);
    }

    public static ConfiguredBenchmarkBean createConfiguredBenchmarkBean(Model model, Resource benchmarkResource,
            Resource experimentResource) {
        ConfiguredBenchmarkBean bean = new ConfiguredBenchmarkBean();
        createBenchmarkBean(model, benchmarkResource, bean);

        // fill ConfigurationParamValues
        Map<String, ConfigurationParamValueBean> configuredParams = new HashMap<String, ConfigurationParamValueBean>();
        createParamValueBeans(model, experimentResource,
                model.listResourcesWithProperty(RDF.type, HOBBIT.ConfigurableParameter), configuredParams);
        createParamValueBeans(model, experimentResource, model.listResourcesWithProperty(RDF.type, HOBBIT.Parameter),
                configuredParams);
        bean.setConfigurationParamValues(new ArrayList<>(configuredParams.values()));

        return bean;
    }

    /**
     * Creates a {@link BenchmarkBean} from the given RDF model by collecting all
     * benchmark-relevant information found for the given benchmark
     * {@link Resource}.
     *
     * @param model
     *            the RDF model containing the benchmark model
     * @param benchmarkResource
     *            the {@link Resource} representing the benchmark
     * @return a {@link BenchmarkBean} containing the found information
     */
    public static <T extends BenchmarkBean> T createBenchmarkBean(Model model, Resource benchmarkResource, T bean) {
        String label = RdfHelper.getLabel(model, benchmarkResource);
        if (label == null) {
            label = benchmarkResource.getURI();
            LOGGER.info("Benchmark {} model does not have a label.", label);
        }
        String description = RdfHelper.getDescription(model, benchmarkResource);
        if (description == null) {
            LOGGER.info("Benchmark {} model does not have a description.", benchmarkResource.getURI());
        }

        bean.setId(benchmarkResource.getURI());
        bean.setName(label);
        bean.setDescription(description);
        parseBenchmarkParameters(model, benchmarkResource, bean);

        Map<String, KeyPerformanceIndicatorBean> kpiMap = new HashMap<>();
        createKPIBeans(model, benchmarkResource, model.listResourcesWithProperty(RDF.type, HOBBIT.KPI), kpiMap);
        bean.setKpis(new ArrayList<>(kpiMap.values()));
        return bean;
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
     *            the {@link BenchmarkBean} to which the parameters should be added
     */
    public static void parseBenchmarkParameters(Model model, Resource benchmark, BenchmarkBean benchmarkBean) {
        NodeIterator nodeIterator = model.listObjectsOfProperty(benchmark, HOBBIT.hasParameter);
        RDFNode node;
        if (nodeIterator.hasNext()) {
            benchmarkBean.setConfigurationParams(new ArrayList<>());
            benchmarkBean.setConfigurationParamNames(new ArrayList<>());
        }
        while (nodeIterator.hasNext()) {
            node = nodeIterator.next();
            if (node.isResource()) {
                parseBenchmarkParameter(model, node.asResource(), benchmarkBean);
            }
        }
    }

    /**
     * Parses the given parameter from the given RDFmodel and adds it to the given
     * {@link BenchmarkBean}.
     *
     * @param model
     *            the RDF model containing the parameter
     * @param parameter
     *            the {@link Resource} representing the parameter
     * @param benchmarkBean
     *            the {@link BenchmarkBean} to which the parameter should be added
     */
    public static void parseBenchmarkParameter(Model model, Resource parameter, BenchmarkBean benchmarkBean) {
        // If this parameter can be configured
        if (model.contains(parameter, RDF.type, HOBBIT.ConfigurableParameter)) {
            ConfigurationParamBean configParam = new ConfigurationParamBean();
            configParam.setId(parameter.getURI());
            configParam.setName(RdfHelper.getLabel(model, parameter));
            if (configParam.getName() == null) {
                configParam.setName(parameter.getURI());
                LOGGER.warn("The benchmark parameter {} does not have a label.", parameter.getURI());
            }
            configParam.setDescription(RdfHelper.getDescription(model, parameter));
            if (configParam.getDescription() == null) {
                LOGGER.warn("The benchmark parameter {} does not have a description.", parameter.getURI());
            }
            configParam.setDefaultValue(RdfHelper.getStringValue(model, parameter, HOBBIT.defaultValue));
            configParam.setFeature(model.contains(parameter, RDF.type, HOBBIT.FeatureParameter));

            NodeIterator nodeIterator = model.listObjectsOfProperty(parameter, RDFS.range);
            RDFNode node;
            if (nodeIterator.hasNext()) {
                node = nodeIterator.next();
                if (node.isResource()) {
                    Resource typeResource = node.asResource();
                    configParam.setRange(typeResource.getURI());
                    // If this is an XSD resource
                    if (XSD.getURI().equals(typeResource.getNameSpace())) {
                        configParam.setDatatype(parseXsdType(typeResource));
                    } else if (model.contains(typeResource, RDF.type, RDFS.Class)
                            || model.contains(typeResource, RDF.type, OWL.Class)) {
                        // Maybe this parameter has a set of predefined enum
                        // values
                        configParam.setOptions(listOptions(model, typeResource));
                    }
                }
            }
            // If the datatype couldn't be found and there is no list of options
            if ((configParam.getDatatype() == null) && (configParam.getOptions() == null)) {
                configParam.setDatatype(Datatype.STRING);
                LOGGER.warn("Couldn't find datatype of parameter {}. Using String as default.", parameter.getURI());

            }
            benchmarkBean.getConfigurationParamNames().add(configParam.getName());
            benchmarkBean.getConfigurationParams().add(configParam);
        }
    }

    /**
     * Derives a list of options that are connected to the given parameter resource
     * via owl:oneOf predicates or <code>null</code> if no such resource could be
     * found.
     *
     * @param model
     *            the RDF model containing the options
     * @param typeResource
     *            the typ resource for which the options are possible values
     * @return a list of options or <code>null</code> if no option could be found
     */
    public static List<SelectOptionBean> listOptions(Model model, Resource typeResource) {
        ResIterator iterator = model.listSubjectsWithProperty(RDF.type, typeResource);
        Resource option;
        String optionLabel;
        List<SelectOptionBean> options = new ArrayList<>();
        while (iterator.hasNext()) {
            option = iterator.next();
            optionLabel = RdfHelper.getLabel(model, option);
            options.add(new SelectOptionBean(optionLabel != null ? optionLabel : option.getURI(), option.getURI()));
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
        } else if (XSD.unsignedInt.equals(typeResource) || XSD.positiveInteger.equals(typeResource)) {
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
        challenge.setId(challengeResource.getURI());
        challenge.setName(RdfHelper.getLabel(model, challengeResource));
        challenge.setDescription(RdfHelper.getDescription(model, challengeResource));
        challenge.setOrganizer(RdfHelper.getStringValue(model, challengeResource, HOBBIT.organizer));
        challenge.setHomepage(RdfHelper.getStringValue(model, challengeResource, FOAF.homepage));
        Literal literal = RdfHelper.getLiteral(model, challengeResource, HOBBIT.closed);
        if (literal != null) {
            try {
                challenge.setClosed(getBooleanFromLiteral(literal));
            } catch (DatatypeFormatException e) {
                LOGGER.error(
                        "Got an unexpected non-boolean literal that couldn't be interpreted as flag. Returning null.",
                        e);
                return null;
            }
        }

        literal = RdfHelper.getLiteral(model, challengeResource, HOBBIT.visible);
        if (literal != null) {
            try {
                challenge.setVisible(getBooleanFromLiteral(literal));
            } catch (DatatypeFormatException e) {
                LOGGER.error(
                        "Got an unexpected non-boolean literal that couldn't be interpreted as flag. Returning null.",
                        e);
                return null;
            }
        }

        // Internally, we are only using UTC
        ZoneOffset offset = ZoneOffset.UTC;
        Calendar cal = getTolerantDateTimeValue(model, challengeResource, HOBBIT.executionDate);
        if (cal != null) {
            LocalDate localDate = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));
            LocalTime localTime = LocalTime.of(0, 0, 0);
            challenge.setExecutionDate(OffsetDateTime.of(localDate, localTime, offset));
        }
        cal = getTolerantDateTimeValue(model, challengeResource, HOBBIT.publicationDate);
        if (cal != null) {
            LocalDate localDate = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));
            LocalTime localTime = LocalTime.of(0, 0, 0);
            challenge.setPublishDate(OffsetDateTime.of(localDate, localTime, offset));
        }
        challenge.setTasks(listChallengeTasks(model, challengeResource));

        return challenge;
    }

    private static boolean getBooleanFromLiteral(Literal literal) throws DatatypeFormatException {
        try {
            return literal.getBoolean();
        } catch (DatatypeFormatException e) {
            // This litral is not a boolean. Try to understand it
            String lexicalForm = literal.getLexicalForm().toLowerCase();
            boolean result;
            if ("true".equals(lexicalForm) || "1".equals(lexicalForm)) {
                result = true;
            } else if ("false".equals(lexicalForm) || "0".equals(lexicalForm)) {
                result = false;
            } else {
                throw e;
            }
            LOGGER.warn("Interpreted the non-boolean literal {} as {}. This should be avoided.", literal.toString(),
                    result);
            return result;
        }
    }

    public static Calendar getTolerantDateTimeValue(Model model, Resource resource, Property property) {
        Calendar cal = RdfHelper.getDateTimeValue(model, resource, property);
        if (cal == null) {
            // try to read date instead
            cal = RdfHelper.getDateValue(model, resource, property);
        }
        return cal;
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
        task.setName(RdfHelper.getLabel(model, taskResource));
        task.setDescription(RdfHelper.getDescription(model, taskResource));
        Resource benchmarkResource = RdfHelper.getObjectResource(model, taskResource, HOBBIT.involvesBenchmark);
        if (benchmarkResource != null) {
            task.setBenchmark(createBenchmarkBean(model, benchmarkResource));
            task.getBenchmark().setSystems(listSystemBeans(model, taskResource));
        }
        task.setConfigurationParams(createParamValueBeans(model, taskResource, benchmarkResource));
        task.setId(taskResource.getURI());

        List<String> rankingKPIs = new ArrayList<>();
        Resource rankingKPIsSequence = RdfHelper.getObjectResource(model, taskResource, HOBBIT.rankingKPIs);
        if (rankingKPIsSequence != null) {
            SeqImpl sequence = new SeqImpl(rankingKPIsSequence, (ModelCom) model);
            NodeIterator sequenceIterator = sequence.iterator();
            while (sequenceIterator.hasNext()) {
                RDFNode node = sequenceIterator.next();
                if (node.isResource()) {
                    rankingKPIs.add(node.asResource().toString());
                }
            }
        }
        task.setRankingKPIs(rankingKPIs);

        return task;
    }

    public static List<SystemBean> listSystemBeans(Model model, Resource involvingResource) {
        List<SystemBean> systems = new ArrayList<>();
        if (model == null) {
            return systems;
        }
        // iterate over all systems
        NodeIterator systemIterator = model.listObjectsOfProperty(involvingResource, HOBBIT.involvesSystemInstance);
        RDFNode node;
        while (systemIterator.hasNext()) {
            node = systemIterator.next();
            if (node.isResource()) {
                systems.add(getSystemBean(model, node.asResource()));
            }
        }

        return systems;
    }

    public static SystemBean getSystemBean(Model model, Resource systemResource) {
        if (model == null) {
            return null;
        }
        return new SystemBean(systemResource.getURI(), RdfHelper.getLabel(model, systemResource),
                RdfHelper.getDescription(model, systemResource));
    }

    /**
     * Extracts configuration parameters of the given challenge task from the given
     * model.
     *
     * @param model
     *            the model containing the triples
     * @param taskResource
     *            the challenge task resource
     * @param benchResource
     *            the benchmark resource which might have hobbit:hasParameter
     *            triples. It is ignored if it is set to <code>null</code>
     * @return a list of configuration parameters
     */
    public static List<ConfigurationParamValueBean> createParamValueBeans(Model model, Resource taskResource,
            Resource benchResource) {
        if ((model == null) || (taskResource == null)) {
            return new ArrayList<>(0);
        }
        Map<String, ConfigurationParamValueBean> parameters = new HashMap<String, ConfigurationParamValueBean>();
        createParamValueBeans(model, taskResource,
                model.listResourcesWithProperty(RDF.type, HOBBIT.ConfigurableParameter), parameters);
        createParamValueBeans(model, taskResource, model.listResourcesWithProperty(RDF.type, HOBBIT.Parameter),
                parameters);
        if (benchResource != null) {
            createParamValueBeans(model, taskResource, model.listObjectsOfProperty(benchResource, HOBBIT.hasParameter),
                    parameters);
        }
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
                paramBean.setId(parameterUri);
                paramBean.setValue(RdfHelper.getStringValue(model, taskResource, paraProp));

                paramBean.setName(RdfHelper.getLabel(model, paraProp));
                if (paramBean.getName() == null) {
                    paramBean.setName(parameter.getURI());
                    LOGGER.info("The benchmark parameter {} does not have a label.", parameter.getURI());
                }
                paramBean.setDescription(RdfHelper.getDescription(model, paraProp));
                if (paramBean.getDescription() == null) {
                    LOGGER.info("The benchmark parameter {} does not have a description.", parameter.getURI());
                }
                NodeIterator nodeIterator = model.listObjectsOfProperty(parameter, RDFS.range);
                RDFNode node;
                if (nodeIterator.hasNext()) {
                    node = nodeIterator.next();
                    if (node.isResource()) {
                        Resource typeResource = node.asResource();
                        paramBean.setRange(typeResource.getURI());
                        // If this is an XSD resource
                        if (XSD.getURI().equals(typeResource.getNameSpace())) {
                            paramBean.setDatatype(parseXsdType(typeResource));
                        }
                    }
                }
                parameters.put(parameterUri, paramBean);
            }
        }
    }

    private static void createKPIBeans(Model model, Resource experiment, ResIterator kpiIterator,
            Map<String, KeyPerformanceIndicatorBean> kpis) {
        Resource kpi;
        Property kpiProp;
        String parameterUri;
        while (kpiIterator.hasNext()) {
            kpi = kpiIterator.next();
            parameterUri = kpi.getURI();
            kpiProp = model.getProperty(parameterUri);
            // If the KPI has not been seen before AND (it is either used as
            // property with the given resource OR the given resource is
            // connected via hobbit:measuresKPI with the KPI)
            if ((model.contains(experiment, kpiProp) || model.contains(experiment, HOBBIT.measuresKPI, kpi))
                    && !kpis.containsKey(parameterUri)) {
                KeyPerformanceIndicatorBean kpiBean = createKpiBean(model, experiment, kpiProp);
                kpis.put(parameterUri, kpiBean);
            }
        }
    }

    private static KeyPerformanceIndicatorBean createKpiBean(Model model, Resource experiment, Property kpiProp) {
        KeyPerformanceIndicatorBean kpiBean = null;
        // Try to parse the value of the KPI as a diagram
        Resource object = RdfHelper.getObjectResource(model, experiment, kpiProp);
        if (object != null) {
            // we have a resource as KPI. Check its type
            if (model.contains(object, RDF.type, DataCube.DataSet)) {
                kpiBean = createDiagramBean(model, kpiProp, object);
            }
        }
        // If it is not a diagram, create a normal bean
        if (kpiBean == null) {
            kpiBean = new KeyPerformanceIndicatorBean();
            kpiBean.setValue(RdfHelper.getStringValue(model, experiment, kpiProp));
        }
        kpiBean.setId(kpiProp.getURI());

        kpiBean.setName(RdfHelper.getLabel(model, kpiProp));
        if (kpiBean.getName() == null) {
            kpiBean.setName(kpiProp.getURI());
            LOGGER.info("The benchmark parameter {} does not have a label.", kpiProp.getURI());
        }
        kpiBean.setDescription(RdfHelper.getDescription(model, kpiProp));
        if (kpiBean.getDescription() == null) {
            LOGGER.info("The benchmark parameter {} does not have a description.", kpiProp.getURI());
        }
        NodeIterator nodeIterator = model.listObjectsOfProperty(kpiProp, RDFS.range);
        RDFNode node;
        if (nodeIterator.hasNext()) {
            node = nodeIterator.next();
            if (node.isResource()) {
                Resource typeResource = node.asResource();
                kpiBean.setRange(typeResource.getURI());
                // If this is an XSD resource
                if (XSD.getURI().equals(typeResource.getNameSpace())) {
                    kpiBean.setDatatype(parseXsdType(typeResource));
                }
            }
        }
        Resource ranking = RdfHelper.getObjectResource(model, kpiProp, HOBBIT.ranking);
        if (ranking != null) {
            kpiBean.setRanking(ranking.toString());
        }
        return kpiBean;
    }

    private static KeyPerformanceIndicatorBean createDiagramBean(Model model, Property kpiProp, Resource dataset) {
        DiagramBean bean = new DiagramBean();
        Resource structureNode = RdfHelper.getObjectResource(model, dataset, DataCube.structure);
        if (structureNode == null) {
            return null;
        }
        List<Resource> components = RdfHelper.getObjectResources(model, structureNode, DataCube.component);
        if (components.size() < 2) {
            return null;
        }
        Property dimensionProperty = null, measureProperty = null;
        for (Resource component : components) {
            Resource temp = RdfHelper.getObjectResource(model, component, DataCube.dimension);
            // If this is a dimension
            if ((temp != null) && (model.contains(temp, RDF.type, DataCube.DimensionProperty))) {
                dimensionProperty = model.getProperty(temp.getURI());
            } else {
                temp = RdfHelper.getObjectResource(model, component, DataCube.measure);
                // If this is a measure
                if ((temp != null) && (model.contains(temp, RDF.type, DataCube.MeasureProperty))) {
                    measureProperty = model.getProperty(temp.getURI());
                }
            }
        }
        // if the dimension or measure are missing
        if ((dimensionProperty == null) || (measureProperty == null)) {
            return null;
        }
        final Property fDimProp = dimensionProperty, fMeaProp = measureProperty;
        // go through the observations and collect the points
        List<Point> points = RdfHelper.getSubjectResources(model, DataCube.dataSet, dataset).parallelStream()
                // it should be an observation
                .filter(o -> model.contains(o, RDF.type, DataCube.Observation))
                // it should have a value for the dimension
                .filter(o -> model.contains(o, fDimProp))
                // it should have a value for the measure
                .filter(o -> model.contains(o, fMeaProp))
                // map the observations to points
                .map(o -> Point.createFromObservation(model, o, fDimProp, fMeaProp))
                // point creation should have been successful
                .filter(p -> (p != null)).collect(Collectors.toList());
        Collections.sort(points);
        bean.setData(points.toArray(new Point[points.size()]));
        bean.setLabel(RdfHelper.getLabel(model, measureProperty));
        return bean;
    }

    private static void createParamValueBeans(Model model, Resource taskResource, NodeIterator parameterIterator,
            Map<String, ConfigurationParamValueBean> parameters) {
        RDFNode node;
        Resource parameter;
        Property paraProp;
        String parameterUri;
        while (parameterIterator.hasNext()) {
            node = parameterIterator.next();
            if (node.isResource()) {
                parameter = node.asResource();
                parameterUri = parameter.getURI();
                paraProp = model.getProperty(parameterUri);
                if (model.contains(taskResource, paraProp) && !parameters.containsKey(parameterUri)) {
                    ConfigurationParamValueBean paramBean = new ConfigurationParamValueBean();
                    paramBean.setId(parameterUri);
                    paramBean.setValue(RdfHelper.getStringValue(model, taskResource, paraProp));
                    parameters.put(parameterUri, paramBean);
                }
            }
        }
    }

    @Deprecated
    public static String getChallengeId(String uri) {
        if (uri.startsWith(Constants.CHALLENGE_URI_NS)) {
            uri = uri.substring(Constants.CHALLENGE_URI_NS.length());
        }
        return uri;
    }

    @Deprecated
    public static String getChallengeUri(String id) {
        return Constants.CHALLENGE_URI_NS + id;
        // return id;
    }

    public static List<ExperimentBean> createExperimentBeans(Model model) {
        List<ExperimentBean> result = new ArrayList<>();
        if (model != null) {
            ResIterator expIterator = model.listResourcesWithProperty(RDF.type, HOBBIT.Experiment);
            while (expIterator.hasNext()) {
                result.add(createExperimentBean(model, expIterator.next()));
            }
        }
        return result;
    }

    public static ExperimentBean createExperimentBean(Model model, Resource experiment) {
        if (model == null) {
            return null;
        }
        ExperimentBean bean = new ExperimentBean();
        bean.setId(HobbitExperiments.getExperimentId(experiment));
        Resource benchmarkResource = RdfHelper.getObjectResource(model, experiment, HOBBIT.involvesBenchmark);
        if (benchmarkResource != null) {
            bean.setBenchmark(createConfiguredBenchmarkBean(model, benchmarkResource, experiment));
        }
        Resource systemResource = RdfHelper.getObjectResource(model, experiment, HOBBIT.involvesSystemInstance);
        if (systemResource != null) {
            bean.setSystem(getSystemBean(model, systemResource));
        }
        Resource challengeTask = RdfHelper.getObjectResource(model, experiment, HOBBIT.isPartOf);
        if (challengeTask != null) {
            bean.setChallengeTask(getChallengeTask(model, challengeTask));
        }
        Map<String, KeyPerformanceIndicatorBean> kpis = new HashMap<String, KeyPerformanceIndicatorBean>();
        createKPIBeans(model, experiment, model.listResourcesWithProperty(RDF.type, HOBBIT.KPI), kpis);
        bean.setKpis(new ArrayList<>(kpis.values()));
        bean.setDiagrams(bean.getKpis().stream().filter(k -> k.getClass().equals(DiagramBean.class))
                .map(k -> (DiagramBean) k).collect(Collectors.toList()));

        bean.setError(getErrorMessage(RdfHelper.getObjectResource(model, experiment, HOBBIT.terminatedWithError)));

        return bean;
    }

    public static List<TaskRegistrationBean> listRegisteredSystems(Model model) {
        List<TaskRegistrationBean> registrations = new ArrayList<>();
        if (model == null) {
            return registrations;
        }
        List<ChallengeBean> challenges = listChallenges(model);
        for (ChallengeBean challenge : challenges) {
            for (ChallengeTaskBean task : challenge.getTasks()) {
                BenchmarkBean benchmark = task.getBenchmark();
                if (benchmark != null) {
                    for (SystemBean system : task.getBenchmark().getSystems()) {
                        registrations.add(new TaskRegistrationBean(challenge.getId(), task.getId(), system.getId(), true));
                    }
                } else {
                    LOGGER.info("Task {} does not have a benchmark.", task.getId());
                }
            }
        }
        return registrations;
    }

    public static String getErrorMessage(Resource errorResource) {
        if (errorResource == null) {
            return null;
        }
        if (HobbitErrors.BenchmarkCrashed.equals(errorResource)) {
            return "The benchmark terminated with an error.";
        } else if (HobbitErrors.BenchmarkImageMissing.equals(errorResource)) {
            return "The benchmark image could not be loaded.";
        } else if (HobbitErrors.BenchmarkCreationError.equals(errorResource)) {
            return "The benchmark could not be created.";
        } else if (HobbitErrors.ExperimentTookTooMuchTime.equals(errorResource)) {
            return "The experiment took too much time.";
        } else if (HobbitErrors.SystemCrashed.equals(errorResource)) {
            return "The benchmarked system terminated with an error.";
        } else if (HobbitErrors.SystemImageMissing.equals(errorResource)) {
            return "The benchmarked system image could not be loaded.";
        } else if (HobbitErrors.SystemCreationError.equals(errorResource)) {
            return "The benchmarked system could not be created.";
        } else if (HobbitErrors.TerminatedByUser.equals(errorResource)) {
            return "The experiment has been terminated by the user.";
        } else if (HobbitErrors.UnexpectedError.equals(errorResource)) {
            return "An unexpected error occurred.";
        } else {
            return "An unknown error occurred.";
        }
    }
}
