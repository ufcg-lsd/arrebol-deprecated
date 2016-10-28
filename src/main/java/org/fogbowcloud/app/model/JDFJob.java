package org.fogbowcloud.app.model;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.fogbowcloud.blowout.core.model.Job;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * It add the job name, job name and sched path to the {@link Job} abstraction.
 */
public class JDFJob extends Job {

	//FIXME: do we need this implementation?
	//FIXME: maybe, we should have name and id in the default Job abstraction.
	// Also, i think we do not need schedpath here

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
		//TODO remove
		return null;
	}

	public Task getTaskById(String taskId) {
		//TODO
		return this.getTaskList().get(taskId);
	}

	public TaskState getTaskState(String taskId) {
		//TODO remove
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
	
	public JSONObject toJSON() {
		try {
			JSONObject job = new JSONObject();
			job.put("jobId", this.getId());
			job.put("name", this.getName());
			job.put("schedPath", this.getSchedPath());
			job.put("owner", this.getId());
			job.put("uuid", this.getUUID());
			JSONArray tasks = new JSONArray();
			Map<String, Task> taskList = this.getTaskList();
			for (Entry<String, Task> entry : taskList.entrySet()) {
				tasks.put(entry.getValue().toJSON());
			}
			job.put("tasks", tasks);
			return job;
		} catch (JSONException e) {
			LOGGER.debug("Error while trying to create a JSONObject from JDFJob", e);
			return null;
		}
	}

	public static JDFJob fromJSON(JSONObject job) {
		
		return null;
	}

}
