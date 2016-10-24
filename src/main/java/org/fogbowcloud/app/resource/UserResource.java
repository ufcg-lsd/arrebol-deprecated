package org.fogbowcloud.app.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.ServerResourceUtils;
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
	public Representation createUser(Representation entity) 
			throws ResourceException, FileUploadException, IOException {
		JDFSchedulerApplication app = (JDFSchedulerApplication) getApplication();
		
		Map<String, String> fieldMap = new HashMap<String, String>();
    	fieldMap.put(REQUEST_ATTR_USERNAME, null);
    	Map<String, File> fileMap = new HashMap<String, File>();
    	fileMap.put(REQUEST_ATTR_PUBLICKEY, null);
    	
    	ServerResourceUtils.loadFields(entity, fieldMap, fileMap);
		checkMandatoryAttributes(fieldMap, fileMap);
		
		String username = fieldMap.get(REQUEST_ATTR_USERNAME);
		File publicKeyFile = fileMap.get(REQUEST_ATTR_PUBLICKEY);
		if (app.getUser(username) != null) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
		}
		String publicKey = IOUtils.toString(new FileInputStream(publicKeyFile));
		app.addUser(username, publicKey);
		setStatus(Status.SUCCESS_CREATED);
		return new StringRepresentation("OK");
	}

	private void checkMandatoryAttributes(Map<String, String> fieldMap,
			Map<String, File> fileMap) {
		String username = fieldMap.get(REQUEST_ATTR_USERNAME);
		File publicKey = fileMap.get(REQUEST_ATTR_PUBLICKEY);
		if (username == null || username.isEmpty() 
				|| publicKey == null || !publicKey.exists()) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
		}
	}
}
