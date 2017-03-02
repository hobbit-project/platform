package de.usu.research.hobbit.gui.rest.beans;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SubmitResponseBean {
	private String id;
	private Date timestamp;
	
	public SubmitResponseBean() {		
	}

	public SubmitResponseBean(String id) {
		this.id = id;
		this.timestamp = new Date();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}
