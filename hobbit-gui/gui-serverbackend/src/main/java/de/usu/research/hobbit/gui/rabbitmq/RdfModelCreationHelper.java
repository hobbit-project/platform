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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitChallenges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rest.Datatype;
import de.usu.research.hobbit.gui.rest.beans.BenchmarkBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeBean;
import de.usu.research.hobbit.gui.rest.beans.ChallengeTaskBean;
import de.usu.research.hobbit.gui.rest.beans.ConfigurationParamBean;
import de.usu.research.hobbit.gui.rest.beans.ConfigurationParamValueBean;
import de.usu.research.hobbit.gui.rest.beans.KeyPerformanceIndicatorBean;
import de.usu.research.hobbit.gui.rest.beans.SelectOptionBean;
import de.usu.research.hobbit.gui.rest.beans.SystemBean;

/**
 * Implements simple methods to create RDFmodel based on given front end beans.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class RdfModelCreationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdfModelCreationHelper.class);

    public static final String KPI_SEQ_APPENDIX = "_KPIs";

    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    private static final Set<String> CHALLENGE_MODEL_PREDICATES = new HashSet<String>(
            Arrays.asList(RDF.type.toString(), RDFS.label.toString(), RDFS.comment.toString(), FOAF.homepage.toString(),
                    HOBBIT.organizer.toString(), HOBBIT.executionDate.toString(), HOBBIT.publicationDate.toString(),
                    HOBBIT.visible.toString(), HOBBIT.closed.toString()));

    /**
     * Creates a new RDF model with some predefined prefixes.
     *
     * @return a new RDF model
     */
    public static Model createNewModel() {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("rdf", RDF.getURI());
        model.setNsPrefix("rdfs", RDFS.getURI());
        model.setNsPrefix("hobbit", HOBBIT.getURI());
        model.setNsPrefix("chal", HobbitChallenges.getURI());
        return model;
    }

    /**
     * Adds the given challenge to the given model and returns the created
     * {@link Resource} of the challenge.
     *
     * @param challenge
     *            the bean containing the information about the challenge that
     *            should be added
     * @param model
     *            the RDF model to which the challenge should be added
     * @return the {@link Resource} representing the newly created challenge
     */
    public static Resource addChallenge(ChallengeBean challenge, Model model) {
        String challengeUri = challenge.getId();
        Resource challengeResource = model.getResource(challengeUri);
        model.add(challengeResource, RDF.type, HOBBIT.Challenge);
        if (challenge.getName() != null) {
            model.add(challengeResource, RDFS.label, challenge.getName(), "en");
        }
        if (challenge.getDescription() != null) {
            model.add(challengeResource, RDFS.comment, challenge.getDescription(), "en");
        }
        if (challenge.getOrganizer() != null) {
            model.add(challengeResource, HOBBIT.organizer, challenge.getOrganizer(), "en");
        }
        if (challenge.getExecutionDate() != null) {
            model.addLiteral(challengeResource, HOBBIT.executionDate,
                    getDateLiteral(model, challenge.getExecutionDate()));
        }
        if (challenge.getPublishDate() != null) {
            model.addLiteral(challengeResource, HOBBIT.publicationDate,
                    getDateLiteral(model, challenge.getPublishDate()));
        }
        if (challenge.getHomepage() != null) {
            String challengeHomepage = challenge.getHomepage();
            // If the homepage does not start with http:// or https://
            if ((!challengeHomepage.startsWith("http://")) && (!challengeHomepage.startsWith("https://"))) {
                challengeHomepage = "http://" + challengeHomepage;
            }
            try {
                model.add(challengeResource, FOAF.homepage, model.getResource(challengeHomepage));
            } catch (Exception e) {
                LOGGER.warn("Couldn't store \"{}\" as homepage URL. It will be ignored.", challengeHomepage);
            }
        }
        model.addLiteral(challengeResource, HOBBIT.visible, challenge.isVisible());
        model.addLiteral(challengeResource, HOBBIT.closed, challenge.isClosed());
        if (challenge.getTasks() != null) {
            Resource taskResource;
            for (ChallengeTaskBean task : challenge.getTasks()) {
                taskResource = addChallengeTask(task, model);
                model.add(taskResource, HOBBIT.isTaskOf, challengeResource);
            }
        }
        return challengeResource;
    }

    /**
     * Adds the given challenge task to the given model and returns the created
     * {@link Resource} of the task.
     *
     * @param task
     *            the bean containing the information about the challenge task that
     *            should be added
     * @param model
     *            the RDF model to which the challenge task should be added
     * @return the {@link Resource} representing the newly created challenge task
     */
    public static Resource addChallengeTask(ChallengeTaskBean task, Model model) {
        String taskUri = task.getId();
        Resource taskResource = model.getResource(taskUri);
        model.add(taskResource, RDF.type, HOBBIT.ChallengeTask);
        if (task.getName() != null) {
            model.add(taskResource, RDFS.label, task.getName(), "en");
        }
        if (task.getDescription() != null) {
            model.add(taskResource, RDFS.comment, task.getDescription(), "en");
        }
        Resource benchmarkResource = null;
        if (task.getBenchmark() != null) {
            benchmarkResource = addBenchmark(task.getBenchmark(), model);
            model.add(taskResource, HOBBIT.involvesBenchmark, benchmarkResource);
            if (task.getBenchmark().getSystems() != null) {
                Resource systemResource;
                for (SystemBean system : task.getBenchmark().getSystems()) {
                    systemResource = addSystem(system, model);
                    model.add(taskResource, HOBBIT.involvesSystemInstance, systemResource);
                }
            }
        }
        if (task.getConfigurationParams() != null) {
            for (ConfigurationParamValueBean parameter : task.getConfigurationParams()) {
                addParameterValue(parameter, taskResource, benchmarkResource, model);
            }
        }
        List<String> rankingKpis = task.getRankingKPIs();
        if ((rankingKpis != null) && (rankingKpis.size() > 0)) {
            Seq kpiSeq = model.createSeq(taskUri + KPI_SEQ_APPENDIX);
            for (String kpi : rankingKpis) {
                kpiSeq.add(model.getResource(kpi));
            }
            model.add(kpiSeq, RDF.type, HOBBIT.KPISeq);
            model.add(taskResource, HOBBIT.rankingKPIs, kpiSeq);
        }
        return taskResource;
    }

    /**
     * Adds the given benchmark to the given model and returns the created
     * {@link Resource} of the benchmark. Note that the list of systems (
     * {@link BenchmarkBean#systems}) is not added to the model.
     *
     * @param benchmark
     *            the bean containing the information about the benchmark that
     *            should be added
     * @param model
     *            the RDF model to which the benchmark should be added
     * @return the {@link Resource} representing the newly created benchmark
     */
    public static Resource addBenchmark(BenchmarkBean benchmark, Model model) {
        Resource benchmarkResource = model.getResource(benchmark.getId());
        model.add(benchmarkResource, RDF.type, HOBBIT.Benchmark);
        if ((benchmark.getName() != null) && (!benchmark.getName().equals(benchmark.getId()))) {
            model.add(benchmarkResource, RDFS.label, benchmark.getName(), "en");
        }
        if ((benchmark.getDescription() != null) && (!benchmark.getDescription().equals(benchmark.getId()))) {
            model.add(benchmarkResource, RDFS.comment, benchmark.getDescription(), "en");
        }
        if (benchmark.getConfigurationParams() != null) {
            Resource paramResource;
            for (ConfigurationParamBean parameter : benchmark.getConfigurationParams()) {
                paramResource = addParameter(parameter, model);
                model.add(benchmarkResource, HOBBIT.hasParameter, paramResource);
            }
        }
        if (benchmark.getKpis() != null) {
            Resource kpiResource;
            for (KeyPerformanceIndicatorBean kpi : benchmark.getKpis()) {
                kpiResource = addKpi(kpi, model);
                model.add(benchmarkResource, HOBBIT.measuresKPI, kpiResource);
            }
        }
        return benchmarkResource;
    }

    /**
     * Adds the given configuration parameter to the given model and returns the
     * created {@link Resource} of the parameter.
     *
     * @param parameter
     *            the bean containing the information about the parameter that
     *            should be added
     * @param model
     *            the RDF model to which the parameter should be added
     * @return the {@link Resource} representing the newly created parameter
     */
    public static Resource addParameter(ConfigurationParamBean parameter, Model model) {
        Resource paramResource = model.getResource(parameter.getId());
        model.add(paramResource, RDF.type, HOBBIT.ConfigurableParameter);
        model.add(paramResource, RDF.type, HOBBIT.Parameter);
        if (parameter.isFeature()) {
            model.add(paramResource, RDF.type, HOBBIT.FeatureParameter);
        }
        if ((parameter.getName() != null) && (!parameter.getName().equals(parameter.getId()))) {
            model.add(paramResource, RDFS.label, parameter.getName(), "en");
        }
        if ((parameter.getDescription() != null) && (!parameter.getDescription().equals(parameter.getId()))) {
            model.add(paramResource, RDFS.comment, parameter.getDescription(), "en");
        }
        if (parameter.getDatatype() != null) {
            XSDDatatype datatype = datatypeToXsd(parameter.getDatatype());
            if (datatype != null) {
                model.add(paramResource, RDFS.range, model.getResource(datatype.getURI()));
                if (parameter.getDefaultValue() != null) {
                    model.add(paramResource, HOBBIT.defaultValue, parameter.getDefaultValue(), datatype);
                }
            }
        } else if (parameter.getOptions() != null) {
            Resource parameterType = model.getResource(parameter.getRange());
            model.add(paramResource, RDFS.range, parameterType);
            model.add(parameterType, RDF.type, RDFS.Class);
            model.add(parameterType, RDF.type, OWL.Class);
            Resource optionRes;
            for (SelectOptionBean option : parameter.getOptions()) {
                optionRes = model.createResource(option.getValue());
                model.add(optionRes, RDF.type, parameterType);
                if (!option.getValue().equals(option.getLabel())) {
                    model.add(optionRes, RDFS.label, option.getLabel(), "en");
                }
            }
            if (parameter.getDefaultValue() != null) {
                model.add(paramResource, HOBBIT.defaultValue, model.getResource(parameter.getDefaultValue()));
            }
        }
        return paramResource;
    }

    private static Resource addKpi(KeyPerformanceIndicatorBean kpi, Model model) {
        Resource kpiResource = model.getResource(kpi.getId());
        model.add(kpiResource, RDF.type, HOBBIT.KPI);
        if ((kpi.getName() != null) && (!kpi.getName().equals(kpi.getId()))) {
            model.add(kpiResource, RDFS.label, kpi.getName(), "en");
        }
        if ((kpi.getDescription() != null) && (!kpi.getDescription().equals(kpi.getId()))) {
            model.add(kpiResource, RDFS.comment, kpi.getDescription(), "en");
        }
        if (kpi.getDatatype() != null) {
            XSDDatatype datatype = datatypeToXsd(kpi.getDatatype());
            if (datatype != null) {
                model.add(kpiResource, RDFS.range, model.getResource(datatype.getURI()));
            }
        }
        if (kpi.getRanking() != null) {
            model.add(kpiResource, HOBBIT.ranking, model.getResource(kpi.getRanking()));
        }
        return kpiResource;
    }

    private static void addParameterValue(ConfigurationParamValueBean parameter, Resource taskResource,
            Resource benchmarkResource, Model model) {

        Property parameterProperty = model.getProperty(parameter.getId());
        model.add(parameterProperty, RDF.type, HOBBIT.ConfigurableParameter);
        model.add(parameterProperty, RDF.type, HOBBIT.Parameter);

        Resource typeResource = null;
        if (parameter.getDatatype() != null) {
            XSDDatatype type = datatypeToXsd(parameter.getDatatype());
            typeResource = model.getResource(type.getURI());
            try {
                model.add(taskResource, parameterProperty,
                        model.createTypedLiteral(type.parse(parameter.getValue()), type));
            } catch (DatatypeFormatException e) {
                LOGGER.error(
                        "Couldn't create typed literal for " + parameter.toString() + ". Adding literal without type.",
                        e);
                model.add(taskResource, parameterProperty, parameter.getValue());
            }
        } else {
            Resource valueResource = model.getResource(parameter.getValue());
            model.add(taskResource, parameterProperty, valueResource);
            if (parameter.getRange() != null) {
                typeResource = model.getResource(parameter.getRange());
            }
        }

        if (typeResource != null) {
            model.add(parameterProperty, RDFS.range, typeResource);
        }
        if ((parameter.getName() != null) && (!parameter.getName().equals(parameter.getId()))) {
            model.add(parameterProperty, RDFS.label, parameter.getName(), "en");
        }
        if ((parameter.getDescription() != null) && (!parameter.getDescription().equals(parameter.getId()))) {
            model.add(parameterProperty, RDFS.comment, parameter.getDescription(), "en");
        }
        if (benchmarkResource != null) {
            model.add(benchmarkResource, HOBBIT.hasParameter, parameterProperty);
        }
    }

    public static Resource addSystem(SystemBean system, Model model) {
        Resource systemResource = model.getResource(system.getId());
        if (system.getName() != null) {
            model.add(systemResource, RDFS.label, system.getName(), "en");
        }
        if (system.getDescription() != null) {
            model.add(systemResource, RDFS.comment, system.getDescription(), "en");
        }
        return systemResource;
    }

    public static XSDDatatype datatypeToXsd(Datatype datatype) {
        switch (datatype) {
        case BOOLEAN:
            return XSDDatatype.XSDboolean;
        case DECIMAL:
            return XSDDatatype.XSDdecimal;
        case DOUBLE:
            return XSDDatatype.XSDdouble;
        case FLOAT:
            return XSDDatatype.XSDfloat;
        case INTEGER:
            return XSDDatatype.XSDint;
        case STRING:
            return XSDDatatype.XSDstring;
        case UNSIGNED_INT:
            return XSDDatatype.XSDunsignedInt;
        default:
            return null;
        }
    }

    public static Literal getDateLiteral(Model model, OffsetDateTime date) {
        // Calendar cal = Calendar.getInstance(DEFAULT_TIME_ZONE);
        // cal.set(date.getYear(), date.getMonth().getValue() - 1,
        // date.getDayOfMonth(), date.getHour(), date.getMinute(),
        // date.getSecond());
        // cal.set(Calendar.MILLISECOND, 0);
        // return model.createTypedLiteral(new XSDDateTime(cal),
        // XSDDatatype.XSDdateTime);
        String str = date.format(DateTimeFormatter.ISO_DATE_TIME);
        return model.createTypedLiteral(XSDDatatype.XSDdateTime.parseValidated(str), XSDDatatype.XSDdateTime);
    }

    /**
     * This method removes all triples from the given model which can not be
     * represented as bean. It is necessary to do that before comparing two RDF
     * models with each other if one of the has been created from a challenge bean
     * using {@link #addChallenge(ChallengeBean, Model)}.
     *
     * @param model
     */
    public static void reduceModelToChallenge(Model model, Resource challenge) {
        // remove all statements that have the challenge as subject but not one of the
        // allowed predicates
        model.remove(model.listStatements(challenge, null, (RDFNode) null).toList().stream()
                .filter(s -> !CHALLENGE_MODEL_PREDICATES.contains(s.getPredicate().getURI()))
                .collect(Collectors.toList()));
        // Go through all tasks
        ResIterator taskIterator = model.listSubjectsWithProperty(HOBBIT.isTaskOf, challenge);
        Resource task;
        List<Statement> stmtsToRemove = new ArrayList<Statement>();
        while (taskIterator.hasNext()) {
            task = taskIterator.next();
            // XXX TEMPORARILY: remove all KPISeq information
            List<Statement> temp = model.listStatements(task, HOBBIT.rankingKPIs, (RDFNode) null).toList();
            stmtsToRemove.addAll(temp);
            for (Statement s : temp) {
                if (s.getObject().isResource()) {
                    stmtsToRemove
                            .addAll(model.listStatements(s.getObject().asResource(), null, (RDFNode) null).toList());
                }
            }
            // Remove all registration information since they are handled by a different method
            stmtsToRemove.addAll(model.listStatements(task, HOBBIT.involvesSystemInstance, (RDFNode) null).toList());
        }
        model.remove(stmtsToRemove);
    }
}
