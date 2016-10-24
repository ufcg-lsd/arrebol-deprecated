package org.fogbowcloud.app.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.scheduler.core.model.Task;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.amazonaws.util.json.JSONObject;

public class TaskResource4JDF extends ServerResource {
	private static final Logger LOGGER = Logger.getLogger(TaskResource4JDF.class);

	List<String> validVariables = new ArrayList<String>();

	@Get
	public Representation fetch() throws Exception {
		LOGGER.info("Getting tasks...");
		String taskId = (String) getRequest().getAttributes().get("taskId");
		LOGGER.debug("TaskId is " + taskId);
		// FIXME not being used
		String varName = (String) getRequest().getAttributes().get("varName");
		LOGGER.debug("varName is " + varName);

		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		
        @SuppressWarnings("rawtypes")
		User owner = ResourceUtil.authenticateUser(application, (Series)getRequestAttributes()
				.get("org.restlet.http.headers"));		
		
		Task task = application.getTaskById(taskId, owner.getUsername());
		LOGGER.debug("TaskId " + taskId + " is of task " + task);
		if (task == null) {
			throw new ResourceException(404, new Exception("Task id not found"));
		}

		JSONObject jsonTask = new JSONObject();

		jsonTask.put("metadata", task.getAllMetadata());
		jsonTask.put("state", application.getTaskState(taskId, owner.getUsername()).toString());
		return new StringRepresentation(jsonTask.toString(), MediaType.TEXT_PLAIN);
	}
}
