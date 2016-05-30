package org.fogbowcloud.app.model;

import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Task;

import java.util.UUID;

/**
 * It add the job name, job name and sched path to the {@link Job} abstraction.
 */
public class JDFJob extends Job {

    private final String jobId;
    private final String name;
    private String schedPath;

    public JDFJob(String schedPath) {
        this(schedPath, "");
    }

    public JDFJob(String schedPath, String jobName) {
        super();
        this.schedPath = schedPath;
        this.name = jobName;
        this.jobId = UUID.randomUUID().toString();
    }

    public String getId() {
        return jobId;
    }

    public String getName() {
        return this.name;
    }

    public String getSchedPath() {
        //FIXME: it is odd nobody uses it. Do we really need it?
        return this.schedPath;
    }

    @Override
    public void run(Task task) {
        tasksReady.remove(task);
        tasksRunning.add(task);

    }

    public void restart(Task task) {
        tasksRunning.remove(task);
        tasksReady.add(task);
    }

    @Override
    public void finish(Task task) {
        tasksRunning.remove(task);
        tasksCompleted.add(task);
    }

    @Override
    public void fail(Task task) {
        tasksRunning.remove(task);
        tasksFailed.add(task);
    }

    public Task getCompletedTask(String taskId) {
        for (Task task : this.tasksCompleted) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    public Task getTaskById(String taskId) {
        for (Task task : this.tasksCompleted) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        for (Task task : this.tasksFailed) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        for (Task task : this.tasksRunning) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        for (Task task : this.tasksReady) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    public TaskState getTaskState(String taskId) {
        for (Task task : this.tasksCompleted) {
            if (task.getId().equals(taskId)) {
                return TaskState.COMPLETED;
            }
        }
        for (Task task : this.tasksFailed) {
            if (task.getId().equals(taskId)) {
                return TaskState.FAILED;
            }
        }
        for (Task task : this.tasksRunning) {
            if (task.getId().equals(taskId)) {
                return TaskState.RUNNING;
            }
        }
        for (Task task : this.tasksReady) {
            if (task.getId().equals(taskId)) {
                return TaskState.READY;
            }
        }
        return null;
    }

}