package org.fogbowcloud.app.resource;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.PropertiesConstants;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

public class ResourceUtil {

	private static final Logger LOGGER = Logger
			.getLogger(ResourceUtil.class);
	
	public static User authenticateUser(JDFSchedulerApplication application,
			@SuppressWarnings("rawtypes") Series headers) throws IOException, GeneralSecurityException {
        String credentials = headers.getFirstValue(PropertiesConstants.X_CREDENTIALS);
        User user = application.authUser(credentials);
        if (user == null) {
        
        	throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
        }
        return user;
	}
}
