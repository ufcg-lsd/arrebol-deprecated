package org.fogbowcloud.app.model;

import org.json.JSONException;
import org.json.JSONObject;

public class User {

	private String username;
	private String publicKey;
	private boolean active;

	public User(String username, String publicKey) {
		this.username = username;
		this.publicKey = publicKey;
		this.setActive(true);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
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
		user.put("publicKey", this.publicKey);
		return user;
	}

	public static User fromJSON(JSONObject userJSON) {
		return new User(userJSON.optString("username"),
				userJSON.optString("publicKey"));
	}
}
