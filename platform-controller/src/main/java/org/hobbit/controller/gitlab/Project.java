package org.hobbit.controller.gitlab;

/**
 * Created by Timofey Ermilov on 17/10/2016.
 */
public class Project {
    public String benchmarkMetadata;
    public String systemMetadata;
    public String user;

    public Project(String benchmarkMetadata, String systemMetadata, String user) {
        this.benchmarkMetadata = benchmarkMetadata;
        this.systemMetadata = systemMetadata;
        this.user = user;
    }
}
