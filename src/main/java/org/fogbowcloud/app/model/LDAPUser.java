package org.fogbowcloud.app.model;

public class LDAPUser implements User{

	private String user;
	//FIXME: do we need it?
	private String UUID;
	
	public LDAPUser(String user) {
		this.user = user;
	}
	
	@Override
	public String getUsername() {
		return this.user;
	}

	@Override
	public String getUUID() {
		return this.UUID;
	}

}
