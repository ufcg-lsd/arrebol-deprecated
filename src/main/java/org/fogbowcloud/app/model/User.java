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

	public String getPrivatekey() {
		return publicKey;
	}

	public void setPublickey(String publickey) {
		this.publicKey = publickey;
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
