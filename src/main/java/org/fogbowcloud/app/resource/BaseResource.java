package org.fogbowcloud.app.resource;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import java.io.IOException;
import java.security.GeneralSecurityException;

class BaseResource extends ServerResource {

    private static final Logger LOGGER = Logger.getLogger(BaseResource.class);

    User authenticateUser(JDFSchedulerApplication application, Series headers) {
        User owner;
        try {
            String credentials = headers.getFirstValue(ArrebolPropertiesConstants.X_CREDENTIALS);
            owner = application.authUser(credentials);
        } catch (GeneralSecurityException e) {
            LOGGER.error("Error trying to authenticate", e);
            throw new ResourceException(
                    Status.CLIENT_ERROR_UNAUTHORIZED,
                    "There was an error trying to authenticate.\nTry again later."
            );
        } catch (IOException e) {
            LOGGER.error("Error trying to authenticate", e);
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    "Failed to read request header."
            );
        }
        if (owner == null) {
            LOGGER.error("Authentication failed. Wrong username/password.");
            throw new ResourceException(
                    Status.CLIENT_ERROR_UNAUTHORIZED,
                    "Incorrect username/password."
            );
        }
        return owner;
    }
}
