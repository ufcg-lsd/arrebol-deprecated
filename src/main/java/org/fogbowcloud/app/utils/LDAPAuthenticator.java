package org.fogbowcloud.app.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.LDAPUser;
import org.fogbowcloud.app.model.User;
import org.json.JSONException;
import org.json.JSONObject;

public class LDAPAuthenticator implements ArrebolAuthenticator {
	

	private static final String LDAP_AUTH = "ldapauth";
	public static final String CRED_AUTH_URL = "authUrl";
	public static final String CRED_LDAP_BASE = "base";
	public static final String CRED_LDAP_ENCRYPT = "encrypt";
	public static final String CRED_PRIVATE_KEY = "privateKey";
	public static final String CRED_PUBLIC_KEY = "publicKey";
	public static final String ENCRYPT_TYPE = ":TYPE:";
	public static final String ENCRYPT_PASS = ":PASS:";
	public static final String PASSWORD_ENCRYPTED = "{" + ENCRYPT_TYPE + "}" + ENCRYPT_PASS;
	public static final String ACCESSID_SEPARATOR = "!#!";

	private String ldapBase;
	private String encryptType;
	
	
	public static final String CRED_USERNAME = "username";
	public static final String CRED_PASSWORD = "password";

	private static final Logger LOGGER = Logger.getLogger(LDAPAuthenticator.class);
	
	private String ldapUrl;

	private Properties properties;
	

	public LDAPAuthenticator(Properties properties) {
		this.properties = properties;
		this.ldapUrl = properties.getProperty(AppPropertiesConstants.LDAP_AUTHENTICATION_URL);
		this.ldapBase = properties.getProperty(AppPropertiesConstants.LDAP_AUTHENTICATION_BASE);
	}

	@Override
	public User authenticateUser(String credentials){
		JSONObject cred;
		String username = "";
		String password = "";
		try {
			cred = new JSONObject(credentials);
			username = cred.getString(CRED_USERNAME);
			password = cred.getString(CRED_PASSWORD);
			LOGGER.debug("username: " + username +" password: "+ password);
			User user = new LDAPUser(ldapAuthenticate(username, password));
			return user;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
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
	
	
	protected String ldapAuthenticate(String uid, String password) throws Exception {

		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, ldapUrl);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");

		DirContext ctx = null;
		String name = null;
		try {

			password = encryptPassword(password);

			ctx = new InitialDirContext(env);

			//Search the directory to get User Name and Domain from UID
			String filter = "(&(objectClass=inetOrgPerson)(uid={0}))";
			SearchControls ctls = new SearchControls();
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			ctls.setReturningAttributes(new String[0]);
			ctls.setReturningObjFlag(true);
			NamingEnumeration enm = ctx.search(ldapBase, filter, new String[] { uid }, ctls);

			String dn = null;

			if (enm.hasMore()) {
				SearchResult result = (SearchResult) enm.next();
				dn = result.getNameInNamespace();
				name = extractUserName(result);
			}

			if (dn == null || enm.hasMore()) {
				// uid not found or not unique
				throw new NamingException("Authentication failed");
			}
			
			// Bind with found DN and given password
			ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
			ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
			// Perform a lookup in order to force a bind operation with JNDI
			ctx.lookup(dn);

			enm.close();

			return name;
			
		} catch (Exception e) {
			LOGGER.error("Error while authenticate " + uid +" - Error: "+e.getMessage());
 			throw e;
		} finally {
			ctx.close();
		}

	}

	private String encryptPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {

		if (encryptType == null || encryptType.isEmpty()) {
			return password;
		}

		MessageDigest algorithm = MessageDigest.getInstance(encryptType);
		byte messageDigest[] = algorithm.digest(password.getBytes("UTF-8"));

		StringBuilder hexString = new StringBuilder();
		for (byte b : messageDigest) {
			hexString.append(String.format("%02X", 0xFF & b));
		}

		return PASSWORD_ENCRYPTED.replaceAll(ENCRYPT_TYPE, encryptType).replaceAll(ENCRYPT_PASS, hexString.toString());

	}
	
	private String extractUserName(SearchResult result) {
		String nameGroup[] = result.getName().split(",");
		if(nameGroup != null && nameGroup.length > 0){
			String cnName[] = nameGroup[0].split("=");
			if(cnName != null && cnName.length > 1){
				return cnName[1];
			}
		}
		return null;
	}

	@Override
	public String getAuthenticatorName() {
		return LDAP_AUTH;
	}
	
}
