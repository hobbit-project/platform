package de.usu.research.hobbit.gui.rabbitmq;

import org.apache.jena.rdf.model.Model;

import de.usu.research.hobbit.gui.rest.beans.BenchmarkBean;

public class BenchmarkBeanTest extends AbstractRdfHelperTest {

    public BenchmarkBeanTest() {
        super("de/usu/research/hobbit/gui/rabbitmq/benchmark.ttl", "de/usu/research/hobbit/gui/rabbitmq/benchmarkBeanContent.ttl");
    }

    @Override
    protected Model performTransformation(Model model) {
        BenchmarkBean bean = RdfModelHelper.createBenchmarkBean(model);
        Model resultModel = RdfModelCreationHelper.createNewModel();
        RdfModelCreationHelper.addBenchmark(bean, resultModel);
        return resultModel;
    }

}
