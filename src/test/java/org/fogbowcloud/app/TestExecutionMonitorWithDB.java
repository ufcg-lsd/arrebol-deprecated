package org.fogbowcloud.app;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.util.thread.Scheduler;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.core.model.TaskProcess;
import org.fogbowcloud.blowout.core.model.TaskProcessImpl;
import org.fogbowcloud.blowout.core.util.AppPropertiesConstants;
import org.fogbowcloud.blowout.infrastructure.exception.InfrastructureException;
import org.fogbowcloud.blowout.infrastructure.manager.InfrastructureManager;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.HTreeMap;

import com.amazonaws.auth.policy.Resource;

public class TestExecutionMonitorWithDB {

	public Task task;
	public Scheduler scheduler;
	public JDFJob job;
	public InfrastructureManager IM;
	public Resource resource;
	public String FAKE_TASK_ID = "FAKE_TASK_ID";
	private CurrentThreadExecutorService executorService;
	private DB db;
	private HTreeMap<String, JDFJob> jobDB;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp(){
		task = spy(new TaskImpl(FAKE_TASK_ID, null));
		IM = mock(InfrastructureManager.class);
		resource = mock(Resource.class);
		db = mock(DB.class);
		jobDB = mock(HTreeMap.class);

		job = mock(JDFJob.class);
		executorService = new CurrentThreadExecutorService();
		scheduler = spy(new Scheduler(IM, job));
	}

	@Test
	public void testExecutionMonitor() throws InfrastructureException, InterruptedException {
		doReturn(jobDB).when(db).getHashMap(AppPropertiesConstants.DB_MAP_NAME);
		doNothing().when(db).commit();
		doReturn(job).when(jobDB).put(eq("FAKE_JOB_ID"), eq(job));
		ExecutionMonitor executionMonitor = new ExecutionMonitor(scheduler, executorService, job);
		TaskProcess tp = mock(TaskProcess.class);
		List<TaskProcess> processes = new ArrayList<TaskProcess>();
		processes.add(tp);
		doReturn(processes).when(scheduler).getRunningProcs();
		doReturn(TaskProcessImpl.State.FINNISHED).when(tp).getStatus();
		doNothing().when(scheduler).taskCompleted(tp);
		executionMonitor.run();
		Thread.sleep(500);
		verify(tp, times(2)).getStatus();
	}

	@Test
	public void testExecutionMonitorTaskFails() throws InterruptedException {
		doReturn(jobDB).when(db).getHashMap(AppPropertiesConstants.DB_MAP_NAME);
		doNothing().when(db).commit();
		doReturn(job).when(jobDB).put(eq("FAKE_JOB_ID"), eq(job));
		ExecutionMonitor executionMonitor = new ExecutionMonitor(scheduler, executorService, job);
		TaskProcess tp = mock(TaskProcess.class);
		List<TaskProcess> processes = new ArrayList<TaskProcess>();
		processes.add(tp);
		doReturn(processes).when(scheduler).getRunningProcs();
		doReturn(TaskProcessImpl.State.FAILED).when(tp).getStatus();
		doNothing().when(scheduler).taskCompleted(tp);
		doNothing().when(job).finish(task);
		executionMonitor.run();
		Thread.sleep(500);
		verify(tp).getStatus();
	}

	@Test
	public void testExecutionIsNotOver() throws InfrastructureException, InterruptedException {
		doReturn(jobDB).when(db).getHashMap(AppPropertiesConstants.DB_MAP_NAME);
		doNothing().when(db).commit();
		doReturn(job).when(jobDB).put(eq("FAKE_JOB_ID"), eq(job));
		ExecutionMonitor executionMonitor = new ExecutionMonitor(scheduler, executorService, job);
		TaskProcess tp = mock(TaskProcess.class);
		doReturn(TaskProcessImpl.State.RUNNING).when(tp).getStatus();
		List<TaskProcess> processes = new ArrayList<TaskProcess>();
		processes.add(tp);
		doReturn(processes).when(scheduler).getRunningProcs();
		executionMonitor.run();
		verify(tp, times(2)).getStatus();
		verify(scheduler, never()).taskCompleted(tp);
	}

}
