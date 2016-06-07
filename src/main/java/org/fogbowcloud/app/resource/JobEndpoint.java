package org.fogbowcloud.app.resource;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.json.JSONArray;

public class JobEndpoint extends ServerResource {

	private static final Logger LOGGER = Logger.getLogger(JobEndpoint.class);

	@Get
	public Representation stopJob() throws IOException, NoSuchAlgorithmException {

		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();

        @SuppressWarnings("rawtypes")
		String owner = ResourceUtil.authenticateUser(application, (Series)getRequestAttributes()
				.get("org.restlet.http.headers"));
		
		JSONArray jobsJSONArray = jobsToJSONArray(application.getAllJobs(owner));
		LOGGER.debug("Info:" + jobsJSONArray.toString());

		return new StringRepresentation(jobsJSONArray.toString(), MediaType.TEXT_PLAIN);
	}

	protected JSONArray jobsToJSONArray(List<JDFJob> jobs) {
		JSONArray jArray = new JSONArray();

		for (JDFJob job : jobs) {
			JSONObject jobInfo = new JSONObject();
			JSONArray taskArray = new JSONArray();
			try {
				jobInfo.put("id", job.getId());
				jobInfo.put("text", job.getName());
				int taskNumber = 1;
				fillTasks(taskArray, job, taskNumber);
				jobInfo.put("tasks", taskArray);
				jArray.put(jobInfo);
			} catch (JSONException e) {
				LOGGER.error(e.getMessage());
			}
		}
		return jArray;
	}

	private void fillTasks(JSONArray jArray, JDFJob job, int taskNumber) throws JSONException {
		for (Task task : job.getByState(TaskState.READY)){
			JSONObject taskInfo = new JSONObject();
			taskInfo.put("id", task.getId());
			taskInfo.put("text", String.valueOf(taskNumber));
			taskInfo.put("state", "READY");
			jArray.put(taskInfo);
			taskNumber++;
		}
		for (Task task : job.getByState(TaskState.RUNNING)){
			JSONObject taskInfo = new JSONObject();
			taskInfo.put("id", task.getId());
			taskInfo.put("text", String.valueOf(taskNumber));
			taskInfo.put("state", "READY");
			jArray.put(taskInfo);
			taskNumber++;
		}
		for (Task task : job.getByState(TaskState.FAILED)){
			JSONObject taskInfo = new JSONObject();
			taskInfo.put("id", task.getId());
			taskInfo.put("text", String.valueOf(taskNumber));
			taskInfo.put("state", "READY");
			jArray.put(taskInfo);
			taskNumber++;
		}
		for (Task task : job.getByState(TaskState.COMPLETED)){
			JSONObject taskInfo = new JSONObject();
			taskInfo.put("id", task.getId());
			taskInfo.put("text", String.valueOf(taskNumber));
			taskInfo.put("state", "READY");
			jArray.put(taskInfo);
			taskNumber++;
		}
	}


}
