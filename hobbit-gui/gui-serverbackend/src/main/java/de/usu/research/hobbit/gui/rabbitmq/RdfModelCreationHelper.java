package de.usu.research.hobbit.gui.rabbitmq;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
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
import de.usu.research.hobbit.gui.rest.SystemBean;

/**
 * Implements simple methods to create RDFmodel based on given front end beans.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class RdfModelCreationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdfModelCreationHelper.class);

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
        model.setNsPrefix("chal", Constants.CHALLENGE_URI_NS);
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
        String challengeUri = RdfModelHelper.getChallengeUri(challenge.id);
        Resource challengeResource = model.getResource(challengeUri);
        model.add(challengeResource, RDF.type, HOBBIT.Challenge);
        if (challenge.name != null) {
            model.add(challengeResource, RDFS.label, challenge.name, "en");
        }
        if (challenge.description != null) {
            model.add(challengeResource, RDFS.comment, challenge.description, "en");
        }
        if (challenge.organizer != null) {
            model.add(challengeResource, HOBBIT.organizer, challenge.organizer, "en");
        }
        if (challenge.executionDate != null) {
            LOGGER.error("Not implemented until now. Clarify the format of the date!");
            // TODO add date to model
        }
        // TODO visible has no matching RDF property
        model.addLiteral(challengeResource, HOBBIT.closed, challenge.closed);
        if (challenge.tasks != null) {
            Resource taskResource;
            for (ChallengeTaskBean task : challenge.tasks) {
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
     *            the bean containing the information about the challenge task
     *            that should be added
     * @param model
     *            the RDF model to which the challenge task should be added
     * @return the {@link Resource} representing the newly created challenge
     *         task
     */
    public static Resource addChallengeTask(ChallengeTaskBean task, Model model) {
        String taskUri = RdfModelHelper.getChallengeUri(task.id);
        Resource taskResource = model.getResource(taskUri);
        model.add(taskResource, RDF.type, HOBBIT.ChallengeTask);
        if (task.name != null) {
            model.add(taskResource, RDFS.label, task.name, "en");
        }
        if (task.description != null) {
            model.add(taskResource, RDFS.comment, task.description, "en");
        }
        if (task.benchmark != null) {
            Resource benchmarkResource = addBenchmark(task.benchmark, model);
            model.add(taskResource, HOBBIT.involvesBenchmark, benchmarkResource);
            if (task.benchmark.systems != null) {
                Resource systemResource;
                for (SystemBean system : task.benchmark.systems) {
                    systemResource = addSystem(system, model);
                    model.add(taskResource, HOBBIT.involvesSystemInstance, systemResource);
                }
            }
        }
        if (task.configurationParams != null) {
            for (ConfigurationParamValueBean parameter : task.configurationParams) {
                addParameterValue(parameter, taskResource, model);
            }
        }
        return taskResource;
    }

    /**
     * Adds the given benchmark to the given model and returns the created
     * {@link Resource} of the benchmark. Note that the list of systems
     * ({@link BenchmarkBean#systems}) is not added to the model.
     * 
     * @param benchmark
     *            the bean containing the information about the benchmark that
     *            should be added
     * @param model
     *            the RDF model to which the benchmark should be added
     * @return the {@link Resource} representing the newly created benchmark
     */
    public static Resource addBenchmark(BenchmarkBean benchmark, Model model) {
        Resource benchmarkResource = model.getResource(benchmark.id);
        model.add(benchmarkResource, RDF.type, HOBBIT.Benchmark);
        if (benchmark.name != null) {
            model.add(benchmarkResource, RDFS.label, benchmark.name, "en");
        }
        if (benchmark.description != null) {
            model.add(benchmarkResource, RDFS.comment, benchmark.description, "en");
        }
        if (benchmark.configurationParams != null) {
            Resource paramResource;
            for (ConfigurationParamBean parameter : benchmark.configurationParams) {
                paramResource = addParameter(parameter, model);
                model.add(benchmarkResource, HOBBIT.hasParameter, paramResource);
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
        Resource paramResource = model.getResource(parameter.id);
        model.add(paramResource, RDF.type, HOBBIT.ConfigurableParameter);
        model.add(paramResource, RDF.type, HOBBIT.Parameter);
        if (parameter.isFeature) {
            model.add(paramResource, RDF.type, HOBBIT.FeatureParameter);
        }
        if (parameter.name != null) {
            model.add(paramResource, RDFS.label, parameter.name, "en");
        }
        if (parameter.description != null) {
            model.add(paramResource, RDFS.comment, parameter.description, "en");
        }
        if (parameter.datatype != null) {
            XSDDatatype datatype = datatypeToXsd(parameter.datatype);
            if (datatype != null) {
                model.add(paramResource, RDFS.range, model.getResource(datatype.getURI()));
                if (parameter.defaultValue != null) {
                    model.add(paramResource, HOBBIT.defaultValue, parameter.defaultValue, datatype);
                }
            }
        } else if (parameter.options != null) {
            Resource parameterType = model.getResource(parameter.range);
            model.add(paramResource, RDFS.range, parameterType);
            model.add(parameterType, RDF.type, RDFS.Class);
            model.add(parameterType, RDF.type, OWL.Class);
            Resource optionRes, linkedListCell = null, oldCell = null;
            for (SelectOptionBean option : parameter.options) {
                oldCell = linkedListCell;
                linkedListCell = model.createResource(new AnonId());
                if (oldCell == null) {
                    model.add(parameterType, OWL.oneOf, linkedListCell);
                } else {
                    model.add(oldCell, RDF.rest, linkedListCell);
                }
                optionRes = model.createResource(option.value);
                model.add(linkedListCell, RDF.first, optionRes);
                if (!option.value.equals(option.label)) {
                    model.add(optionRes, RDFS.label, option.label, "en");
                }
            }
            model.add(linkedListCell, RDF.rest, RDF.nil);
            if (parameter.defaultValue != null) {
                model.add(paramResource, HOBBIT.defaultValue, model.getResource(parameter.defaultValue));
            }
        }
        return paramResource;
    }

    private static void addParameterValue(ConfigurationParamValueBean parameter, Resource taskResource, Model model) {
        // TODO Auto-generated method stub

    }

    public static Resource addSystem(SystemBean system, Model model) {
        // TODO Auto-generated method stub
        return null;
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
}
