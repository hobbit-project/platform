package org.hobbit.controller.data;

import org.apache.jena.rdf.model.Model;
import org.hobbit.core.data.SystemMetaData;

/**
 * An extended metadata class for systems including the RDF model of the system.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ExtSystemMetaData extends SystemMetaData {

    public Model model;
}
