package org.fogbowcloud.app.resource;

import org.apache.commons.fileupload.FileUploadException;
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
import org.restlet.util.Series;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

public class JobResource extends BaseResource {

	//FIXME: it seems we can make it simpler

	private static final String COMPLETION = "completion";

	private static final Logger LOGGER = Logger.getLogger(JobResource.class);
	
	static final String JOB_LIST = "Jobs";
	private static final String JOB_TASKS = "Tasks";
	private static final String JOB_ID = "id";
	private static final String JOB_FRIENDLY = "name";
	private static final String STATE = "state";
	private static final String RETRIES = "retries";
	private static final String TASK_ID = "taskid";
	private static final String JOBPATH = "jobpath";
	public static final String FRIENDLY = "friendly";
	public static final String JDF_FILE_PATH = "jdffilepath";

	private JSONArray jobTasks = new JSONArray();

	@Get
	public Representation fetch() {
		LOGGER.info("Getting Jobs...");
		String jobId = (String) getRequest().getAttributes().get(JOBPATH);

		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		Series headers = (Series) getRequestAttributes().get("org.restlet.http.headers");

		User owner;
		try {
			owner = authenticateUser(application, headers);
		} catch (GeneralSecurityException e) {
			LOGGER.error("Error trying to authenticate", e);
			setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new StringRepresentation("There was an error trying to authenticate.\nTry again later.");
		} catch (IOException e) {
			LOGGER.error("Error trying to authenticate", e);
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation("Failed to read request header.");
		}
		if (owner == null) {
			LOGGER.error("Incorrect username/password.");
			setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new StringRepresentation("Incorrect username/password.");
		}

		JSONObject jsonJob = new JSONObject();
		JSONArray jobs = new JSONArray();
		// If no job id is passed, return list with all jobs for user
		if (jobId == null) {
			LOGGER.debug("Get all jobs");
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
		} else {
			LOGGER.debug("Get job " + jobId);
			JDFJob job = application.getJobById(jobId, owner.getUser());
			if (job == null) {
				job = application.getJobByName(jobId, owner.getUser());
				if (job == null) {
					LOGGER.debug("Could not find job with id " + jobId + " for user " + owner.getUsername());
					setStatus(Status.CLIENT_ERROR_NOT_FOUND);
					return new StringRepresentation("Could not find job with id '" + jobId + "'.");
				}
				jsonJob.put(JOB_FRIENDLY, jobId);
				jsonJob.put(JOB_ID, job.getId());
				jsonJob.put(STATE, job.getState());
                jsonJob.put(COMPLETION, job.completionPercentage());
            } else {
				jsonJob.put(JOB_ID, jobId);
				jsonJob.put(JOB_FRIENDLY, job.getName());
				jsonJob.put(STATE, job.getState());
                jsonJob.put(COMPLETION, job.completionPercentage());
            }
			LOGGER.debug("JobID " + jobId + " is of job " + job);

			for (Task task : job.getTasks()) {
				JSONObject jTask = new JSONObject();
				jTask.put(TASK_ID, task.getId());
				TaskState ts = application.getTaskState(task.getId());
				jTask.put(STATE, ts != null ? ts.getDesc().toUpperCase() : "UNDEFINED");
                int retries = application.getTaskRetries(task.getId(), owner.getUser());
                jTask.put(RETRIES, retries >= 0 ? task.getRetries() : "DIDN'T RUN");
                jobTasks.put(jTask);
			}
			jsonJob.put(JOB_TASKS, jobTasks);
			return new StringRepresentation(jsonJob.toString(), MediaType.TEXT_PLAIN);
		}
	}

	@Post
	public Representation addJob(Representation entity) {
		if (entity != null && !MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) {
			LOGGER.error("Form sent with incorrect format. " + entity.getMediaType().getName());
			setStatus(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
			return new StringRepresentation("Form sent with incorrect format.");
		}

		// Credentials
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put(JDF_FILE_PATH, null);
		fieldMap.put(ArrebolPropertiesConstants.X_CREDENTIALS, null);

		Map<String, File> fileMap = new HashMap<>();
		fileMap.put(JDF_FILE_PATH, null);

		try {
			LOGGER.debug("Collecting file from request");
			ServerResourceUtils.loadFields(entity, fieldMap, fileMap);
			File jdf = fileMap.get(JDF_FILE_PATH);
			if (jdf != null) {
				fieldMap.put(JDF_FILE_PATH, jdf.getAbsolutePath());
			}
		} catch (FileUploadException e) {
			LOGGER.error("Failed receiving file from client.", e);
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return new StringRepresentation("JDF upload failed.\nTry again later.");
		} catch (IOException e) {
			LOGGER.error("Failed reading JDF file.", e);
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return new StringRepresentation("Failed reading JDF file.\nTry again later.");
		}

		LOGGER.debug("Doing authentication to post job");
		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		Series headers = (Series) getRequestAttributes().get("org.restlet.http.headers");
		headers.add(ArrebolPropertiesConstants.X_CREDENTIALS, fieldMap.get(ArrebolPropertiesConstants.X_CREDENTIALS));

		User owner;
		try {
			owner = authenticateUser(application, headers);
		} catch (GeneralSecurityException e) {
			LOGGER.error("Error trying to authenticate", e);
			setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new StringRepresentation("There was an error trying to authenticate.\nTry again later.");
		} catch (IOException e) {
			LOGGER.error("Error trying to authenticate", e);
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation("Failed to read request header.");
		}
		if (owner == null) {
			LOGGER.error("Incorrect username/password.");
			setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new StringRepresentation("Incorrect username/password.");
		}

		// Creating job
		String jdfAbsolutePath = fieldMap.get(JDF_FILE_PATH);
		if (jdfAbsolutePath == null) {
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation("No JDF file submitted");
		}
		try {
			String jobId;
			LOGGER.debug("jdfpath <" + jdfAbsolutePath + ">");
			jobId = application.addJob(jdfAbsolutePath, owner);
			LOGGER.debug("Job "+ jobId + " created at time: "+ System.currentTimeMillis() );
			return new StringRepresentation(jobId, MediaType.TEXT_PLAIN);
		} catch (CompilerException ce) {
			LOGGER.error(ce.getMessage(), ce);
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation("Could not compile JDF file.");
		} catch (NameAlreadyInUseException | BlowoutException iae) {
			LOGGER.error(iae.getMessage(), iae);
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation(iae.getMessage());
		} catch (IOException e) {
			LOGGER.error("Could not read JDF file.", e);
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation("Could not read JDF file.");
		}
	}

	@Delete
	public Representation stopJob() {
		JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
		Series headers = (Series) getRequestAttributes().get("org.restlet.http.headers");
		User owner;
		try {
			owner = authenticateUser(application, headers);
		} catch (GeneralSecurityException e) {
			LOGGER.error("Error trying to authenticate", e);
			setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new StringRepresentation("There was an error trying to authenticate.\nTry again later.");
		} catch (IOException e) {
			LOGGER.error("Error trying to authenticate", e);
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation("Failed to read request header.");
		}
		if (owner == null) {
			LOGGER.error("Incorrect username/password.");
			setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new StringRepresentation("Incorrect username/password.");
		}

		String JDFString = (String) getRequest().getAttributes().get(JOBPATH);

		LOGGER.debug("Got JDF File: " + JDFString);

		String jobId = application.stopJob(JDFString, owner.getUser());

		if (jobId == null) {
			LOGGER.debug("Could not find job with id " + JDFString + " for user " + owner.getUsername());
			setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return new StringRepresentation("Could not find job with id '" + JDFString + "'.");
		}

		return new StringRepresentation(jobId, MediaType.TEXT_PLAIN);
	}
}
