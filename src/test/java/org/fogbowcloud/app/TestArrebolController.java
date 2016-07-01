package org.fogbowcloud.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.fogbowcloud.app.utils.AppPropertiesConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.DB;
import org.mockito.Mockito;

public class TestArrebolController {
	
	private ArrebolController arrebolController;
	
	@Before
	public void setUp() throws Exception {
		Properties properties = new Properties();
		properties.put(AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING, "true");
		properties.put(AppPropertiesConstants.INFRA_IS_STATIC, "true");
		properties.put(AppPropertiesConstants.INFRA_RESOURCE_REUSE_TIMES, "2");
		properties.put(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH, "");
		properties.put(AppPropertiesConstants.REST_SERVER_PORT, "4444");
		properties.put(AppPropertiesConstants.EXECUTION_MONITOR_PERIOD, "60000");
		properties.put(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME, 
				"org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowInfrastructureProvider");
		this.arrebolController = Mockito.spy(new ArrebolController(properties));		
		Mockito.doReturn(null).when(this.arrebolController).getInfraManager(true, true);		
		this.arrebolController.init();
		
		Scheduler scheduler = Mockito.mock(Scheduler.class);
		this.arrebolController.setScheduler(scheduler);
	}
	
	@After
	public void tearDown() {
		String[] filePaths = new String[3];
		filePaths[0] = AppPropertiesConstants.DB_FILE_NAME;
		filePaths[1] = AppPropertiesConstants.DB_FILE_NAME + ".p";
		filePaths[2] = AppPropertiesConstants.DB_FILE_NAME + ".t";
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
		Job job = new JDFJob("", owner);
		Mockito.when(this.arrebolController.getScheduler().getJobById(Mockito.anyString())).thenReturn(job);
		String jobId = "jobId00";
		Assert.assertEquals(job,this.arrebolController.getJobById(jobId, owner));
	}
		
	@Test
	public void testAddJob() throws Exception {
		List<Task> tasks = new ArrayList<Task>();		
		Mockito.doReturn(tasks).when(this.arrebolController).getTasksFromJDFFile(Mockito.anyString(), 
				Mockito.anyString(), Mockito.any(JDFJob.class));	
		
		String jdfFilePath = "";
		String schedPath = "";
		String friendlyName = "";
		this.arrebolController.addJob(jdfFilePath, schedPath, friendlyName);
		
		Mockito.verify(this.arrebolController.getScheduler(), Mockito.times(1)).addJob(Mockito.any(Job.class));
	}
	
	@Test
	public void testGetAllJobs() {
		ArrayList<Job> jobs = new ArrayList<Job>();
		String owner = "owner";
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		Mockito.when(this.arrebolController.getScheduler().getJobs()).thenReturn(jobs);
		
		Assert.assertEquals(jobs, this.arrebolController.getAllJobs(owner));
		
		DB jobDB = this.arrebolController.getJobDB();
		ConcurrentMap<String, JDFJob> jobMapDB = jobDB.get(AppPropertiesConstants.DB_MAP_NAME);
		ConcurrentMap<String,JDFJob> jobMap = this.arrebolController.getJobMap();
		Assert.assertEquals(jobMapDB, jobMap);
		Assert.assertEquals(jobs.size(), jobMap.size());
	}
	
	@Test
	public void testGetAllJobsWithoutAnotherUser() {
		ArrayList<Job> jobs = new ArrayList<Job>();
		String owner = "owner";
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		Mockito.when(this.arrebolController.getScheduler().getJobs()).thenReturn(jobs);
		
		Assert.assertEquals(0, this.arrebolController.getAllJobs("wrong user owner").size());
		
		DB jobDB = this.arrebolController.getJobDB();
		ConcurrentMap<String, JDFJob> jobMapDB = jobDB.get(AppPropertiesConstants.DB_MAP_NAME);
		ConcurrentMap<String,JDFJob> jobMap = this.arrebolController.getJobMap();
		Assert.assertEquals(jobMapDB, jobMap);
		Assert.assertEquals(0, jobMap.size());
	}	
	
	@Test
	public void testGetJobByName() {
		String jobName = "jobName00";
		ArrayList<Job> jobs = new ArrayList<Job>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner);
		jdfJob.setFriendlyName(jobName);
		jobs.add(jdfJob);
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		Mockito.when(this.arrebolController.getScheduler().getJobs()).thenReturn(jobs);
		
		this.arrebolController.getJobByName(jobName, owner);
		Assert.assertEquals(jdfJob, this.arrebolController.getJobByName(jobName, owner));
	}
	
	@Test
	public void testStopJob() {
		String jobName = "jobName00";
		ArrayList<Job> jobs = new ArrayList<Job>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner);
		jdfJob.setFriendlyName(jobName);
		jobs.add(jdfJob);
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		Mockito.when(this.arrebolController.getScheduler().getJobs()).thenReturn(jobs);		
		Mockito.when(this.arrebolController.getScheduler().removeJob(Mockito.eq(jdfJob.getId()))).thenReturn(jdfJob);
		
		// update DB Map
		this.arrebolController.getAllJobs(owner);
		
		Assert.assertEquals(jdfJob.getId(), this.arrebolController.stopJob(jobName, owner));
		Assert.assertEquals(jobs.size() - 1, this.arrebolController.getJobMap().size());
	}
	
	@Test
	public void testStopJobWithId() {
		String jobName = "jobName00";
		ArrayList<Job> jobs = new ArrayList<Job>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner);
		jobs.add(jdfJob);
		jobs.add(new JDFJob("", owner));
		jobs.add(new JDFJob("", owner));
		Mockito.when(this.arrebolController.getScheduler().getJobs()).thenReturn(jobs);		
		Mockito.when(this.arrebolController.getScheduler().removeJob(Mockito.eq(jdfJob.getId()))).thenReturn(jdfJob);
		Mockito.when(this.arrebolController.getScheduler().getJobById(Mockito.eq(jdfJob.getId()))).thenReturn(jdfJob);
		
		// update DB Map
		this.arrebolController.getAllJobs(owner);
		
		Assert.assertEquals(jdfJob.getId(), this.arrebolController.stopJob(jdfJob.getId(), owner));
		Assert.assertEquals(jobs.size() - 1, this.arrebolController.getJobMap().size());
	}	
	
	@Test
	public void testGetTaskById() {
		String taskId = "taskId00";
		Task task = new TaskImpl(taskId, new Specification("image", "username", "publicKey", "privateKeyFilePath", "", ""));
		
		ArrayList<Job> jobs = new ArrayList<Job>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner);
		jdfJob.run(task);
		jobs.add(jdfJob);
		Mockito.when(this.arrebolController.getScheduler().getJobs()).thenReturn(jobs);
		
		Assert.assertEquals(jobs, this.arrebolController.getAllJobs(owner));				
		Assert.assertEquals(task, this.arrebolController.getTaskById(taskId, owner));
	}
	
	@Test
	public void testGetTaskState() {
		String taskId = "taskId00";
		Task task = new TaskImpl(taskId, new Specification("image", "username", "publicKey", "privateKeyFilePath", "", ""));
		
		ArrayList<Job> jobs = new ArrayList<Job>();
		String owner = "owner";
		JDFJob jdfJob = new JDFJob("", owner);
		jdfJob.run(task);
		jobs.add(jdfJob);
		Mockito.when(this.arrebolController.getScheduler().getJobs()).thenReturn(jobs);
		
		Assert.assertEquals(jobs, this.arrebolController.getAllJobs(owner));				
		Assert.assertEquals(TaskState.RUNNING, this.arrebolController.getTaskState(taskId, owner));
	}
	
}
