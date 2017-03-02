package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NamedEntityBean {
	/**
	 * The challenge task id or in OWL speak URI
	 */
	protected String id;

	/**
	 * The challenge task name/ rdfs:label
	 */
	protected String name;

	/**
	 * The description of the challenge task / rdfs:comment
	 */
	protected String description;

	public NamedEntityBean() {
	}

	public NamedEntityBean(String id, String name, String description) {
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
	
	@Override
	public String toString() {
		return getClass().getName() + ",id=" + id + ",name=" + name;
	}

    @Override
    public int hashCode() {
        return ((id == null) ? 0 : id.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NamedEntityBean other = (NamedEntityBean) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
