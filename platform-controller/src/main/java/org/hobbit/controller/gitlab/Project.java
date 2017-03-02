package org.hobbit.controller.gitlab;

/**
 * Simple structure containing the relevant meta data of a gitlab project that
 * contained at least one benchmark file or one system file.
 * 
 * 
 * Note that the {@link #benchmarkMetadata} or the {@link #systemMetadata} might
 * be <code>null</code> but never both at the same time.
 * 
 * @author Timofey Ermilov on 17/10/2016.
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 */
public class Project {
    /**
     * Content of the benchmark meta data file.
     */
    public String benchmarkMetadata;
    /**
     * Content of the system meta data file.
     */
    public String systemMetadata;
    /**
     * Name of the owner of the project in which the files have been found.
     */
    public String user;
    
    public String name;

    public Project(String benchmarkMetadata, String systemMetadata, String user, String name) {
        this.benchmarkMetadata = benchmarkMetadata;
        this.systemMetadata = systemMetadata;
        this.user = user;
        this.name = name;
    }
}
