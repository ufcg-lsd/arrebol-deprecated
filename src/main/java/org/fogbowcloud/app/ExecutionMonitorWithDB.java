package org.fogbowcloud.app;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.Job;
import org.fogbowcloud.app.utils.PropertiesConstants;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.mapdb.DB;

public class ExecutionMonitorWithDB implements Runnable {

	private BlowoutController blowoutController;
	private ArrebolController arrebolController;
	private static final Logger LOGGER = Logger.getLogger(ExecutionMonitorWithDB.class);
	private ExecutorService service;
	private DB db;
	private ConcurrentMap<String, JDFJob> jobMap;

	public ExecutionMonitorWithDB(BlowoutController blowoutController, ArrebolController arrebolController,
			DB pendingImageDownloadDB) {
		this(blowoutController, arrebolController, Executors.newFixedThreadPool(3), pendingImageDownloadDB);
	}

	public ExecutionMonitorWithDB(BlowoutController blowoutController, ArrebolController arrebolController,
			ExecutorService service, DB db) {
		this.blowoutController = blowoutController;
		this.arrebolController = arrebolController;
		if (service == null) {
			this.service = Executors.newFixedThreadPool(3);
		} else {
			this.service = service;
		}
		this.db = db;
		this.jobMap = db.getHashMap(PropertiesConstants.DB_MAP_NAME);
	}

	@Override
	public void run() {
		LOGGER.debug("Submitting monitoring tasks");

		for (JDFJob aJob : jobMap.values()) {
			JDFJob aJDFJob = (JDFJob) aJob;
			this.jobMap.put(aJDFJob.getId(), aJDFJob);
			this.db.commit();

			for (Task task : aJob.getTasks()) {
				if (!task.isFinished()) {
					TaskState taskState = blowoutController.getTaskState(task.getId());
					LOGGER.debug("Process " + task.getId() + " has state " + taskState.getDesc());
					service.submit(new TaskExecutionChecker(task));
				}
			}
		}
	}

	class TaskExecutionChecker implements Runnable {

		protected Task task;
		protected Job job;

		public TaskExecutionChecker(Task task) {
			this.task = task;
		}

		@Override
		public void run() {

			TaskState state = blowoutController.getTaskState(task.getId());

			if (TaskState.COMPLETED.equals(state)) {
				blowoutController.cleanTask(task);
				arrebolController.moveTaskToFinished(task);
				return;
			}
		}
	}
}
