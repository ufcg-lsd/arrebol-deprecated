package org.fogbowcloud.app;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Resource;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.app.utils.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.exceptions.InfrastructureException;
import org.mapdb.DB;

public class ExecutionMonitorWithDB implements Runnable {

	private Scheduler scheduler;
	private static final Logger LOGGER = Logger.getLogger(ExecutionMonitorWithDB.class);
	private ExecutorService service;
	private DB db;
	private ConcurrentMap<String, JDFJob> jobMap;

	public ExecutionMonitorWithDB(Scheduler scheduler,DB pendingImageDownloadDB) {
		this(scheduler, Executors.newFixedThreadPool(3),pendingImageDownloadDB);
	}

	public ExecutionMonitorWithDB(Scheduler scheduler, ExecutorService service, DB db) {
		this.scheduler = scheduler;
		if(service == null){
			this.service = Executors.newFixedThreadPool(3);
		}else{
			this.service = service;
		}
		this.db = db;
		this.jobMap =db.getHashMap(AppPropertiesConstants.DB_MAP_NAME);
	}

	@Override
	public void run() {
		LOGGER.debug("Submitting monitoring tasks");
		for (Job aJob : scheduler.getJobs()){
			JDFJob aJDFJob = (JDFJob) aJob;
			LOGGER.debug("Submitting job: "+ aJDFJob.toString() );
			for (Task task : aJDFJob.getByState(TaskState.RUNNING)) {
				LOGGER.debug("Submitting taskId: "+ task.getId());
				service.submit(new TaskExecutionChecker(task, this.scheduler, aJDFJob));
			}
			this.jobMap.put(aJDFJob.getId(), aJDFJob);
			this.db.commit();
		}
	}

	class TaskExecutionChecker implements Runnable {

		protected Task task; 
		protected Scheduler scheduler;
		protected Job job;

		public TaskExecutionChecker(Task task, Scheduler scheduler, Job job){
			this.task = task;
			this.scheduler = scheduler;
			this.job = job;
		}

		@Override
		public void run() {
			LOGGER.debug("Monitoring task: " + task.getId() + " failed: " + task.isFailed() + " completed: " + task.isFinished());

			if (task.checkTimeOuted()){
				LOGGER.debug("Task: "+ task.getId() + " timed out");
				job.fail(task);
				scheduler.taskFailed(task);
				return;
			}

			if (task.isFailed()) {
				LOGGER.debug("Task: " + task.getId() + " failed");
				job.fail(task);
				scheduler.taskFailed(task);
				return;
			}

			if (task.isFinished()){
				LOGGER.debug("Task: " + task.getId() + " finished");
				job.finish(task);
				scheduler.taskCompleted(task);
				return;
			}

			try {
				if (!checkResourceConnectivity(task)){
					if (!task.mayRetry()) {
						job.fail(task);
						scheduler.taskFailed(task);
					} else {
						task.setRetries(task.getRetries() + 1);
					}
				} else {
					task.setRetries(0);
				}
			} catch (InfrastructureException e) {
				LOGGER.error("Error while checking connectivity.", e);
			}
		}

		private boolean checkResourceConnectivity(Task task) throws InfrastructureException{
			Resource resource = scheduler.getAssociateResource(task);
			return resource.checkConnectivity();
		}
	}
}
