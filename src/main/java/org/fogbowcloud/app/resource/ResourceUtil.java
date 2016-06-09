package org.fogbowcloud.app.resource;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.commons.httpclient.HttpStatus;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.AppPropertiesConstants;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

public class ResourceUtil {

	public static String authenticateUser(JDFSchedulerApplication application,
			@SuppressWarnings("rawtypes") Series headers) throws IOException, GeneralSecurityException {        
        String nonce = headers.getFirstValue(AppPropertiesConstants.X_AUTH_NONCE);
        String user = headers.getFirstValue(AppPropertiesConstants.X_AUTH_USER);
        String hash = headers.getFirstValue(AppPropertiesConstants.X_AUTH_HASH);
        
        if (!application.authUser(user, hash, nonce)) {
        	throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
        }
        return user;
	}
    
	public static String authenticateUserOnPost(
			JDFSchedulerApplication application, String nonce, String user,
			String hash) throws IOException, GeneralSecurityException {
    	if (!application.authUser(user, hash, nonce)) {
        	throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
        }
        return user;
    }
	
}
