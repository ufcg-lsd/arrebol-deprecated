package org.fogbowcloud.app.utils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;

import javax.crypto.Cipher;

import org.junit.Assert;
import org.junit.Test;

public class TestRSAUtilsAdditions {

	@Test
	public void TestEncryptDecrypt() throws IOException, GeneralSecurityException {
		String testString = "testString";
		
		KeyPair pair = RSAUtils.generateKeyPair();
		
		String encrypted = RSAUtils.encrypt(testString, pair.getPublic());
		
		String encrypted2 = RSAUtils.encrypt(testString, pair.getPublic());
		
		
		String decrypt = RSAUtils.decrypt(encrypted, pair.getPrivate());
		
		String decrypt2 = RSAUtils.decrypt(encrypted2, pair.getPrivate());
		
		String decrypt3 = RSAUtils.decrypt(encrypted, pair.getPrivate());
		
		Assert.assertEquals(decrypt, decrypt3);
		
		Assert.assertEquals(decrypt, decrypt2);
		
		Assert.assertEquals(testString, decrypt);
		
	}
}
