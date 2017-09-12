package org.fogbowcloud.app;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.datastore.JobDataStore;
import org.fogbowcloud.app.exception.ArrebolException;
import org.fogbowcloud.app.jdfcompiler.main.CompilerException;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.JDFTasks;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.app.utils.authenticator.ArrebolAuthenticator;
import org.fogbowcloud.app.utils.authenticator.Credential;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.exception.BlowoutException;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.blowout.core.util.AppPropertiesConstants;
import org.fogbowcloud.blowout.core.util.ManagerTimer;
import org.fogbowcloud.blowout.infrastructure.provider.fogbow.FogbowRequirementsHelper;
import org.json.JSONException;
import org.json.JSONObject;

public class ArrebolController {

	private static final Logger LOGGER = Logger.getLogger(ArrebolController.class);

	private BlowoutController blowoutController;
	private Properties properties;
	private List<Integer> nonces;
	private HashMap<String, Task> finishedTasks;
	private JobDataStore jobDataStore;
	private ArrebolAuthenticator auth;

    private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));

	public ArrebolController(Properties properties)
			throws BlowoutException, ArrebolException {
		if (properties == null) {
			throw new IllegalArgumentException("Properties cannot be null.");
		} else if (!checkProperties(properties)) {
			throw new ArrebolException("Error while initializing Arrebol Controller.");
		}
		this.finishedTasks = new HashMap<>();
		this.properties = properties;
		this.blowoutController = new BlowoutController(properties);
	}

	public Properties getProperties() {
		return properties;
	}

	public void init() throws Exception {
		// FIXME: add as constructor param?
		this.auth = createAuthenticatorPluginInstance();
		// FIXME: replace by a proper
		this.jobDataStore = new JobDataStore(properties.getProperty(AppPropertiesConstants.DB_DATASTORE_URL));

		Boolean removePreviousResources = Boolean.valueOf(
				this.properties.getProperty(ArrebolPropertiesConstants.REMOVE_PREVIOUS_RESOURCES)
		);

		LOGGER.debug("Properties: " + properties.getProperty(ArrebolPropertiesConstants.DEFAULT_SPECS_FILE_PATH));

		blowoutController.start(removePreviousResources);

		LOGGER.debug("Application to be started on port: " + properties.getProperty(ArrebolPropertiesConstants.REST_SERVER_PORT));
		LOGGER.info("Properties: " + properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH));

		this.nonces = new ArrayList<>();

		LOGGER.debug(
				"Starting Scheduler and Execution Monitor, execution monitor period: "
				+ properties.getProperty(ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD)
		);
        ExecutionMonitorWithDB executionMonitor = new ExecutionMonitorWithDB(blowoutController, this, jobDataStore);
        int schedulerPeriod = Integer.valueOf(properties.getProperty(ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD));
        LOGGER.info("Starting scheduler with period: " + schedulerPeriod);
		executionMonitorTimer.scheduleAtFixedRate(executionMonitor, 0, schedulerPeriod);

		restartAllJobs();
	}

	public void restartAllJobs() throws BlowoutException {
		for (JDFJob job : this.jobDataStore.getAll()) {
			ArrayList<Task> taskList = new ArrayList<>();
			for (Task task : job.getTasks()) {
				if (!task.isFinished()) {
					taskList.add(task);
					LOGGER.debug("Specification of Recovered task: " + task.getSpecification().toJSON().toString());
					LOGGER.debug("Task Requirements: " + task.getSpecification()
							.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS));
				}
			}
			blowoutController.addTaskList(taskList);
		}
	}

	private ArrebolAuthenticator createAuthenticatorPluginInstance() throws Exception {
		String providerClassName = this.properties.getProperty(ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN);
		Class<?> forName = Class.forName(providerClassName);
		Object clazz = forName.getConstructor(Properties.class).newInstance(this.properties);
		if (!(clazz instanceof ArrebolAuthenticator)) {
			throw new Exception("Authenticator Class Name is not a ArrebolAuthenticator implementation");
		}

		return (ArrebolAuthenticator) clazz;
	}

	public JDFJob getJobById(String jobId, String owner) {
        return this.jobDataStore.getByJobId(jobId, owner);
	}

	public String addJob(String jdfFilePath, String schedPath, User owner)
			throws CompilerException, NameAlreadyInUseException, BlowoutException, IOException {
		JDFJob tmpJob = new JDFJob(schedPath, owner.getUser(), new ArrayList<Task>(), owner.getUsername());
		LOGGER.debug("Adding job  of owner " +owner.getUsername()+" to scheduler" );
		List<Task> taskList = getTasksFromJDFFile(jdfFilePath, tmpJob);
		JDFJob job = new JDFJob(tmpJob.getId(), tmpJob.getSchedPath(), tmpJob.getOwner(), taskList, owner.getUsername());
		job.setFriendlyName(tmpJob.getName());

		if (job.getName() != null && !job.getName().trim().isEmpty()
				&& getJobByName(job.getName(), owner.getUser()) != null) {
			throw new NameAlreadyInUseException(
					"The name " + job.getName() + " is already in use for the user " + owner.getUser()
			);
		}

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
			// TODO review this IFs
			if (jobName.equals(job.getName())) {
				return job;
			}
		}
		return null;
	}

	public Task getTaskById(String taskId, String owner) {
		for (JDFJob job : getAllJobs(owner)) {
            Task task = job.getTaskById(taskId);
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

		Credential credential;
		try {
			credential = Credential.fromJSON(new JSONObject(credentials));
		} catch (JSONException e) {
			LOGGER.error("Invalid credentials format", e);
			return null;
		}

		LOGGER.debug("Checking nonce");
		if (this.nonces.contains(credential.getNonce())) {
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

	protected List<Task> getTasksFromJDFFile(String jdfFilePath, JDFJob job) throws CompilerException, IOException {
        return JDFTasks.getTasksFromJDFFile(job, jdfFilePath, this.properties);
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


	private static String requiredPropertyMessage(String property) {
		return "Required property " + property + " was not set";
	}

	//TODO: Maybe this method should be separate in some utils classes, one to each plugin in use
	// Each plugin must responsible to check the value of its properties
	private static boolean checkProperties(Properties properties) {
		// Arrebol required properties
		if (!properties.containsKey(ArrebolPropertiesConstants.REST_SERVER_PORT)) {
			LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.REST_SERVER_PORT));
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD)) {
			LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD));
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.PUBLIC_KEY_CONSTANT)) {
			LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.PUBLIC_KEY_CONSTANT));
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.PRIVATE_KEY_FILEPATH)) {
			LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.PRIVATE_KEY_FILEPATH));
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.REMOTE_OUTPUT_FOLDER)) {
			LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.REMOTE_OUTPUT_FOLDER));
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.LOCAL_OUTPUT_FOLDER)) {
			LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.LOCAL_OUTPUT_FOLDER));
			return false;
		}
		if (properties.containsKey(ArrebolPropertiesConstants.ENCRYPTION_TYPE)) {
			try {
				MessageDigest.getInstance(properties.getProperty(ArrebolPropertiesConstants.ENCRYPTION_TYPE));
			} catch (NoSuchAlgorithmException e) {
				String builder = "Property " +
						ArrebolPropertiesConstants.ENCRYPTION_TYPE +
						"(" +
						properties.getProperty(ArrebolPropertiesConstants.ENCRYPTION_TYPE) +
						") does not refer to a valid encryption algorithm." +
						" Valid options are 'MD5', 'SHA-1' and 'SHA-256'.";
				LOGGER.error(builder);
				return false;
			}
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN)) {
			LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN));
			return false;
		} else {
			String authenticationPlugin = properties.getProperty(ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN);
			if (authenticationPlugin.equals("org.fogbowcloud.app.utils.LDAPAuthenticator")) {
				if (!properties.containsKey(ArrebolPropertiesConstants.LDAP_AUTHENTICATION_URL)) {
					LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.LDAP_AUTHENTICATION_URL));
					return false;
				}
				if (!properties.containsKey(ArrebolPropertiesConstants.LDAP_AUTHENTICATION_BASE)) {
					LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.LDAP_AUTHENTICATION_BASE));
					return false;
				}
			}
		}
		LOGGER.debug("All properties are set");
		return true;
	}
}
