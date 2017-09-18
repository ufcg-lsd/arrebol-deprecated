package org.fogbowcloud.app;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.fogbowcloud.app.datastore.JobDataStore;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.core.model.TaskProcess;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.blowout.infrastructure.exception.InfrastructureException;
import org.fogbowcloud.blowout.infrastructure.manager.InfrastructureManager;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.auth.policy.Resource;
import org.mockito.Mockito;

public class TestExecutionMonitorWithDB {

	private static final String FAKE_UUID = "1234";
	private Task task;
	private BlowoutController blowout;
	private ArrebolController arrebol;
	private JDFJob job;
	private String FAKE_TASK_ID = "FAKE_TASK_ID";
	private CurrentThreadExecutorService executorService;
	private JobDataStore db;
	private HashMap<String, JDFJob> jobDB;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {
		task = spy(new TaskImpl(FAKE_TASK_ID, null, FAKE_UUID));
		db = mock(JobDataStore.class);
		jobDB = mock(HashMap.class);
		job = mock(JDFJob.class);
		executorService = new CurrentThreadExecutorService();
		arrebol = mock(ArrebolController.class);
		blowout = mock(BlowoutController.class);
	}

	@Test
	public void testExecutionMonitor() throws InfrastructureException, InterruptedException {
		List<JDFJob> jdfJobs = new ArrayList<>();
		doReturn("jobId").when(job).getId();
		doReturn(job).when(jobDB).put("jobId", job);
		
		ArrayList<Task> tasks = new ArrayList<>();
		tasks.add(task);
		doReturn(tasks).when(job).getTasks();
		doReturn(TaskState.COMPLETED).when(arrebol).getTaskState(Mockito.anyString(), Mockito.anyString());
		doNothing().when(arrebol).moveTaskToFinished(task);
		jdfJobs.add(job);
		doReturn(jdfJobs).when(db).getAll();
		ExecutionMonitorWithDB monitor = new ExecutionMonitorWithDB(arrebol, executorService, db);
		monitor.run();
		verify(arrebol).moveTaskToFinished(task);
		TaskProcess tp = mock(TaskProcess.class);
		List<TaskProcess> processes = new ArrayList<>();
		processes.add(tp);
		doReturn(TaskState.FINNISHED).when(tp).getStatus();
	}

	@Test
	public void testExecutionMonitorTaskFails() throws InterruptedException {
		List<JDFJob> jdfJobs = new ArrayList<>();
		doReturn("jobId").when(job).getId();
		doReturn(job).when(jobDB).put("jobId", job);
		
		ArrayList<Task> tasks = new ArrayList<>();
		tasks.add(task);
		doReturn(tasks).when(job).getTasks();
		doReturn(TaskState.FAILED).when(arrebol).getTaskState(Mockito.anyString(), Mockito.anyString());
		jdfJobs.add(job);
		doReturn(jdfJobs).when(db).getAll();
		ExecutionMonitorWithDB monitor = new ExecutionMonitorWithDB(arrebol,executorService, db);
		monitor.run();
		verify(arrebol, never()).moveTaskToFinished(task);
		
		TaskProcess tp = mock(TaskProcess.class);
		List<TaskProcess> processes = new ArrayList<>();
		processes.add(tp);
		doNothing().when(job).finish(task);
	}

	@Test
	public void testExecutionIsNotOver() throws InfrastructureException, InterruptedException {
		List<JDFJob> jdfJobs = new ArrayList<>();
		doReturn("jobId").when(job).getId();
		doReturn(job).when(jobDB).put("jobId", job);
		
		ArrayList<Task> tasks = new ArrayList<>();
		tasks.add(task);
		doReturn(tasks).when(job).getTasks();
		doReturn(TaskState.RUNNING).when(arrebol).getTaskState(Mockito.anyString(), Mockito.anyString());
		jdfJobs.add(job);
		doReturn(jdfJobs).when(db).getAll();
		ExecutionMonitorWithDB monitor = new ExecutionMonitorWithDB(arrebol,executorService, db);
		monitor.run();
		verify(arrebol, never()).moveTaskToFinished(task);
		TaskProcess tp = mock(TaskProcess.class);
		doReturn(TaskState.RUNNING).when(tp).getStatus();
		List<TaskProcess> processes = new ArrayList<>();
		processes.add(tp);
	}

}
