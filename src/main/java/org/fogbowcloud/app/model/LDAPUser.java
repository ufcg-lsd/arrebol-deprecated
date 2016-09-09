package org.fogbowcloud.app.model;

public class LDAPUser implements User{

	private String user;
	
	public void setUser(String user) {
		this.user = user;
	}

	public LDAPUser(String user) {
		this.user = user;
	}
	
	@Override
	public String getUsername() {
		return this.user;
	}

}
