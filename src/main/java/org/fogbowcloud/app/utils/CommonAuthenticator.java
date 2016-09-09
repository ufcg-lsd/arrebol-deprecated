package org.fogbowcloud.app.utils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;
import java.util.Random;

import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.model.UserImpl;
import org.json.JSONException;
import org.json.JSONObject;

public class CommonAuthenticator implements ArrebolAuthenticator {

	Properties properties;
	
	public CommonAuthenticator(Properties properties) {
		this.properties = properties;
	}
	
	public static int getNonce() {
		return new Random().nextInt(999999);
	}

	public static boolean checkUserSignature(String hash, User user, int nonce)
			throws IOException, GeneralSecurityException {
		if (user == null) {
			return false;
		}
		// When using the arrebol CLI the generated base64 hash has some line
		// breaks
		// to be able to send it in the request we replace the new line char
		// with "*"
		// and here we need to replace it back with the new line char
		hash = hash.replace("*", "\n");
		RSAPublicKey publicKey = RSAUtils.getPublicKeyFromString(((UserImpl)user).getPublicKey());
		return RSAUtils.verify(publicKey, user.getUsername() + nonce, hash);

	}

	@Override
	public User authenticateUser(String credentials) {
		try {
			JSONObject cred = new JSONObject(credentials);

			User user = getUser(cred.getString("userJson"));

			String hash = cred.getString("password");

			int nonce = Integer.getInteger(cred.getString("nonce"));
			try {
				if (checkUserSignature(hash, user, nonce)) {
					return user;
				}
				return null;
			} catch (Exception e) {
				return null;
			}
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
	}

	private User getUser(String credentials) {
		JSONObject userInfo;
		try {
			userInfo = new JSONObject(credentials);
			return UserImpl.fromJSON(userInfo);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}