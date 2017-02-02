package org.fogbowcloud.app;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.jetty.util.thread.Scheduler;
import org.fogbowcloud.app.datastore.JobDataStore;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.Job;
import org.fogbowcloud.app.model.User;
import org.fogbowcloud.app.utils.PropertiesConstants;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.core.util.AppPropertiesConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

		properties.put(PropertiesConstants.REST_SERVER_PORT, "4444");
		properties.put(PropertiesConstants.EXECUTION_MONITOR_PERIOD, "60000");
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT, "300000000");
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME, "30000");
		properties.put("datastore_url", "jdbc:h2:/local/marcosancj/git/arrebol/datastores/fogbowresourcesdatastore");
		this.arrebolController = Mockito.spy(new ArrebolController(properties));		
		this.arrebolController.init();
		}

	@After
	public void tearDown() {
		//FIXME fix test to use the new DB
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
	public void testGetJobById() {
		String owner = "owner";
		JDFJob job = new JDFJob("", owner, new ArrayList<Task>());
		String jobId = "jobId00";
		Assert.assertEquals(job,this.arrebolController.getJobById(jobId, owner));
	}
		
	@Test
	public void testAddJob() throws Exception {
		List<Task> tasks = new ArrayList<Task>();
		Mockito.doReturn(tasks).when(this.arrebolController).getTasksFromJDFFile(Mockito.anyString(),
				Mockito.any(JDFJob.class));

		String jdfFilePath = "";
		String schedPath = "";
		User user = Mockito.mock(User.class);
		BlowoutController controller = mock(BlowoutController.class);
		HashMap<String, JDFJob> jobMap = mock(HashMap.class);
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
		ArrayList<Task> task = new ArrayList<Task>();
		jobs.add(new JDFJob("", owner, task));
		jobs.add(new JDFJob("", owner, task));
		jobs.add(new JDFJob("", owner, task));
		jobs.add(new JDFJob("", owner, task));

		BlowoutController controller = mock(BlowoutController.class);
		HashMap<String, JDFJob> jobMap = mock(HashMap.class);
		arrebolController.setBlowoutController(controller);
		this.arrebolController.setJobMap(jobMap);
		doReturn(jobs).when(jobMap).values();
		doReturn(mock(Job.class)).when(jobMap).put(any(String.class), any(JDFJob.class));
		doNothing().when(this.arrebolController).updateJob(any(JDFJob.class));

		this.arrebolController.getAllJobs(owner);

		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		
		Assert.assertEquals(jobs, this.arrebolController.getAllJobs(owner));
		JobDataStore jobDataStore = this.arrebolController.getJobDataStore();
		Assert.assertEquals(jobs.size(), jobDataStore.getAllByOwner(owner).size());
	}

	@Test
	public void testGetAllJobsWithoutAnotherUser() {
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		ArrayList<Task> task = new ArrayList<Task>();
		jobs.add(new JDFJob("", owner, task));
		jobs.add(new JDFJob("", owner, task));
		jobs.add(new JDFJob("", owner, task));
		jobs.add(new JDFJob("", owner, task));
		// Mockito.when(this.arrebolController.getScheduler().getJobs()).thenReturn(jobs);
		BlowoutController controller = mock(BlowoutController.class);
		HashMap<String, JDFJob> jobMap = mock(HashMap.class);
		arrebolController.setBlowoutController(controller);
		this.arrebolController.setJobMap(jobMap);

		Assert.assertEquals(0, this.arrebolController.getAllJobs("wrong user owner").size());

		Assert.assertEquals(0, jobMap.size());

		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		
		Assert.assertEquals(0, this.arrebolController.getAllJobs("wrong user owner").size());
		
		JobDataStore jobDataStore = this.arrebolController.getJobDataStore();
		Assert.assertEquals(0, jobDataStore.getAllByOwner("wrong user owner").size());
	}	
	
	@Test
	public void testGetJobByName() {
		String jobName = "jobName00";
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		ArrayList<Task> task = new ArrayList<Task>();
		JDFJob jdfJob = new JDFJob("", owner, new ArrayList<Task>());
		jdfJob.setFriendlyName(jobName);
		jobs.add(jdfJob);
		jobs.add(new JDFJob("", owner, task));
		jobs.add(new JDFJob("", owner, task));
		BlowoutController controller = mock(BlowoutController.class);
		HashMap<String, JDFJob> jobMap = mock(HashMap.class);
		arrebolController.setBlowoutController(controller);
		this.arrebolController.setJobMap(jobMap);
		doReturn(jobs).when(jobMap).values();

		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		
		this.arrebolController.getJobByName(jobName, owner);
		Assert.assertEquals(jdfJob, this.arrebolController.getJobByName(jobName, owner));
	}

	@Test
	public void testStopJob() {
		String jobName = "jobName00";
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner, new ArrayList<Task>());
		ArrayList<Task> task = new ArrayList<Task>();
		jdfJob.setFriendlyName(jobName);
		jobs.add(jdfJob);
		jobs.add(new JDFJob("", owner, task));
		jobs.add(new JDFJob("", owner, task));
		BlowoutController controller = mock(BlowoutController.class);
		HashMap<String, JDFJob> jobMap = mock(HashMap.class);
		arrebolController.setBlowoutController(controller);
		doReturn(jobs).when(jobMap).values();
		this.arrebolController.setJobMap(jobMap);
		doNothing().when(arrebolController).updateJob(any(JDFJob.class));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		
		// update DB Map
		this.arrebolController.getAllJobs(owner);

		Assert.assertEquals(jdfJob.getId(), this.arrebolController.stopJob(jobName, owner));
		Assert.assertEquals(jobs.size() - 1, this.arrebolController.getJobDataStore().getAll().size());
	}

	@Test
	public void testStopJobWithId() {
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner, new ArrayList<Task>());
		jobs.add(jdfJob);
		ArrayList<Task> task = new ArrayList<Task>();
		jobs.add(new JDFJob("", owner, task));
		jobs.add(new JDFJob("", owner, task));

		BlowoutController controller = mock(BlowoutController.class);
		HashMap<String, JDFJob> jobMap = mock(HashMap.class);
		arrebolController.setBlowoutController(controller);
		doReturn(jobs).when(jobMap).values();
		this.arrebolController.setJobMap(jobMap);
		doNothing().when(arrebolController).updateJob(any(JDFJob.class));
		doReturn(jdfJob).when(this.arrebolController).getJobByName(anyString(), eq(owner));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		jobs.add(new JDFJob("", owner, new ArrayList<Task>()));
		
		// update DB Map
		this.arrebolController.getAllJobs(owner);

		Assert.assertEquals(jdfJob.getId(), this.arrebolController.stopJob(jdfJob.getId(), owner));
		Assert.assertEquals(jobs.size() - 1, this.arrebolController.getJobDataStore().getAll().size());
	}	
	
	@Test
	public void testGetTaskById() {
		String taskId = "taskId00";
		Task task = new TaskImpl(taskId, new Specification("image", "username", "publicKey", "privateKeyFilePath", "", ""));
		List<Task> tasks = new ArrayList<Task>();
		tasks.add(task);
		
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner, tasks);
		jobs.add(jdfJob);
		
		Assert.assertEquals(jobs, this.arrebolController.getAllJobs(owner));				
		Assert.assertEquals(task, this.arrebolController.getTaskById(taskId, owner));

		jdfJob.addTask(task);
		// jdfJob.run(task);
		tasks.add(task);

		BlowoutController controller = mock(BlowoutController.class);
		HashMap<String, JDFJob> jobMap = mock(HashMap.class);
		arrebolController.setBlowoutController(controller);
		doReturn(jobs).when(jobMap).values();
		this.arrebolController.setJobMap(jobMap);
		doNothing().when(arrebolController).updateJob(any(JDFJob.class));
		// update DB Map

		Assert.assertEquals(jobs, this.arrebolController.getAllJobs(owner));
		Assert.assertEquals(task, this.arrebolController.getTaskById(taskId, owner));
	}
}
