package de.usu.research.hobbit.gui.rabbitmq;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.Constants;
import org.hobbit.utils.test.ModelComparisonHelper;
import org.hobbit.vocab.HOBBIT;
import org.junit.Assert;
import org.junit.Test;

import de.usu.research.hobbit.gui.rest.Datatype;
import de.usu.research.hobbit.gui.rest.beans.ConfigurationParamValueBean;
import de.usu.research.hobbit.gui.rest.beans.SubmitModelBean;

public class PlatformControllerClientTest {

    @Test
    public void testAddParameters() {
        Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(expectedModel.getResource(Constants.NEW_EXPERIMENT_URI), RDF.type, HOBBIT.Experiment);
        expectedModel.add(expectedModel.getResource(Constants.NEW_EXPERIMENT_URI), HOBBIT.involvesBenchmark,
                expectedModel.getResource("http://w3id.org/hobbit/platform_benchmark/vocab#PlatformBenchmark"));
        expectedModel.add(expectedModel.getResource(Constants.NEW_EXPERIMENT_URI), HOBBIT.involvesSystemInstance,
                expectedModel.getResource("http://w3id.org/hobbit/platform_benchmark/vocab#PlatformBenchmarkSystem"));
        expectedModel.add(expectedModel.getResource(Constants.NEW_EXPERIMENT_URI),
                expectedModel.getProperty("http://w3id.org/hobbit/platform_benchmark/vocab#numberOfDataGenerators"),
                "2", XSDDatatype.XSDunsignedInt);
        expectedModel.add(expectedModel.getResource(Constants.NEW_EXPERIMENT_URI),
                expectedModel.getProperty("http://w3id.org/hobbit/platform_benchmark/vocab#someProp"),
                expectedModel.getResource("http://w3id.org/hobbit/platform_benchmark/vocab#SomeResource"));
        expectedModel.add(expectedModel.getResource(Constants.NEW_EXPERIMENT_URI),
                expectedModel.getProperty("http://w3id.org/hobbit/platform_benchmark/vocab#seed"), "31",
                XSDDatatype.XSDinteger);

        SubmitModelBean benchmarkConf = new SubmitModelBean();
        benchmarkConf.setBenchmark("http://w3id.org/hobbit/platform_benchmark/vocab#PlatformBenchmark");
        benchmarkConf.setSystem("http://w3id.org/hobbit/platform_benchmark/vocab#PlatformBenchmarkSystem");
        List<ConfigurationParamValueBean> parameters = new ArrayList<>();
        ConfigurationParamValueBean param = new ConfigurationParamValueBean();
        param.setDatatype(Datatype.UNSIGNED_INT);
        param.setId("http://w3id.org/hobbit/platform_benchmark/vocab#numberOfDataGenerators");
        param.setValue("2");
        param.setRange("http://www.w3.org/2001/XMLSchema#unsignedInt");
        parameters.add(param);
        param = new ConfigurationParamValueBean();
        param.setDatatype(null);
        param.setId("http://w3id.org/hobbit/platform_benchmark/vocab#someProp");
        param.setValue("http://w3id.org/hobbit/platform_benchmark/vocab#SomeResource");
        param.setRange("http://w3id.org/hobbit/platform_benchmark/vocab#SomeResourceType");
        parameters.add(param);
        param = new ConfigurationParamValueBean();
        param.setDatatype(Datatype.INTEGER);
        param.setId("http://w3id.org/hobbit/platform_benchmark/vocab#seed");
        param.setValue("31");
        param.setRange("http://www.w3.org/2001/XMLSchema#integer");
        parameters.add(param);

        benchmarkConf.setConfigurationParams(parameters);

        Model model = ModelFactory.createDefaultModel();

        String benchmarkInstanceId = Constants.NEW_EXPERIMENT_URI;
        Resource benchmarkInstanceResource = model.createResource(benchmarkInstanceId);
        model.add(benchmarkInstanceResource, RDF.type, HOBBIT.Experiment);
        model.add(benchmarkInstanceResource, HOBBIT.involvesBenchmark,
                model.createResource(benchmarkConf.getBenchmark()));
        model.add(benchmarkInstanceResource, HOBBIT.involvesSystemInstance,
                model.createResource(benchmarkConf.getSystem()));

        model = PlatformControllerClient.addParameters(model, benchmarkInstanceResource,
                benchmarkConf.getConfigurationParams());

        Set<Statement> stmts = ModelComparisonHelper.getMissingStatements(expectedModel, model);
        Assert.assertEquals(stmts.toString(), 0, stmts.size());
        stmts = ModelComparisonHelper.getMissingStatements(model, expectedModel);
        Assert.assertEquals(stmts.toString(), 0, stmts.size());
    }
}
