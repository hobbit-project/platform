package org.hobbit.controller.gitlab;

import java.util.List;

/**
 * Created by Timofey Ermilov on 17/10/2016.
 */
public interface GitlabController {
    public List<Project> getAllProjects();

    public void stopFetchingProjects();
}
