package org.fogbowcloud.app;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.app.datastore.JobDataStore;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.exception.BlowoutException;
import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.core.util.AppPropertiesConstants;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestArrebolController {

	private static final String FAKE_UUID = "1234";
	private static final String testowner = "testowner";
	private static final String testtaskid = "testtaskId";
	private static final String testimage = "testimage";
	private static final String testPublicKey = "testPublicKey";
	private static final String testPrivateKeyPath = "testPrivateKeyPath";

	private ArrebolController arrebolController;
	private BlowoutController blowoutController;
	private JobDataStore dataStore;

	@Before
	public void setUp() throws Exception {
		Properties properties = new Properties();
		properties.put(ArrebolPropertiesConstants.REST_SERVER_PORT, "4444");
		properties.put(ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD, "60000");
		properties.put(
				ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN,
				"org.fogbowcloud.app.utils.authenticator.CommonAuthenticator"
		);
		properties.put(ArrebolPropertiesConstants.PUBLIC_KEY_CONSTANT, testPublicKey);
		properties.put(ArrebolPropertiesConstants.PRIVATE_KEY_FILEPATH, testPrivateKeyPath);
		properties.put(ArrebolPropertiesConstants.REMOTE_OUTPUT_FOLDER, "/tmp");
		properties.put(ArrebolPropertiesConstants.LOCAL_OUTPUT_FOLDER, "/tmp");

		properties.put(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT, "300000000");
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME, "30000");
		properties.put(AppPropertiesConstants.INFRA_IS_STATIC, "true");
		properties.put(
				AppPropertiesConstants.INFRA_AUTH_TOKEN_UPDATE_PLUGIN,
				"org.fogbowcloud.blowout.infrastructure.token.LDAPTokenUpdatePlugin"
		);
		properties.put(
				AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME,
				"org.fogbowcloud.blowout.infrastructure.provider.fogbow.FogbowInfrastructureProvider"
		);
		properties.put(AppPropertiesConstants.DB_DATASTORE_URL, "jdbc:h2:/tmp/datastores/testfogbowresourcesdatastore");
		this.arrebolController = Mockito.spy(new ArrebolController(properties));
		this.blowoutController = mock(BlowoutController.class);
		this.dataStore = Mockito.spy(new JobDataStore(properties.getProperty(AppPropertiesConstants.DB_DATASTORE_URL)));

		this.arrebolController.setBlowoutController(blowoutController);
		this.arrebolController.setDataStore(dataStore);
	}

	@After
	public void tearDown() {
		// FIXME fix test to use the new DB
		this.dataStore.deleteAll();
	}

	@Test
	public void testRestart() throws BlowoutException, JSONException{
		ArrayList<Task> taskList = new ArrayList<>();
		Specification spec = new Specification(
				testimage,
				testowner,
				testPublicKey,
				testPrivateKeyPath,
				"",
				""
		);
		Task task = new TaskImpl(testtaskid, spec, FAKE_UUID);
		taskList.add(task);
		
		JDFJob job = new JDFJob(testowner, taskList, "");
		this.arrebolController.getJobDataStore().insert(job);
		
		try {
			this.arrebolController.restartAllJobs();
		} catch (BlowoutException e) {
			Assert.fail();
		}
		Assert.assertEquals(1, this.arrebolController.getAllJobs(testowner).size());
		JDFJob job1 = this.arrebolController.getAllJobs(testowner).get(0);
		assert(job1.equals(job));
		System.out.println(this.arrebolController.getAllJobs(testowner).get(0).getTaskById(testtaskid).getSpecification().toJSON().toString());
		
		Specification spec2 =  Specification.fromJSON(
				new JSONObject(
						this.arrebolController
								.getAllJobs(testowner)
								.get(0)
								.getTaskById(testtaskid)
								.getSpecification()
								.toJSON()
								.toString()
				)
		);
		Assert.assertEquals(1, this.arrebolController.getAllJobs(testowner).get(0).getTaskList().size());
		assert(spec.equals(spec2));
		assert(task.equals(this.arrebolController.getAllJobs(testowner).get(0).getTaskList().get(testtaskid)));
		assert(spec.equals(this.arrebolController.getAllJobs(testowner).get(0).getTaskList().get(testtaskid).getSpecification()));
		Mockito.verify(this.blowoutController).addTaskList(taskList);
	}

	@Test
	public void testGetJobById() {
		String jobId = "jobId00";
		JDFJob job = new JDFJob(jobId, testowner, new ArrayList<Task>(), null);
		doReturn(job).when(dataStore).getByJobId(jobId, testowner);
		assert(job.equals(this.arrebolController.getJobById(jobId, testowner)));
	}

	@Test
	public void testAddJob() throws Exception {
		String jdfFilePath = "";
		User user = Mockito.mock(User.class);

		JDFJob job = new JDFJob(user.getUser(), new ArrayList<Task>(), user.getUsername());
		Mockito.doReturn(job).when(this.arrebolController).createJobFromJDFFile(
				Mockito.anyString(),
				Mockito.any(User.class)
		);

		BlowoutController controller = mock(BlowoutController.class);
		arrebolController.setBlowoutController(controller);
		this.arrebolController.addJob(jdfFilePath, user);
		Mockito.verify(controller).addTaskList(job.getTasks());
	}

	@Test
	public void testGetAllJobs() {
		ArrayList<JDFJob> jobs = new ArrayList<>();
		ArrayList<Task> task = new ArrayList<>();
		jobs.add(new JDFJob("job1", testowner, task, null));
		jobs.add(new JDFJob("job2", testowner, task, null));
		jobs.add(new JDFJob("job3", testowner, task, null));
		jobs.add(new JDFJob("job4", testowner, task, null));

		BlowoutController controller = mock(BlowoutController.class);
		arrebolController.setBlowoutController(controller);
		doReturn(jobs).when(this.dataStore).getAllByOwner(testowner);
		doNothing().when(this.arrebolController).updateJob(any(JDFJob.class));

		this.arrebolController.getAllJobs(testowner);

		Assert.assertEquals(jobs.size(), this.arrebolController.getAllJobs(testowner).size());
	}

	@Test
	public void testGetAllJobsWithoutAnotherUser() {
		ArrayList<JDFJob> jobs = new ArrayList<>();
		ArrayList<Task> task = new ArrayList<>();
		jobs.add(new JDFJob(testowner, task, null));
		jobs.add(new JDFJob(testowner, task, null));
		jobs.add(new JDFJob(testowner, task, null));
		jobs.add(new JDFJob(testowner, task, null));
		// Mockito.when(this.arrebolController.getScheduler().getJobs()).thenReturn(jobs);
		BlowoutController controller = mock(BlowoutController.class);
		arrebolController.setBlowoutController(controller);
		doReturn(jobs).when(this.dataStore).getAllByOwner(testowner);

		Assert.assertEquals(0, this.arrebolController.getAllJobs("wrong user owner").size());

		jobs.add(new JDFJob(testowner, task, null));
		jobs.add(new JDFJob(testowner, task, null));
		jobs.add(new JDFJob(testowner, task, null));
		jobs.add(new JDFJob(testowner, task, null));

		Assert.assertEquals(0, this.arrebolController.getAllJobs("wrong user owner").size());

		JobDataStore jobDataStore = this.arrebolController.getJobDataStore();
		Assert.assertEquals(0, jobDataStore.getAllByOwner("wrong user owner").size());
	}

	@Test
	public void testGetJobByName() {
		String jobName = "jobName00";
		ArrayList<JDFJob> jobs = new ArrayList<>();
		ArrayList<Task> task = new ArrayList<>();
		JDFJob jdfJob = new JDFJob(testowner, task, null);
		jdfJob.setFriendlyName(jobName);
		jobs.add(jdfJob);
		jobs.add(new JDFJob(testowner, task, null));
		jobs.add(new JDFJob(testowner, task, null));
		BlowoutController controller = mock(BlowoutController.class);
		arrebolController.setBlowoutController(controller);

		jobs.add(new JDFJob(testowner, task, null));
		jobs.add(new JDFJob(testowner, task, null));

		doReturn(jobs).when(this.dataStore).getAllByOwner(testowner);

		this.arrebolController.getJobByName(jobName, testowner);
		assert(jdfJob.equals(this.arrebolController.getJobByName(jobName, testowner)));
	}

	@Test
	public void testStopJob() {
		String jobName = "jobName00";
		ArrayList<JDFJob> jobs = new ArrayList<>();
		JDFJob jdfJob = new JDFJob(testowner, new ArrayList<Task>(), null);
		ArrayList<Task> task = new ArrayList<>();
		jdfJob.setFriendlyName(jobName);
		jobs.add(jdfJob);
		jobs.add(new JDFJob(testowner, task, null));
		jobs.add(new JDFJob(testowner, task, null));
		doReturn(true).when(this.dataStore).deleteByJobId(jdfJob.getId(), testowner);
		doNothing().when(arrebolController).updateJob(any(JDFJob.class));
		doNothing().when(blowoutController).cleanTask(any(Task.class));
		doReturn(jdfJob).when(arrebolController).getJobByName(jobName, testowner);
		// update DB Map
		this.arrebolController.stopJob(jobName, testowner);

		Mockito.verify(this.dataStore).deleteByJobId(jdfJob.getId(), testowner);
	}

	@Test
	public void testStopJobWithId() {
		ArrayList<JDFJob> jobs = new ArrayList<>();
		ArrayList<Task> task = new ArrayList<>();
		JDFJob jdfJob = new JDFJob(testowner, task, null);
		jobs.add(jdfJob);
		jobs.add(new JDFJob("job1", testowner, task, null));
		jobs.add(new JDFJob("job2", testowner, task, null));

		doReturn(jobs).when(this.dataStore).getAllByOwner(testowner);
		doReturn(true).when(this.dataStore).deleteByJobId(jdfJob.getId(), testowner);
		doNothing().when(arrebolController).updateJob(any(JDFJob.class));
		doNothing().when(blowoutController).cleanTask(any(Task.class));
		doReturn(jdfJob).when(this.dataStore).getByJobId(jdfJob.getId(), testowner);
		// update DB Map
		this.arrebolController.stopJob(jdfJob.getId(), testowner);

		Mockito.verify(this.dataStore).deleteByJobId(jdfJob.getId(), testowner);
	}

	@Test
	public void testGetTaskById() {
		Task task = new TaskImpl(testtaskid, new Specification(
				testimage,
				testowner,
				testPublicKey,
				testPrivateKeyPath,
				"",
				""
		), FAKE_UUID);
		List<Task> tasks = new ArrayList<>();
		tasks.add(task);

		ArrayList<JDFJob> jobs = new ArrayList<>();
		JDFJob jdfJob = new JDFJob(testowner, tasks, null);
		jobs.add(jdfJob);

		doReturn(jobs).when(this.dataStore).getAllByOwner(testowner);
		assert(jobs.get(0).equals( this.arrebolController.getAllJobs(testowner).get(0)));
		Assert.assertEquals(task, this.arrebolController.getTaskById(testtaskid, testowner));

		jdfJob.addTask(task);
		// jdfJob.run(task);
		tasks.add(task);

		BlowoutController controller = mock(BlowoutController.class);
		arrebolController.setBlowoutController(controller);
		doNothing().when(arrebolController).updateJob(any(JDFJob.class));
		// update DB Map

		Assert.assertEquals(jobs, this.arrebolController.getAllJobs(testowner));
		Assert.assertEquals(task, this.arrebolController.getTaskById(testtaskid, testowner));
	}
}
