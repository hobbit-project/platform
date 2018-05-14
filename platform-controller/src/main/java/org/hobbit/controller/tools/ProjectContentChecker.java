package org.hobbit.controller.tools;

import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.hobbit.controller.gitlab.GitlabController;
import org.hobbit.controller.gitlab.GitlabControllerImpl;
import org.hobbit.controller.gitlab.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectContentChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectContentChecker.class);

    public static void main(String[] args) throws InterruptedException {
        String queryString = null;
        if (args.length < 2) {
            System.out.println(
                    "Error: wrong usage. The following parameters are necessary:\n <security-token> <SPARQL-construct-Query>");
            return;
        } else if (args.length == 2) {
            queryString = args[1];
        } else {
            int count = 0;
            for (int i = 1; i < args.length; i++) {
                count += args[i].length();
            }
            count += args.length - 1;
            StringBuilder builder = new StringBuilder(count);
            for (int i = 1; i < args.length; i++) {
                builder.append(args[i]);
                if (i < (args.length - 1)) {
                    builder.append(' ');
                }
            }
            queryString = builder.toString();
        }
        GitlabController controller = new GitlabControllerImpl(args[0], true, true);
        try {
            // wait until the controller has initialized itself
            List<Project> projects = controller.getAllProjects();
            while (projects.isEmpty()) {
                Thread.sleep(1000);
                projects = controller.getAllProjects();
            }
            for (Project p : projects) {
                if (p.systemModel != null) {
                    Model result = query(p.systemModel, queryString);
                    if (result.size() > 0) {
                        System.out.println("System model of " + p.name + " gave result:");
                        printModel(result);
                    }
                }
                if (p.benchmarkModel != null) {
                    Model result = query(p.benchmarkModel, queryString);
                    if (result.size() > 0) {
                        System.out.println("Benchmark model of " + p.name + " gave result:");
                        printModel(result);
                    }
                }
            }
            System.out.println("Finished search.");
        } finally {
            controller.stopFetchingProjects();
        }
    }

    public static void printModel(Model model) {
        StmtIterator iterator = model.listStatements();
        while(iterator.hasNext()) {
            System.out.println(iterator.next().toString());
        }
    }

    public static Model query(Model model, String query) {
        QueryExecution qe = null;
        try {
            Dataset dataset = DatasetFactory.create(model);
            qe = QueryExecutionFactory.create(query, dataset);
            return qe.execConstruct();
        } catch (Exception e) {
            LOGGER.error("Exception while querying model.", e);
            return ModelFactory.createDefaultModel();
        } finally {
            if (qe != null) {
                qe.close();
            }
        }
    }
}
