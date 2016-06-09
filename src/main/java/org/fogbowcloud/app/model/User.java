package org.fogbowcloud.app.model;

import org.json.JSONException;
import org.json.JSONObject;

public class User {

	private String username;
	private String privateKey;
	private String publicKey;
	private boolean active;

	public User(String username, String privateKey, String publicKey) {
		this.username = username;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.setActive(true);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject user = new JSONObject();
		user.put("username", this.username);
		user.put("privateKey", this.privateKey);
		user.put("publicKey", this.publicKey);
		return user;
	}

	public static User fromJSON(JSONObject userJSON) {
		return new User(userJSON.optString("username"),
				userJSON.optString("privateKey"), 
				userJSON.optString("publicKey"));
	}
}
