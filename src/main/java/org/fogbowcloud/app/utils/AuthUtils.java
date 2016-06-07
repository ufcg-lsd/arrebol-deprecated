package org.fogbowcloud.app.utils;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class AuthUtils {

	public static int getNonce(){
		return new Random().nextInt(999999);
	}
	
	public static boolean checkHash(String hash,String pass, int nonce) throws NoSuchAlgorithmException, IOException {
		return hash.equals(hashMethod(pass, nonce));
	}
	
	private static String hashMethod(String pass, int nonce) throws NoSuchAlgorithmException, IOException {
		String toEnc = pass + nonce;
		MessageDigest mdEnc = MessageDigest.getInstance("MD5"); 
		mdEnc.update(toEnc.getBytes(), 0, toEnc.length());
		String md5 = new BigInteger(1, mdEnc.digest()).toString(16); // Hash value
		return md5;
	}
}