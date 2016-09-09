package org.fogbowcloud.app.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.app.model.LDAPUser;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.manager.core.plugins.identity.ldap.LdapIdentityPlugin;
import org.fogbowcloud.manager.occi.model.Token;
import org.json.JSONException;
import org.json.JSONObject;

public class LDAPAuthenticator implements ArrebolAuthenticator {
	
	public static final String CRED_USERNAME = "username";
	public static final String CRED_PASSWORD = "password";

	private LdapIdentityPlugin identityPlugin;

	public LDAPAuthenticator(Properties properties) {
		identityPlugin = new LdapIdentityPlugin(properties);
	}

	@Override
	public User authenticateUser(String credentials) {
		Token token = identityPlugin.createToken(getMapFromCred(credentials));
		User user = new LDAPUser(token.getUser(), token.getAccessId());
		return user;
	}

	public Map<String, String> getMapFromCred(String credentials) {
		try {
			JSONObject cred = new JSONObject(credentials);
			Map<String, String> credentialsMap = new HashMap<String, String>();
			
			credentialsMap.put(CRED_PASSWORD, cred.getString(CRED_PASSWORD));
			credentialsMap.put(CRED_USERNAME, cred.getString(CRED_USERNAME));
			return credentialsMap;

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}

}
