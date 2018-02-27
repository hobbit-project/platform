/**
 * This file is part of gui-serverbackend.
 *
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlRootElement
public class TaskRegistrationBean {
    private String challengeId;
    private String taskId;
    private String systemId;
    private boolean registered = false;

    public TaskRegistrationBean() {
    }

    /**
     * Constructor.
     * 
     * @param challengeId
     *            URI of the challenge
     * @param taskId
     *            URI of the challenge task
     * @param systemId
     *            URI of the registered system
     * @deprecated Use
     *             {@link TaskRegistrationBean#TaskRegistrationBean(String, String, String, boolean)}
     *             instead with an explicit assignment of the flag whether the
     *             system is registered or not.
     */
    @Deprecated
    public TaskRegistrationBean(String challengeId, String taskId, String systemId) {
        this(challengeId, taskId, systemId, false);
    }

    public TaskRegistrationBean(String challengeId, String taskId, String systemId, boolean registered) {
        this.challengeId = challengeId;
        this.taskId = taskId;
        this.systemId = systemId;
        this.registered = registered;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * @return the registered
     */
    public boolean isRegistered() {
        return registered;
    }

    /**
     * @param registered the registered to set
     */
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((challengeId == null) ? 0 : challengeId.hashCode());
        result = prime * result + ((systemId == null) ? 0 : systemId.hashCode());
        result = prime * result + ((taskId == null) ? 0 : taskId.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TaskRegistrationBean other = (TaskRegistrationBean) obj;
        if (challengeId == null) {
            if (other.challengeId != null)
                return false;
        } else if (!challengeId.equals(other.challengeId))
            return false;
        if (systemId == null) {
            if (other.systemId != null)
                return false;
        } else if (!systemId.equals(other.systemId))
            return false;
        if (taskId == null) {
            if (other.taskId != null)
                return false;
        } else if (!taskId.equals(other.taskId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
