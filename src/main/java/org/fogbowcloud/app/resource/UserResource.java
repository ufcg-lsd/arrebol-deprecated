package org.fogbowcloud.app.resource;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

import org.apache.http.HttpStatus;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.RSAUtils;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class UserResource extends ServerResource {
	private static final String REQUEST_ATTR_USERNAME = "username";
	private static final String REQUEST_ATTR_PUBLICKEY = "publicKey";
	
	@Post
	public Representation createUser(Representation entity) throws ResourceException {
		Form form = new Form(entity);
		JDFSchedulerApplication app = (JDFSchedulerApplication) getApplication();
		checkMandatoryAttributes(form);
		
		String username = form.getFirstValue(REQUEST_ATTR_USERNAME);
		if (app.getUser(username) != null) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
		}
		KeyPair keyPair;
		try {
			keyPair = RSAUtils.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new ResourceException(HttpStatus.SC_INTERNAL_SERVER_ERROR, 
					"Internal Server Error", "Could not create the user's key pair.", "");
		}
		
		User user = app.addUser(username, keyPair);
		setStatus(Status.SUCCESS_CREATED);
		RSAPublicKey publicKey = null;
		String pemPublicKey = null;
		try {
			publicKey = RSAUtils.getPublicKeyFromString(user.getPublicKey());
			pemPublicKey = RSAUtils.savePublicKeyInPEMFormat(publicKey);
		} catch (Exception e) {
			//TODO catches an exception, need to remove the user
		}
		return new StringRepresentation(pemPublicKey);
	}

	private void checkMandatoryAttributes(Form form) {
		String username = form.getFirstValue(REQUEST_ATTR_USERNAME);
		if (username == null || username.isEmpty()) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
		}
	}
	
}
