package org.fogbowcloud.app.utils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.Random;

import org.fogbowcloud.app.model.User;

public class AuthUtils {

	public static int getNonce() {
		return new Random().nextInt(999999);
	}

	public static boolean checkUserSignature(String hash, User user, int nonce)
			throws IOException, GeneralSecurityException {
		if (user == null) {
			return false;
		}
		// When using the arrebol CLI the generated base64 hash has some line breaks
		// to be able to send it in the request we replace the new line char with "*"
		// and here we need to replace it back with the new line char
		hash = hash.replace("*", "\n");
		RSAPublicKey publicKey = RSAUtils
				.getPublicKeyFromString(user.getPublicKey());
		return RSAUtils.verify(publicKey, user.getUsername() + nonce, hash);

	}
}