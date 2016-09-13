package org.fogbowcloud.app.utils;

import org.fogbowcloud.app.model.User;

public interface ArrebolAuthenticator {
	
	public User authenticateUser(String credentials);

	public String getAuthenticatorName();
	
}
