package org.fogbowcloud.app.resource;

import org.apache.http.HttpStatus;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
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
		String publicKey = form.getFirstValue(REQUEST_ATTR_PUBLICKEY);
		
		if (app.getUser(username) != null) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
		}
		
		app.addUser(username, publicKey);
		setStatus(Status.SUCCESS_CREATED);
		return new StringRepresentation("OK");
	}

	private void checkMandatoryAttributes(Form form) {
		String username = form.getFirstValue(REQUEST_ATTR_USERNAME);
		String publicKey = form.getFirstValue(REQUEST_ATTR_PUBLICKEY);
		if (username == null || username.isEmpty() 
				|| publicKey == null || publicKey.isEmpty()) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
		}
	}
	
}
