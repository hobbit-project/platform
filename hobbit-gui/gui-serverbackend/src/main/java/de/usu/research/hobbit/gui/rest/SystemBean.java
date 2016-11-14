package de.usu.research.hobbit.gui.rest;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SystemBean {
	public String id;
	public String name;
	public String description;
	
	public SystemBean() {		
	}
	
	public SystemBean(String id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
