package org.fogbowcloud.app.resource;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.commons.httpclient.HttpStatus;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.AppPropertiesConstants;
import org.json.JSONObject;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

public class ResourceUtil {

	public static String authenticateUser(JDFSchedulerApplication application,
			@SuppressWarnings("rawtypes") Series headers) throws IOException, GeneralSecurityException {
        String credentials = headers.getFirstValue(AppPropertiesConstants.X_CREDENTIALS);
        User user = application.authUser(credentials);
        if (user == null) {
        
        	throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
        }
        return user.getUsername();
	}
    
	public static String authenticateUserOnPost(
			JDFSchedulerApplication application, String nonce, String user,
			String hash) throws IOException, GeneralSecurityException {
//    	if (!application.authUser(user, hash, nonce)) {
//        	throw new ResourceException(HttpStatus.SC_UNAUTHORIZED);
//        }
        return user;
    }
	
}
