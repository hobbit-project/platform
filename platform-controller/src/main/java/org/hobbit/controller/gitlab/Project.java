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
    
    public boolean isPrivate;

    public Project(String benchmarkMetadata, String systemMetadata, String user, String name, boolean isPrivate) {
        this.benchmarkMetadata = benchmarkMetadata;
        this.systemMetadata = systemMetadata;
        this.user = user;
        this.name = name;
        this.isPrivate = isPrivate;
    }
}
