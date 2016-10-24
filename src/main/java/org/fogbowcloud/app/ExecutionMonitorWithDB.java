package org.fogbowcloud.app;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.blowout.scheduler.core.model.Job;
import org.fogbowcloud.blowout.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.blowout.scheduler.core.model.Resource;
import org.fogbowcloud.blowout.scheduler.core.model.Task;
import org.fogbowcloud.blowout.scheduler.core.model.TaskProcess;
import org.fogbowcloud.blowout.scheduler.core.model.TaskProcessImpl;
import org.fogbowcloud.blowout.scheduler.core.Scheduler;
import org.fogbowcloud.app.utils.AppPropertiesConstants;
import org.fogbowcloud.blowout.scheduler.infrastructure.exceptions.InfrastructureException;
import org.mapdb.DB;

public class ExecutionMonitorWithDB implements Runnable {

	private Scheduler scheduler;
	private static final Logger LOGGER = Logger.getLogger(ExecutionMonitorWithDB.class);
	private ExecutorService service;
	private DB db;
	private ConcurrentMap<String, JDFJob> jobMap;

	public ExecutionMonitorWithDB(Scheduler scheduler, DB pendingImageDownloadDB) {
		this(scheduler, Executors.newFixedThreadPool(3), pendingImageDownloadDB);
	}

	public ExecutionMonitorWithDB(Scheduler scheduler, ExecutorService service, DB db) {
		this.scheduler = scheduler;
		if (service == null) {
			this.service = Executors.newFixedThreadPool(3);
		} else {
			this.service = service;
		}
		this.db = db;
		this.jobMap = db.getHashMap(AppPropertiesConstants.DB_MAP_NAME);
	}

	@Override
	public void run() {
		LOGGER.debug("Submitting monitoring tasks");
		for (TaskProcess tp : scheduler.getRunningProcs()) {
			LOGGER.debug("Process "+ tp.getProcessId() + " has state "+ tp.getStatus());
			service.submit(new TaskExecutionChecker(tp, this.scheduler));
		}
		for (Job aJob : scheduler.getJobs()) {
			JDFJob aJDFJob = (JDFJob) aJob;
			this.jobMap.put(aJDFJob.getId(), aJDFJob);
			this.db.commit();
		}
	}

	class TaskExecutionChecker implements Runnable {

		protected TaskProcess tp;
		protected Scheduler scheduler;
		protected Job job;

		public TaskExecutionChecker(TaskProcess tp, Scheduler scheduler) {
			this.tp = tp;
			this.scheduler = scheduler;
		}

		@Override
		public void run() {

			if (tp.getStatus().equals(TaskProcessImpl.State.FAILED)) {
				scheduler.taskFailed(tp);
				return;
			}

			if (tp.getStatus().equals(TaskProcessImpl.State.FINNISHED)) {
				scheduler.taskCompleted(tp);
				return;
			}
		}
	}
}
