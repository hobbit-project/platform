package org.hobbit.controller.tools;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.controller.gitlab.GitlabController;
import org.hobbit.controller.gitlab.GitlabControllerImpl;
import org.hobbit.controller.gitlab.Project;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;

public class ProjectCounter {

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 1) {
            System.out.println(
                    "Error: wrong usage. The following parameters are necessary:\n <security-token>");
            return;
        }
        GitlabController controller = new GitlabControllerImpl(args[0], true, true);
        try {
            // wait until the controller has initialized itself
            List<Project> projects = controller.getAllProjects();
            while (projects.isEmpty()) {
                Thread.sleep(1000);
                projects = controller.getAllProjects();
            }
            Set<String> benchmarkUris = new HashSet<String>();
            Set<String> systemUris = new HashSet<String>();
            Set<String> systemInstanceUris = new HashSet<String>();
            for (Project p : projects) {
                if (p.benchmarkModel != null) {
                    for(Resource b : RdfHelper.getSubjectResources(p.benchmarkModel, RDF.type, HOBBIT.Benchmark)) {
                        benchmarkUris.add(b.getURI());
                    }
                }
                if (p.systemModel != null) {
                    for(Resource si : RdfHelper.getSubjectResources(p.systemModel, RDF.type, HOBBIT.SystemInstance)) {
                        systemInstanceUris.add(si.getURI());
                        Resource s = RdfHelper.getObjectResource(p.systemModel, si, HOBBIT.instanceOf);
                        if(s != null) {
                            systemUris.add(s.getURI());
                        } else {
                            systemUris.add(si.getURI());
                        }
                    }
                }
            }
            System.out.println("#Benchmarks:       " + benchmarkUris.size());
            System.out.println("#Systems:          " + systemUris.size());
            System.out.println("#System instances: " + systemInstanceUris.size());
        } finally {
            controller.stopFetchingProjects();
        }
    }
}
