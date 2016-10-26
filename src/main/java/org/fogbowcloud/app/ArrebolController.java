package org.fogbowcloud.app;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.jdfcompiler.main.CompilerException;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.JDFTasks;
import org.fogbowcloud.app.model.Job;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.utils.PropertiesConstants;
import org.fogbowcloud.app.utils.authenticator.ArrebolAuthenticator;
import org.fogbowcloud.app.utils.authenticator.Credential;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.blowout.core.util.ManagerTimer;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;

public class ArrebolController {

	//FIXME: get from conf
	private static final int DEFAULT_EXECUTION_MONITOR_INTERVAL = 30000;

	//FIXME: private
	public static final Logger LOGGER = Logger.getLogger(ArrebolController.class);

	//FIXME: final
	private DB jobDB;
	private BlowoutController blowoutController;
	private Properties properties;
	private List<Integer> nonces;
	private ConcurrentMap<String, JDFJob> jobMap;
	private ConcurrentMap<String, Task> finishedTasks;
	private ArrebolAuthenticator auth;

	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));

	public ArrebolController(Properties properties) throws Exception {
		this.properties = properties;
	}

	//FIXME: protected
	public Properties getProperties() {
		return properties;
	}

	protected DB getJobDB() {
		return jobDB;
	}
	//FIXME: private
	//FIXME: replace by a proper DB
	protected ConcurrentMap<String, JDFJob> getJobMap() {
		return jobMap;
	}

	public void init() throws Exception {
		//FIXME: add as constructor param?
		this.auth = createAuthenticatorPluginInstance();
		
		//FIXME: replace by a proper
		final File pendingImageDownloadFile = new File(PropertiesConstants.DB_FILE_NAME);
		this.jobDB = DBMaker.newFileDB(pendingImageDownloadFile).make();
		this.jobDB.checkShouldCreate(PropertiesConstants.DB_MAP_NAME);
		ConcurrentMap<String, JDFJob> jobMapDB = this.jobDB.getHashMap(PropertiesConstants.DB_MAP_NAME);

		Boolean removePreviousResources = new Boolean(
				this.properties.getProperty(PropertiesConstants.REMOVE_PREVIOUS_RESOURCES))
						.booleanValue();

		ArrayList<JDFJob> legacyJobs = getLegacyJobs(jobMapDB);
		LOGGER.debug("Properties: " + properties.getProperty(PropertiesConstants.DEFAULT_SPECS_FILE_PATH));

		//this.scheduler = new Scheduler(infraManager, );
		
		//legacyJobs.toArray(new JDFJob[legacyJobs.size()])
		
		this.blowoutController = new BlowoutController(this.properties);
		blowoutController.start(removePreviousResources);
		
		LOGGER.debug("Application to be started on port: "
				+ properties.getProperty(PropertiesConstants.REST_SERVER_PORT));
		ExecutionMonitorWithDB executionMonitor = new ExecutionMonitorWithDB(this.blowoutController, this, this.jobDB);

		this.jobMap = this.jobDB.getHashMap(PropertiesConstants.DB_MAP_NAME);
		this.nonces = new ArrayList<Integer>();

		LOGGER.debug("Starting Scheduler and Execution Monitor, execution monitor period: "
				+ properties.getProperty(PropertiesConstants.EXECUTION_MONITOR_PERIOD));
		executionMonitorTimer.scheduleAtFixedRate(executionMonitor, 0, DEFAULT_EXECUTION_MONITOR_INTERVAL);
	}

	private ArrayList<JDFJob> getLegacyJobs(ConcurrentMap<String, JDFJob> jobMapDB) {
		ArrayList<JDFJob> legacyJobs = new ArrayList<JDFJob>();

		for (String key : jobMapDB.keySet()) {
			legacyJobs.add((JDFJob) jobMapDB.get(key));
		}
		return legacyJobs;
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
		JDFJob jdfJob = (JDFJob) jobMap.get(jobId);
		if (jdfJob != null && jdfJob.getOwner().equals(owner)) {
			return jdfJob;
		}
		return null;
	}

	public String addJob(String jdfFilePath, String schedPath, User owner)
			throws CompilerException, NameAlreadyInUseException {
		JDFJob job = new JDFJob(schedPath, owner.getUsername());
		job.setUUID(owner.getUUID());
		List<Task> taskList = getTasksFromJDFFile(jdfFilePath, job);
		
		if (job.getName() != null && !job.getName().trim().isEmpty()
				&& getJobByName(job.getName(), owner.getUsername()) != null) {
			throw new NameAlreadyInUseException(
					"The name " + job.getName() + " is already in use for the user " + "owner");
		}

		for (Task task : taskList) {
			job.addTask(task);
		}

		LOGGER.debug("Adding job " + job.getName() + " to scheduler");

		this.jobMap.put(job.getId(), job);
		
		blowoutController.addTaskList(job.getTasks());
		
		return job.getId();
	}

	public ArrayList<JDFJob> getAllJobs(String owner) {
		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
		for (Job job : this.jobMap.values()) {
			JDFJob jdfJob = (JDFJob) job;
			if (jdfJob.getOwner().equals(owner)) {
				jobList.add((JDFJob) job);
				updateJob((JDFJob) job);
			}
		}
		return jobList;
	}

	public void setJobDB(DB jobDB) {
		this.jobDB = jobDB;
	}

	public void updateJob(JDFJob job) {
		this.jobMap.put(job.getId(), job);
		this.jobDB.commit();
	}

	public String stopJob(String jobReference, String owner) {
		Job jobToRemove = getJobByName(jobReference, owner);
		if (jobToRemove != null) {
			this.jobMap.remove(jobToRemove.getId());
			this.jobDB.commit();
			return jobToRemove.getId();
		} else {
			jobToRemove = getJobById(jobReference, owner);
			if (jobToRemove != null) {
				this.jobMap.remove(jobReference);
				this.jobDB.commit();
				return jobToRemove.getId();
			}
		}
		return null;
	}

	public JDFJob getJobByName(String jobName, String owner) {
		if (jobName == null) {
			return null;
		}
		for (Job job : this.jobMap.values()) {
			JDFJob jdfJob = (JDFJob) job;
			// TODO review this IFs
			if (jdfJob.getOwner().equals(owner)) {
				if (jobName.equals(((JDFJob) job).getName())) {
					return (JDFJob) job;
				}
			}
		}
		return null;
	}

	public Task getTaskById(String taskId, String owner) {
		for (Job job : getAllJobs(owner)) {
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
	
	public void moveTaskToFinished(Task task){
		finishedTasks.put(task.getId(), task);
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

	protected List<Task> getTasksFromJDFFile(String jdfFilePath, JDFJob job)
			throws CompilerException {
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

	public void setJobMap(ConcurrentMap<String, JDFJob> jobMap) {
		this.jobMap = jobMap;
	}

	public String getAuthenticatorName() {
		return this.auth.getAuthenticatorName();
	}

	public void setBlowoutController(BlowoutController blowout) {
		this.blowoutController = blowout;
	}
	
}
