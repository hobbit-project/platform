package de.usu.research.hobbit.gui.rest.beans;

public class KeycloakConfigBean {
	private String realm;
	private String url;
	private String clientId;

	@Override
	public String toString() {
		return "realm=" + realm + ", url=" + url + ", clientId=" + clientId;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

}
