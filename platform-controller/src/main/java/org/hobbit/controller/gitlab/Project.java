/**
 * This file is part of platform-controller.
 *
 * platform-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * platform-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with platform-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.controller.gitlab;

import java.util.Date;

import org.apache.jena.rdf.model.Model;

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
    public Model benchmarkModel;
    /**
     * Content of the system meta data file.
     */
    public Model systemModel;
    /**
     * Name of the owner of the project in which the files have been found.
     */
    @Deprecated
    public String user;
    /**
     * Name of the owner of this project
     */
    public String name;
    /**
     * creation date of the project
     */
    public Date createdAt;
    /**
     * Flag showing whether the project is private.
     */
    public boolean isPrivate;
    /**
     * Commit ID of the default branch of this project.
     */
    public String commitId;

    public Project(Model benchmarkModel, Model systemModel, String user, String name, Date createdAt,
                   boolean isPrivate, String commitId) {
        this.benchmarkModel = benchmarkModel;
        this.systemModel = systemModel;
        this.user = user;
        this.name = name;
        this.createdAt = createdAt;
        this.isPrivate = isPrivate;
        this.commitId = commitId;
    }

    /**
     * @return the benchmarkModel
     */
    public Model getBenchmarkModel() {
        return benchmarkModel;
    }

    /**
     * @param benchmarkModel the benchmarkModel to set
     */
    public void setBenchmarkModel(Model benchmarkModel) {
        this.benchmarkModel = benchmarkModel;
    }

    /**
     * @return the systemModel
     */
    public Model getSystemModel() {
        return systemModel;
    }

    /**
     * @param systemModel the systemModel to set
     */
    public void setSystemModel(Model systemModel) {
        this.systemModel = systemModel;
    }

    /**
     * @return the user
     */
    @Deprecated
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    @Deprecated
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the createdAt
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt the createdAt to set
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * @return the isPrivate
     */
    public boolean isPrivate() {
        return isPrivate;
    }

    /**
     * @param isPrivate the isPrivate to set
     */
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    /**
     * @return the commit ID
     */
    public String getCommitId() {
        return commitId;
    }

    /**
     * @param commitId the commit ID to set
     */
    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }
}
