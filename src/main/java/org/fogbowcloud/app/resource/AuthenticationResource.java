package org.fogbowcloud.app.resource;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

public class AuthenticationResource extends BaseResource {

	private static final Logger LOGGER = Logger
			.getLogger(AuthenticationResource.class);
	
	@Get
	public Representation getAuth() {
		JDFSchedulerApplication app = (JDFSchedulerApplication) getApplication();
		String authStrt =  app.getAuthenticatorName();
		LOGGER.debug("authStrat: " + authStrt);
		return new StringRepresentation(authStrt);
	}
	
}
