package org.hobbit.controller.tools;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;
import org.hobbit.controller.gitlab.GitlabController;
import org.hobbit.controller.gitlab.GitlabControllerImpl;
import org.hobbit.controller.gitlab.Project;

/**
 * A simple command line tool that searches for the given term(s) in all
 * available benchmark and system definitions.
 * 
 * arguments: <security-token> <search-term(s)>
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class ProjectSearcher {

    public static void main(String[] args) throws InterruptedException {
        String searchString = null;
        if (args.length < 2) {
            System.out.println(
                    "Error: wrong usage. The following parameters are necessary:\n <security-token> <search-term(s)>");
            return;
        } else if (args.length == 2) {
            searchString = args[1];
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
            searchString = builder.toString();
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
                if ((p.systemModel != null) && containsLiteralWithString(p.systemModel, searchString)) {
                    System.out.println("System model of " + p.name + " contains given string.");
                }
                if ((p.benchmarkModel != null) && containsLiteralWithString(p.benchmarkModel, searchString)) {
                    System.out.println("Benchmark model of " + p.name + " contains given string.");
                }
            }
            System.out.println("Finished search.");
        } finally {
            controller.stopFetchingProjects();
        }
    }

    public static boolean containsLiteralWithString(Model model, String searchString) {
        StmtIterator iterator = model.listStatements();
        RDFNode object;
        while (iterator.hasNext()) {
            object = iterator.next().getObject();
            if (object.isLiteral()) {
                return object.asLiteral().getString().contains(searchString);
            }
        }
        return false;
    }
}
