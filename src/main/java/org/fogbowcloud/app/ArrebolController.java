package org.fogbowcloud.app;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.datastore.JobDataStore;
import org.fogbowcloud.app.jdfcompiler.main.CompilerException;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.JDFTasks;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.app.utils.PropertiesConstants;
import org.fogbowcloud.app.utils.authenticator.ArrebolAuthenticator;
import org.fogbowcloud.app.utils.authenticator.Credential;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.exception.BlowoutException;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.blowout.core.util.AppPropertiesConstants;
import org.fogbowcloud.blowout.core.util.ManagerTimer;
import org.json.JSONException;
import org.json.JSONObject;

public class ArrebolController {

	private static final int DEFAULT_EXECUTION_MONITOR_INTERVAL = 30000;
	private static final int CHECKPOINT_INTERVAL = 30000;

	private static final Logger LOGGER = Logger.getLogger(ArrebolController.class);

	private BlowoutController blowoutController;
	private Properties properties;
	private List<Integer> nonces;
	private HashMap<String, Task> finishedTasks;
	private JobDataStore jobDataStore;
	private ArrebolAuthenticator auth;
	private ExecutionMonitorWithDB executionMonitor;

	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer checkPointTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));

	public ArrebolController(Properties properties) {
		if (properties == null) {
			throw new IllegalArgumentException("Properties cannot be null");
		}
		this.finishedTasks = new HashMap<String, Task>();
		this.properties = properties;
	}

	public Properties getProperties() {
		return properties;
	}
	
	public void init() throws Exception {

		// FIXME: add as constructor param?
		this.auth = createAuthenticatorPluginInstance();
		// FIXME: replace by a proper
		this.jobDataStore = new JobDataStore(properties.getProperty(AppPropertiesConstants.DB_DATASTORE_URL));

		Boolean removePreviousResources = new Boolean(
				this.properties.getProperty(PropertiesConstants.REMOVE_PREVIOUS_RESOURCES)).booleanValue();

		LOGGER.debug("Properties: " + properties.getProperty(PropertiesConstants.DEFAULT_SPECS_FILE_PATH));

		// this.scheduler = new Scheduler(infraManager, );

		// legacyJobs.toArray(new JDFJob[legacyJobs.size()])

		this.blowoutController = new BlowoutController(this.properties);
		blowoutController.start(removePreviousResources);

		
		LOGGER.debug(
				"Application to be started on port: " + properties.getProperty(PropertiesConstants.REST_SERVER_PORT));
		LOGGER.info("Properties: " + properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH));

		this.nonces = new ArrayList<Integer>();

		LOGGER.debug("Starting Scheduler and Execution Monitor, execution monitor period: "
				+ properties.getProperty(PropertiesConstants.EXECUTION_MONITOR_PERIOD));
		executionMonitor = new ExecutionMonitorWithDB(blowoutController, this, jobDataStore);
		executionMonitorTimer.scheduleAtFixedRate(executionMonitor, 0, DEFAULT_EXECUTION_MONITOR_INTERVAL);
		int schedulerPeriod = Integer.valueOf(properties.getProperty(PropertiesConstants.EXECUTION_MONITOR_PERIOD));
		
		restartAllJobs();
		LOGGER.info("Starting scheduler with period: " + schedulerPeriod);

	}


	private void restartAllJobs() throws BlowoutException {
		for (JDFJob job : this.jobDataStore.getAll()) {
			ArrayList<Task> taskList = new ArrayList<Task>();
			for (Task task : job.getTasks()) {
				if (!task.isFinished()) {
					taskList.add(task);
				}
			}
			blowoutController.addTaskList(taskList);
		}
	}

	private ArrebolAuthenticator createAuthenticatorPluginInstance() throws Exception {
		String providerClassName = this.properties.getProperty(PropertiesConstants.AUTHENTICATION_PLUGIN);
		Class<?> forName = Class.forName(providerClassName);
		Object clazz = forName.getConstructor(Properties.class).newInstance(this.properties);
		if (!(clazz instanceof ArrebolAuthenticator)) {
			throw new Exception("Authenticator Class Name is not a ArrebolAuthenticator implementation");
		}

		return (ArrebolAuthenticator) clazz;
	}

	public JDFJob getJobById(String jobId, String owner) {
		JDFJob jdfJob = (JDFJob) this.jobDataStore.getByJobId(jobId, owner);
			return jdfJob;
	}

	public String addJob(String jdfFilePath, String schedPath, User owner)
			throws CompilerException, NameAlreadyInUseException, BlowoutException {
		JDFJob tmpJob = new JDFJob(schedPath, owner.getUsername(), new ArrayList<Task>());
		tmpJob.setUUID(owner.getUUID());
		List<Task> taskList = getTasksFromJDFFile(jdfFilePath, tmpJob);
		JDFJob job = new JDFJob(tmpJob.getId(), tmpJob.getSchedPath(), tmpJob.getOwner(), taskList);
		job.setUUID(tmpJob.getUUID());
		job.setFriendlyName(tmpJob.getName());

		if (job.getName() != null && !job.getName().trim().isEmpty()
				&& getJobByName(job.getName(), owner.getUsername()) != null) {

			throw new NameAlreadyInUseException(
					"The name " + job.getName() + " is already in use for the user " + owner);
		}

		LOGGER.debug("Adding job " + job.getName() + " to scheduler");


		blowoutController.addTaskList(job.getTasks());
		jobDataStore.insert(job);
		return job.getId();
	}

	public ArrayList<JDFJob> getAllJobs(String owner) {
		
		return (ArrayList<JDFJob>) this.jobDataStore.getAllByOwner(owner);
	}

	public void updateJob(JDFJob job) {
		this.jobDataStore.update(job);
	}

	public String stopJob(String jobReference, String owner) {
		JDFJob jobToRemove = getJobByName(jobReference, owner);
		if (jobToRemove != null) {
			this.jobDataStore.deleteByJobId(jobToRemove.getId(), owner);
			for (Task task : jobToRemove.getTasks()) {
				blowoutController.cleanTask(task);
			}
			return jobToRemove.getId();
		} else {
			jobToRemove = getJobById(jobReference, owner);
			if (jobToRemove != null) {
				this.jobDataStore.deleteByJobId(jobToRemove.getId(), owner);
				for (Task task : jobToRemove.getTasks()) {
					blowoutController.cleanTask(task);
				}
				return jobToRemove.getId();
			}
		}
		return null;
	}

	public JDFJob getJobByName(String jobName, String owner) {
		if (jobName == null) {
			return null;
		}
		for (JDFJob job : this.jobDataStore.getAllByOwner(owner)) {
			JDFJob jdfJob = (JDFJob) job;
			// TODO review this IFs
				if (jobName.equals(((JDFJob) job).getName())) {
					return (JDFJob) job;
				}
		}
		return null;
	}

	public Task getTaskById(String taskId, String owner) {
		for (JDFJob job : getAllJobs(owner)) {
			JDFJob jdfJob = (JDFJob) job;
			Task task = jdfJob.getTaskById(taskId);
			if (task != null) {
				return task;
			}
		}
		return null;
	}

	public TaskState getTaskState(String taskId, String owner) {

		Task task = finishedTasks.get(taskId);
		if (task != null) {
			return TaskState.COMPLETED;
		} else {
			task = getTaskById(taskId, owner);
			if (task != null) {
				TaskState taskState = blowoutController.getTaskState(task.getId());
				if (taskState != null) {
					return taskState;
				}
			}
			return null;
		}
	}

	public void moveTaskToFinished(Task task) {
			JDFJob job = this.jobDataStore.getByJobId(task.getMetadata(ArrebolPropertiesConstants.JOB_ID), 
					task.getMetadata(ArrebolPropertiesConstants.OWNER));
			job.finish(task);
			updateJob(job);
	}

	public User authUser(String credentials) throws IOException, GeneralSecurityException {
		if (credentials == null) {
			return null;
		}

		Credential credential = null;
		try {
			credential = Credential.fromJSON(new JSONObject(credentials));
		} catch (JSONException e) {
			LOGGER.error("Invalid credentials format", e);
			return null;
		}

		LOGGER.debug("Checking nonce");
		if (credential != null && this.nonces.contains(credential.getNonce())) {
			return this.auth.authenticateUser(credential);
		}
		nonces.remove(credential.getNonce());
		return null;
	}

	public int getNonce() {
		int nonce = new Random().nextInt(999999);
		this.nonces.add(nonce);
		return nonce;
	}

	protected List<Task> getTasksFromJDFFile(String jdfFilePath, JDFJob job) throws CompilerException {
		List<Task> taskList = JDFTasks.getTasksFromJDFFile(job, jdfFilePath, this.properties);
		return taskList;
	}

	public User getUser(String username) {
		return this.auth.getUserByUsername(username);
	}

	public User addUser(String username, String publicKey) {
		try {
			return this.auth.addUser(username, publicKey);
		} catch (Exception e) {
			throw new RuntimeException("Could not add user", e);
		}
	}

	public String getAuthenticatorName() {
		return this.auth.getAuthenticatorName();
	}


	public void setBlowoutController(BlowoutController blowout) {
		this.blowoutController = blowout;
	}

	public JobDataStore getJobDataStore() {
		return this.jobDataStore;
	}

	public void setDataStore(JobDataStore dataStore) {
		this.jobDataStore = dataStore;
	}

	public HashMap<String, Task> getFinishedTasks() {
		return finishedTasks;
	}

	public void setFinishedTasks(HashMap<String, Task> finishedTasks) {
		this.finishedTasks = finishedTasks;
	}
}
