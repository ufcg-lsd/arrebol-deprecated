package org.fogbowcloud.app.model;

import org.fogbowcloud.blowout.scheduler.core.model.Job;
import org.fogbowcloud.blowout.scheduler.core.model.Task;

import java.util.UUID;

/**
 * It add the job name, job name and sched path to the {@link Job} abstraction.
 */
public class JDFJob extends Job {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7780896231796955706L;
	private final String jobId;
	private String name;
	private String schedPath;
	private final String owner;

	public JDFJob(String schedPath, String owner) {
		super();
		this.schedPath = schedPath;
		this.jobId = UUID.randomUUID().toString();
		this.owner = owner;
	}

	public String getId() {
		return jobId;
	}

	public String getName() {
		return this.name;
	}

	public String getSchedPath() {
		return this.schedPath;
	}

	public String getOwner() {
		return this.owner;
	}

	public Task getCompletedTask(String taskId) {
		//TODO
		return null;
	}

	public Task getTaskById(String taskId) {
		//TODO
		return this.taskList.get(taskId);
	}

	public TaskState getTaskState(String taskId) {
		//TODO
		return null;
	}

	public void setFriendlyName(String name) {
		this.name = name;
	}

	@Override
	public void finish(Task task) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fail(Task task) {
		// TODO Auto-generated method stub
		
	}

}