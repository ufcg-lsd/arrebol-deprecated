package org.fogbowcloud.app;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.datastore.JobDataStore;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskState;

public class ExecutionMonitorWithDB implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ExecutionMonitorWithDB.class);

	private BlowoutController blowoutController;
	private ArrebolController arrebolController;
	private ExecutorService service;
    private ArrayList<JDFJob> jobMap;

	ExecutionMonitorWithDB(BlowoutController blowoutController,
                           ArrebolController arrebolController,
                           JobDataStore dataStore) {
		this(blowoutController, arrebolController, Executors.newFixedThreadPool(3), dataStore);
	}

	ExecutionMonitorWithDB(BlowoutController blowoutController,
                           ArrebolController arrebolController,
                           ExecutorService service,
                           JobDataStore db) {
		this.blowoutController = blowoutController;
		this.arrebolController = arrebolController;
		if (service == null) {
			this.service = Executors.newFixedThreadPool(3);
		} else {
			this.service = service;
		}
        this.jobMap = (ArrayList<JDFJob>) db.getAll();
	}

	@Override
	public void run() {
		LOGGER.debug("Submitting monitoring tasks");

		for (JDFJob aJob : jobMap) {
			LOGGER.debug("Starting monitoring of job " + aJob.getName() +
					"[" + aJob.getId() + "]."
			);
			for (Task task : aJob.getTasks()) {
				LOGGER.debug("Task: " +task +" is being treated");
				if (!task.isFinished()) {
					TaskState taskState = blowoutController.getTaskState(task.getId());
					LOGGER.debug("Process " + task.getId() + " has state " + taskState.getDesc());
					service.submit(new TaskExecutionChecker(task));
				}
			}
		}
	}

	class TaskExecutionChecker implements Runnable {

		private Task task;

		TaskExecutionChecker(Task task) {
			this.task = task;
		}

		@Override
		public void run() {
			TaskState state = blowoutController.getTaskState(task.getId());

			if (TaskState.COMPLETED.equals(state)) {
				blowoutController.cleanTask(task);
				arrebolController.moveTaskToFinished(task);
			}
		}
	}
}
