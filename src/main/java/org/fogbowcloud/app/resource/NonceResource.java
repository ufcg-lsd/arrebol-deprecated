package org.fogbowcloud.app.resource;

import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

public class NonceResource extends BaseResource {

	@Get
	public Representation getNonce() {
		JDFSchedulerApplication app = (JDFSchedulerApplication) getApplication();
		int nonce = app.getNonce();
		return new StringRepresentation(String.valueOf(nonce));
	}
	
}