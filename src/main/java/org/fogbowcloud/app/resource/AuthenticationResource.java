package org.fogbowcloud.app.resource;

import java.io.IOException;

import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class AuthenticationResource extends ServerResource {

	@Get
	public Representation getNOnce() throws IOException {
		JDFSchedulerApplication app = (JDFSchedulerApplication) getApplication();
		int nonce = app.getNonce();
		return new StringRepresentation(String.valueOf(nonce));
	}
	
}