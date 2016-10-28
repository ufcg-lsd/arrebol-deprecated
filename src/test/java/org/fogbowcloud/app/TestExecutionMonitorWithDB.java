package org.fogbowcloud.app;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.fogbowcloud.app.ExecutionMonitorWithDB.TaskExecutionChecker;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.app.utils.PropertiesConstants;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.infrastructure.exception.InfrastructureException;
import org.fogbowcloud.blowout.infrastructure.manager.InfrastructureManager;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.HTreeMap;

import com.amazonaws.auth.policy.Resource;

public class TestExecutionMonitorWithDB {

	public Task task;
	public BlowoutController blowout;
	public ArrebolController arrebol;
	public JDFJob job;
	public InfrastructureManager IM;
	public Resource resource;
	public String FAKE_TASK_ID = "FAKE_TASK_ID";
	private CurrentThreadExecutorService executorService;
	public DB db;
	private HTreeMap<String, JDFJob> jobDB;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {
		task = spy(new TaskImpl(FAKE_TASK_ID, null));
		IM = mock(InfrastructureManager.class);
		resource = mock(Resource.class);
		db = mock(DB.class);
		jobDB = mock(HTreeMap.class);
		doReturn(jobDB).when(db).getHashMap(PropertiesConstants.DB_MAP_NAME);
		job = mock(JDFJob.class);
		executorService = new CurrentThreadExecutorService();
		arrebol = mock(ArrebolController.class);
		blowout = mock(BlowoutController.class);
	}

	@Test
	public void testExecutionMonitor() throws InfrastructureException, InterruptedException {
		List<JDFJob> jdfJobs = new ArrayList<JDFJob>();
		doReturn("jobId").when(job).getId();
		doReturn(job).when(jobDB).put("jobId", job);
		doNothing().when(db).commit();
		
		List<Task> tasks = new ArrayList<Task>();
		tasks.add(task);
		doReturn(tasks).when(job).getTaskList();
		doReturn(TaskState.COMPLETED).when(blowout).getTaskState(FAKE_TASK_ID);
		doNothing().when(blowout).cleanTask(task);
		doNothing().when(arrebol).moveTaskToFinished(task);
		jdfJobs.add(job);
		doReturn(jdfJobs).when(jobDB).values();
		ExecutionMonitorWithDB monitor = new ExecutionMonitorWithDB(blowout, arrebol, executorService, db);
		monitor.run();
		verify(blowout).cleanTask(task);
		verify(arrebol).moveTaskToFinished(task);
	}

	@Test
	public void testExecutionMonitorTaskFails() throws InterruptedException {
		ExecutorService exec = mock(ExecutorService.class);
		ExecutionMonitorWithDB monitor = new ExecutionMonitorWithDB(blowout, arrebol,executorService, db);
		List<JDFJob> jdfJobs = new ArrayList<JDFJob>();
		doReturn("jobId").when(job).getId();
		doReturn(job).when(jobDB).put("jobId", job);
		doNothing().when(db).commit();
		
		List<Task> tasks = new ArrayList<Task>();
		tasks.add(task);
		doReturn(tasks).when(job).getTaskList();
		doReturn(TaskState.FAILED).when(blowout).getTaskState(FAKE_TASK_ID);
		jdfJobs.add(job);
		monitor.run();
		verify(blowout, never()).cleanTask(task);
		verify(arrebol, never()).moveTaskToFinished(task);
		
	}

	@Test
	public void testExecutionIsNotOver() throws InfrastructureException, InterruptedException {
		ExecutionMonitorWithDB monitor = new ExecutionMonitorWithDB(blowout, arrebol,executorService, db);
		List<JDFJob> jdfJobs = new ArrayList<JDFJob>();
		doReturn("jobId").when(job).getId();
		doReturn(job).when(jobDB).put("jobId", job);
		doNothing().when(db).commit();
		
		List<Task> tasks = new ArrayList<Task>();
		tasks.add(task);
		doReturn(tasks).when(job).getTaskList();
		doReturn(TaskState.RUNNING).when(blowout).getTaskState(FAKE_TASK_ID);
		jdfJobs.add(job);
		monitor.run();
		verify(blowout, never()).cleanTask(task);
		verify(arrebol, never()).moveTaskToFinished(task);
	}

}
