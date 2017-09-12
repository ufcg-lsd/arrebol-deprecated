package org.fogbowcloud.app.resource;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.app.NameAlreadyInUseException;
import org.fogbowcloud.app.jdfcompiler.main.CompilerException;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.app.utils.ServerResourceUtils;
import org.fogbowcloud.blowout.core.exception.BlowoutException;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

public class JobResource extends ServerResource {

	//FIXME: it seems we can make it simpler

	private static final Logger LOGGER = Logger.getLogger(JobResource.class);
	
	public static final String JOB_LIST = "Jobs";
	private static final String JOB_TASKS = "Tasks";
	private static final String JOB_ID = "id";
	private static final String JOB_FRIENDLY = "name";
	private static final String STATE = "state";
	private static final String TASK_ID = "taskid";
	private static final String JOBPATH = "jobpath";
	public static final String FRIENDLY = "friendly";
	public static final String SCHED_PATH = "schedpath";
	public static final String JDF_FILE_PATH = "jdffilepath";

	private JSONArray jobTasks = new JSONArray();

	@Get
	public Representation fetch() {
		LOGGER.info("Getting Jobs...");
		String jobId = (String) getRequest().getAttributes().get(JOBPATH);
		LOGGER.debug("JobId is " + jobId);
		
		
		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		JSONObject jsonJob = new JSONObject();

		JSONArray jobs = new JSONArray();

		User owner;
		try {
			owner = ResourceUtil.authenticateUser(
					application,
					(Series) getRequestAttributes().get("org.restlet.http.headers")
			);
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

		if (jobId == null) {
			for (JDFJob job : application.getAllJobs(owner.getUser())) {
				JSONObject jJob = new JSONObject();
				if (job.getName() != null) {
					jJob.put("id", job.getId());
					jJob.put("name", job.getName());

				} else {
					jJob.put("id: ", job.getId());
				}
				jobs.put(jJob);
			}

			jsonJob.put(JOB_LIST, jobs);

			LOGGER.debug("My info Is: " + jsonJob.toString());

			return new StringRepresentation(jsonJob.toString(), MediaType.TEXT_PLAIN);
		}

		JDFJob job = application.getJobById(jobId, owner.getUser());
		if (job == null) {
			job = application.getJobByName(jobId, owner.getUser());
			if (job == null) {
				throw new ResourceException(404);
			}
			jsonJob.put(JOB_FRIENDLY, jobId);
			jsonJob.put(JOB_ID, job.getId());
		} else {
			jsonJob.put(JOB_ID, jobId);
			jsonJob.put(JOB_FRIENDLY, job.getName());
		}
		LOGGER.debug("JobID " + jobId + " is of job " + job);

		for (Task task : job.getTasks()) {
			JSONObject jTask = new JSONObject();
			jTask.put(TASK_ID, task.getId());
			TaskState ts = application.getTaskState(task.getId(), owner.getUser());
			jTask.put(STATE, ts != null ? ts.getDesc().toUpperCase() : "UNDEFINED");
			jobTasks.put(jTask);
		}
		jsonJob.put(JOB_TASKS, jobTasks);
		return new StringRepresentation(jsonJob.toString(), MediaType.TEXT_PLAIN);
	}

	@Post
	public StringRepresentation addJob(Representation entity) {
		if (entity != null && !MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) {
			throw new ResourceException(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
		}


		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		Series headers = (Series) getRequestAttributes().get("org.restlet.http.headers");
		User owner;
		try {
			owner = ResourceUtil.authenticateUser(
					application,
					headers
			);
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

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put(JDF_FILE_PATH, null);
		fieldMap.put(ArrebolPropertiesConstants.X_CREDENTIALS, null);
		fieldMap.put(SCHED_PATH, null);

		try {
			ServerResourceUtils.loadFields(entity, fieldMap, new HashMap<String, File>());
		} catch (FileUploadException e) {
			LOGGER.error("Failed receiving file from client.", e);
			throw new ResourceException(
					Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,
					"JDF upload failed.\nTry again later."
			);
		} catch (IOException e) {
			LOGGER.error("Failed reading JDF file.", e);
			throw new ResourceException(
					Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,
					"Failed reading JDF file.\nTry again later."
			);
		}

		String jdf = fieldMap.get(JDF_FILE_PATH);
		if (jdf == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}
		String schedPath = fieldMap.get(SCHED_PATH);

		String jdfAbsolutePath = fieldMap.get(JDF_FILE_PATH);
		try {
			String jobId;
			LOGGER.debug("jdfpath <" + jdfAbsolutePath + ">" + " schedPath <" + schedPath + ">");
			jobId = application.addJob(jdfAbsolutePath, schedPath, owner);
			return new StringRepresentation(jobId, MediaType.TEXT_PLAIN);
		} catch (CompilerException ce) {
			LOGGER.error(ce.getMessage(), ce);
			throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"Could not compile JDF file.",
					ce
			);
		} catch (NameAlreadyInUseException | BlowoutException iae) {
			LOGGER.error(iae.getMessage(), iae);
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, iae.getMessage());
		} catch (IOException e) {
			LOGGER.error("Could not read JDF file.", e);
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Could not read JDF file.");
		}
	}

	@Delete
	public StringRepresentation stopJob() {
		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		Series headers = (Series) getRequestAttributes().get("org.restlet.http.headers");
		User owner;
		try {
			owner = ResourceUtil.authenticateUser(
					application,
					headers
			);
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

		String JDFString = (String) getRequest().getAttributes().get(JOBPATH);

		LOGGER.debug("Got JDF File: " + JDFString);

		String jobId = application.stopJob(JDFString, owner.getUser());

		if (jobId == null) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Job not found.");
		}

		return new StringRepresentation(jobId, MediaType.TEXT_PLAIN);
	}
}
