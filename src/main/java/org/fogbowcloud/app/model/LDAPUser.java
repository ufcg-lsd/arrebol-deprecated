package org.fogbowcloud.app.model;

public class LDAPUser implements User{

	private String user;
	private String UUID;
	
	public void setUser(String user) {
		this.user = user;
	}

	public String getUUID() {
		return UUID;
	}

	public void setUUID(String uUID) {
		UUID = uUID;
	}

	public LDAPUser(String user, String UUID) {
		this.user = user;
		this.UUID = UUID;
	}
	
	@Override
	public String getUsername() {
		return this.user;
	}

}
