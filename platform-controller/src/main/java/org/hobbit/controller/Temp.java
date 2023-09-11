package org.hobbit.controller;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitExperiments;

public class Temp {

    public static void main(String[] args) {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = new FileInputStream("a2kb-results.ttl")) {
            model.read(in, "", "TTL");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Property dataset = model.getProperty("http://w3id.org/gerbil/hobbit/vocab#hasDataset");
        Property micPre = model.getProperty("http://w3id.org/gerbil/vocab#microPrecision");
        Property micRec = model.getProperty("http://w3id.org/gerbil/vocab#microRecall");
        Property micF1 = model.getProperty("http://w3id.org/gerbil/vocab#microF1");
        Property macPre = model.getProperty("http://w3id.org/gerbil/vocab#macroPrecision");
        Property macRec = model.getProperty("http://w3id.org/gerbil/vocab#macroRecall");
        Property macF1 = model.getProperty("http://w3id.org/gerbil/vocab#macroF1");
        Property errors = model.getProperty("http://w3id.org/gerbil/vocab#errorCount");
        Property avgMillis = model.getProperty("http://w3id.org/gerbil/vocab#avgMillisPerDoc");
        System.out.println("\"ID\",\"system\",\"dataset\",\"mic. P\",\"mic. R\",\"mic. F1\",\"mac. P\", \"mac. R\", \"mac. F1\",\"errors\",\"avg millis/doc\"");
        List<Resource> experiments = RdfHelper.getSubjectResources(model, RDF.type, HOBBIT.Experiment);
        for (Resource experiment : experiments) {
            System.out.print('"');
            System.out.print(HobbitExperiments.getExperimentId(experiment));
            System.out.print("\",\"");
            System.out.print(RdfHelper.getLabel(model, RdfHelper.getObjectResource(model, experiment, HOBBIT.involvesSystemInstance)));
            System.out.print("\",\"");
            System.out.print(RdfHelper.getObjectResource(model, experiment, dataset).getLocalName());
            System.out.print("\",\"");
            if (model.contains(experiment, HOBBIT.terminatedWithError, (RDFNode) null)) {
                System.out.println("\"");
            } else {
                System.out.print(RdfHelper.getStringValue(model, experiment, micPre));
                System.out.print("\",\"");
                System.out.print(RdfHelper.getStringValue(model, experiment, micRec));
                System.out.print("\",\"");
                System.out.print(RdfHelper.getStringValue(model, experiment, micF1));
                System.out.print("\",\"");
                System.out.print(RdfHelper.getStringValue(model, experiment, macPre));
                System.out.print("\",\"");
                System.out.print(RdfHelper.getStringValue(model, experiment, macRec));
                System.out.print("\",\"");
                System.out.print(RdfHelper.getStringValue(model, experiment, macF1));
                System.out.print("\",\"");
                System.out.print(RdfHelper.getStringValue(model, experiment, errors));
                System.out.print("\",\"");
                System.out.print(RdfHelper.getStringValue(model, experiment, avgMillis));
                System.out.println("\"");
            }
        }
    }
}
