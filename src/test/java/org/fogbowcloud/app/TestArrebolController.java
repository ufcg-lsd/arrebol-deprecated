package org.fogbowcloud.app;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.Job;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.utils.PropertiesConstants;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mockito.Mockito;

public class TestArrebolController {

	private ArrebolController arrebolController;

	@Before
	public void setUp() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertiesConstants.REST_SERVER_PORT, "4444");
		properties.put(PropertiesConstants.EXECUTION_MONITOR_PERIOD, "60000");
		properties.put(PropertiesConstants.AUTHENTICATION_PLUGIN,
				"org.fogbowcloud.app.utils.authenticator.CommonAuthenticator");
		this.arrebolController = Mockito.spy(new ArrebolController(properties));
		// this.arrebolController.init();

	}

	@After
	public void tearDown() {
		String[] filePaths = new String[3];
		filePaths[0] = PropertiesConstants.DB_FILE_NAME;
		filePaths[1] = PropertiesConstants.DB_FILE_NAME + ".p";
		filePaths[2] = PropertiesConstants.DB_FILE_NAME + ".t";
		for (int i = 0; i < filePaths.length; i++) {
			File file = new File(filePaths[i]);
			if (file.exists()) {
				file.delete();
			}
		}
	}

	@Test
	public void testAddJob() throws Exception {
		List<Task> tasks = new ArrayList<Task>();
		Mockito.doReturn(tasks).when(this.arrebolController).getTasksFromJDFFile(Mockito.anyString(),
				Mockito.any(JDFJob.class));

		String jdfFilePath = "";
		String schedPath = "";
		String friendlyName = "";
		User user = Mockito.mock(User.class);
		BlowoutController controller = mock(BlowoutController.class);
		ConcurrentMap<String, JDFJob> jobMap = mock(HTreeMap.class);
		arrebolController.setBlowoutController(controller);
		this.arrebolController.setJobMap(jobMap);
		doReturn(mock(Job.class)).when(jobMap).put(any(String.class), any(JDFJob.class));
		this.arrebolController.addJob(jdfFilePath, schedPath, user);
		Mockito.verify(controller).addTaskList(tasks);
		;
	}

	@Test
	public void testGetAllJobs() {
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));

		BlowoutController controller = mock(BlowoutController.class);
		ConcurrentMap<String, JDFJob> jobMap = mock(HTreeMap.class);
		arrebolController.setBlowoutController(controller);
		this.arrebolController.setJobMap(jobMap);
		doReturn(jobs).when(jobMap).values();
		doReturn(mock(Job.class)).when(jobMap).put(any(String.class), any(JDFJob.class));
		doNothing().when(this.arrebolController).updateJob(any(JDFJob.class));

		this.arrebolController.getAllJobs(owner);

	}

	@Test
	public void testGetAllJobsWithoutAnotherUser() {
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		// Mockito.when(this.arrebolController.getScheduler().getJobs()).thenReturn(jobs);
		BlowoutController controller = mock(BlowoutController.class);
		ConcurrentMap<String, JDFJob> jobMap = mock(HTreeMap.class);
		arrebolController.setBlowoutController(controller);
		this.arrebolController.setJobMap(jobMap);

		Assert.assertEquals(0, this.arrebolController.getAllJobs("wrong user owner").size());

		Assert.assertEquals(0, jobMap.size());
	}

	@Test
	public void testGetJobByName() {
		String jobName = "jobName00";
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner);
		jdfJob.setFriendlyName(jobName);
		jobs.add(jdfJob);
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		BlowoutController controller = mock(BlowoutController.class);
		ConcurrentMap<String, JDFJob> jobMap = mock(HTreeMap.class);
		arrebolController.setBlowoutController(controller);
		this.arrebolController.setJobMap(jobMap);
		doReturn(jobs).when(jobMap).values();

		this.arrebolController.getJobByName(jobName, owner);
		Assert.assertEquals(jdfJob, this.arrebolController.getJobByName(jobName, owner));
	}

	@Test
	public void testStopJob() {
		String jobName = "jobName00";
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner);
		jdfJob.setFriendlyName(jobName);
		jobs.add(jdfJob);
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		BlowoutController controller = mock(BlowoutController.class);
		DB jobDB = mock(DB.class);
		ConcurrentMap<String, JDFJob> jobMap = mock(HTreeMap.class);
		arrebolController.setBlowoutController(controller);
		doReturn(jobs).when(jobMap).values();
		this.arrebolController.setJobMap(jobMap);
		doNothing().when(arrebolController).updateJob(any(JDFJob.class));
		// update DB Map
		doNothing().when(jobDB).commit();
		this.arrebolController.setJobDB(jobDB);
		this.arrebolController.getAllJobs(owner);

		Assert.assertEquals(jdfJob.getId(), this.arrebolController.stopJob(jobName, owner));
	}

	@Test
	public void testStopJobWithId() {
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner);
		jobs.add(jdfJob);
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));

		BlowoutController controller = mock(BlowoutController.class);
		DB jobDB = mock(DB.class);
		ConcurrentMap<String, JDFJob> jobMap = mock(HTreeMap.class);
		arrebolController.setBlowoutController(controller);
		doReturn(jobs).when(jobMap).values();
		this.arrebolController.setJobMap(jobMap);
		doNothing().when(arrebolController).updateJob(any(JDFJob.class));
		doReturn(jdfJob).when(this.arrebolController).getJobByName(anyString(), eq(owner));
		doNothing().when(jobDB).commit();
		this.arrebolController.setJobDB(jobDB);
		// update DB Map
		this.arrebolController.getAllJobs(owner);

		Assert.assertEquals(jdfJob.getId(), this.arrebolController.stopJob(jdfJob.getId(), owner));
	}

	@Test
	public void testGetTaskById() {
		String taskId = "taskId00";
		Task task = new TaskImpl(taskId,
				new Specification("image", "username", "publicKey", "privateKeyFilePath", "", ""));

		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner);
		jdfJob.addTask(task);
		// jdfJob.run(task);
		jobs.add(jdfJob);

		BlowoutController controller = mock(BlowoutController.class);
		DB jobDB = mock(DB.class);
		ConcurrentMap<String, JDFJob> jobMap = mock(HTreeMap.class);
		arrebolController.setBlowoutController(controller);
		doReturn(jobs).when(jobMap).values();
		this.arrebolController.setJobMap(jobMap);
		doNothing().when(arrebolController).updateJob(any(JDFJob.class));
		// update DB Map
		doNothing().when(jobDB).commit();

		Assert.assertEquals(jobs, this.arrebolController.getAllJobs(owner));
		Assert.assertEquals(task, this.arrebolController.getTaskById(taskId, owner));
	}
}
