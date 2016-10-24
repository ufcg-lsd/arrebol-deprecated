package org.fogbowcloud.app.utils.authenticator;

import org.fogbowcloud.app.model.User;

public interface ArrebolAuthenticator {
	
	public User authenticateUser(Credential credential);
	public User addUser(String username, String password);
	public User getUserByUsername(String username);
	public String getAuthenticatorName();
	
}
