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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.utils.test.ModelComparisonHelper;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitExperiments;
import org.junit.Assert;
import org.junit.Test;

import de.usu.research.hobbit.gui.rest.Datatype;
import de.usu.research.hobbit.gui.rest.beans.ConfigurationParamValueBean;
import de.usu.research.hobbit.gui.rest.beans.SubmitModelBean;

public class PlatformControllerClientTest {

    @Test
    public void testAddParameters() {
        Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(HobbitExperiments.New, RDF.type, HOBBIT.Experiment);
        expectedModel.add(HobbitExperiments.New, HOBBIT.involvesBenchmark,
                expectedModel.getResource("http://w3id.org/hobbit/platform_benchmark/vocab#PlatformBenchmark"));
        expectedModel.add(HobbitExperiments.New, HOBBIT.involvesSystemInstance,
                expectedModel.getResource("http://w3id.org/hobbit/platform_benchmark/vocab#PlatformBenchmarkSystem"));
        expectedModel.add(HobbitExperiments.New,
                expectedModel.getProperty("http://w3id.org/hobbit/platform_benchmark/vocab#numberOfDataGenerators"),
                "2", XSDDatatype.XSDunsignedInt);
        expectedModel.add(HobbitExperiments.New,
                expectedModel.getProperty("http://w3id.org/hobbit/platform_benchmark/vocab#someProp"),
                expectedModel.getResource("http://w3id.org/hobbit/platform_benchmark/vocab#SomeResource"));
        expectedModel.add(HobbitExperiments.New,
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

        model.add(HobbitExperiments.New, RDF.type, HOBBIT.Experiment);
        model.add(HobbitExperiments.New, HOBBIT.involvesBenchmark,
                model.createResource(benchmarkConf.getBenchmark()));
        model.add(HobbitExperiments.New, HOBBIT.involvesSystemInstance,
                model.createResource(benchmarkConf.getSystem()));

        model = PlatformControllerClient.addParameters(model, HobbitExperiments.New,
                benchmarkConf.getConfigurationParams());

        Set<Statement> stmts = ModelComparisonHelper.getMissingStatements(expectedModel, model);
        Assert.assertEquals(stmts.toString(), 0, stmts.size());
        stmts = ModelComparisonHelper.getMissingStatements(model, expectedModel);
        Assert.assertEquals(stmts.toString(), 0, stmts.size());
    }
}
