package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SystemBean extends NamedEntityBean {
	public SystemBean() {
		super();
	}

	public SystemBean(String id, String name, String description) {
		super(id, name, description);
	}
}
