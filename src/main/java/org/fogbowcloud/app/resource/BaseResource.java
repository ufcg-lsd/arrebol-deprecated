package org.fogbowcloud.app.resource;

import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import java.io.IOException;
import java.security.GeneralSecurityException;

class BaseResource extends ServerResource {

    User authenticateUser(JDFSchedulerApplication application, Series headers) throws IOException, GeneralSecurityException {
        String credentials = headers.getFirstValue(ArrebolPropertiesConstants.X_CREDENTIALS);
        return application.authUser(credentials);
    }
}
