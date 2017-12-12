package org.fogbowcloud.app.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.app.utils.ServerResourceUtils;
import org.fogbowcloud.app.utils.authenticator.LDAPAuthenticator;
import org.restlet.Application;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

public class UserResource extends BaseResource {

	private static final Logger LOGGER = Logger.getLogger(UserResource.class);

	private static final String REQUEST_ATTR_USERNAME = "username";
	private static final String REQUEST_ATTR_PUBLICKEY = "publicKey";

	@Post
	public Representation authenticate(Representation entity) {
		Form data = new Form(entity);
		String creds = data.getFirstValue(ArrebolPropertiesConstants.X_CREDENTIALS);

		JDFSchedulerApplication app = (JDFSchedulerApplication) getApplication();
        Series headers = (Series) getRequestAttributes().get("org.restlet.http.headers");
		headers.add(ArrebolPropertiesConstants.X_CREDENTIALS, creds);
        authenticateUser(app, headers);

	    return new StringRepresentation("Authenticated successfully");
	}
	
	@Put
	public Representation createUser(Representation entity) {
		JDFSchedulerApplication app = (JDFSchedulerApplication) getApplication();
		
		Map<String, String> fieldMap = new HashMap<>();
    	fieldMap.put(REQUEST_ATTR_USERNAME, null);
    	Map<String, File> fileMap = new HashMap<>();
    	fileMap.put(REQUEST_ATTR_PUBLICKEY, null);
    	try {
			ServerResourceUtils.loadFields(entity, fieldMap, fileMap);
		} catch (FileUploadException | IOException e) {
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Failed to read public key.");
		}
		checkMandatoryAttributes(fieldMap, fileMap);
		
		String username = fieldMap.get(REQUEST_ATTR_USERNAME);
		File publicKeyFile = fileMap.get(REQUEST_ATTR_PUBLICKEY);

		User user = null;
		try {
			user = app.getUser(username);
		} catch (RuntimeException e) {
			if (app.getAuthenticatorName().equals(LDAPAuthenticator.AUTH_NAME)) {
				throw new ResourceException(
						Status.SERVER_ERROR_NOT_IMPLEMENTED,
						"Authenticator does not allow creating users. Talk with an administrator."
				);
			}
		}
		if (user != null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "User already exists.");
		}
		
		String publicKey;
		try {
			publicKey = IOUtils.toString(new FileInputStream(publicKeyFile));
		} catch (IOException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to read public key.");
		}
		try {
			app.addUser(username, publicKey);
		} catch (RuntimeException e) {
			if (app.getAuthenticatorName().equals(LDAPAuthenticator.AUTH_NAME)) {
				throw new ResourceException(
						Status.SERVER_ERROR_NOT_IMPLEMENTED,
						"Authenticator does not allow creating users. Talking with an administrator."
				);
			}
		}
		setStatus(Status.SUCCESS_CREATED);
		return new StringRepresentation("OK");
	}

	private void checkMandatoryAttributes(Map<String, String> fieldMap,
			Map<String, File> fileMap) {
		String username = fieldMap.get(REQUEST_ATTR_USERNAME);
		File publicKey = fileMap.get(REQUEST_ATTR_PUBLICKEY);
		if (username == null || username.isEmpty() 
				|| publicKey == null || !publicKey.exists()) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}
	}
}
