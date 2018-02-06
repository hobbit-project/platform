package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExtendedTaskRegistrationBean extends TaskRegistrationBean {

    private SystemBean system;

    public ExtendedTaskRegistrationBean() {
        super();
    }

    public ExtendedTaskRegistrationBean(String challengeId, String taskId, SystemBean system, boolean registered) {
        super(challengeId, taskId, system.getId(), registered);
        this.system = system;
    }

    /**
     * @return the system
     */
    public SystemBean getSystem() {
        return system;
    }

    /**
     * @param system
     *            the system to set
     */
    public void setSystem(SystemBean system) {
        this.system = system;
        super.setSystemId(system.getId());
    }

}
