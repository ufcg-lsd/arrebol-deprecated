package org.fogbowcloud.app.restlet;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.JDFTasks;
import org.fogbowcloud.app.resource.JobResource;
import org.fogbowcloud.app.resource.TaskResource4JDF;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.mapdb.DB;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.service.ConnectorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

public class JDFSchedulerApplicationWithPersistence extends Application {

    private Properties properties;
    private Scheduler scheduler;
    private DB db;
    private ConcurrentMap<String, JDFJob> jobMap;

    public static final Logger LOGGER = Logger.getLogger(JDFSchedulerApplicationWithPersistence.class);

    private Component c;

    public JDFSchedulerApplicationWithPersistence(Scheduler scheduler, Properties properties, DB pendingImageDownloadDB) {
        this.properties = properties;
        this.scheduler = scheduler;
        this.db = pendingImageDownloadDB;
        this.jobMap = db.getHashMap(AppPropertiesConstants.DB_MAP_NAME);
    }


    public void startServer() throws Exception {
        LOGGER.debug("Just Starting JDF Application");
        ConnectorService corsService = new ConnectorService();

        this.getServices().add(corsService);
        LOGGER.debug("Starting application on port: " + properties.getProperty(AppPropertiesConstants.REST_SERVER_PORT));
        c = new Component();
        int port = Integer.parseInt(properties.getProperty(AppPropertiesConstants.REST_SERVER_PORT));
        c.getServers().add(Protocol.HTTP, port);
        c.getDefaultHost().attach(this);
        LOGGER.debug("Starting JDF Application");
        c.start();


    }

    public void stopServer() throws Exception {
        c.stop();
    }

    @Override
    public Restlet createInboundRoot() {

        Router router = new Router(getContext());
        router.attach("/arrebol/job/ui", JobEndpoint.class);
        router.attach("/arrebol/job", JobResource.class);
        router.attach("/arrebol/job/{jobpath}", JobResource.class);
        router.attach("/arrebol/task/{taskId}", TaskResource4JDF.class);
        router.attach("/arrebol/task/{taskId}/{varName}", TaskResource4JDF.class);

        return router;
    }

    public Properties getProperties() {
        return properties;
    }


    public JDFJob getJobById(String jobId) {
        return (JDFJob) this.scheduler.getJobById(jobId);
    }

    public String addJob(String jdfFilePath, String schedPath) {
        return addJob(jdfFilePath, schedPath, "");
    }

    public String addJob(String jdfFilePath, String schedPath, String friendlyName) {

        JDFJob job = new JDFJob(schedPath, friendlyName);

        List<Task> taskList = getTasksFromJDFFile(job.getId(), jdfFilePath, schedPath, properties);
        for (Task task : taskList) {
            job.addTask(task);
        }

        this.scheduler.addJob(job);
        return job.getId();
    }

    public List<Task> getTasksFromJDFFile(String jobId, String jdfFilePath, String schedPath, Properties properties) {
        return JDFTasks.getTasksFromJDFFile(jobId, jdfFilePath, schedPath, properties);
    }


    public ArrayList<JDFJob> getAllJobs() {
        ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
        for (Job job : this.scheduler.getJobs()) {
            jobList.add((JDFJob) job);
            updateJob((JDFJob) job);
        }
        return jobList;
    }

    public Task getTaskById(String taskId) {
        for (Job job : getAllJobs()) {
            JDFJob jdfJob = (JDFJob) job;
            Task task = jdfJob.getTaskById(taskId);
            if (task != null) {
                return task;
            }
        }
        return null;
    }

    public void updateJob(JDFJob job) {
        this.jobMap.put(job.getId(), job);
        this.db.commit();
    }


    public TaskState getTaskState(String taskId) {
        for (Job job : getAllJobs()) {
            JDFJob jdfJob = (JDFJob) job;
            TaskState taskState = jdfJob.getTaskState(taskId);
            if (taskState != null) {
                return taskState;
            }
        }
        return null;
    }


    public String stopJob(String jobId) {
        Job jobToRemove = getJobByName(jobId);
        if (jobToRemove != null) {
            jobMap.remove(jobId);
            this.db.commit();
            return scheduler.removeJob(jobToRemove.getId()).getId();
        } else {
            jobToRemove = getJobById(jobId);
            if (jobToRemove != null) {
                jobMap.remove(jobId);
                this.db.commit();
                return scheduler.removeJob(jobToRemove.getId()).getId();
            }
        }
        return null;
    }

    public JDFJob getJobByName(String jobName) {
        if (jobName == null) {
            return null;
        }
        for (Job job : scheduler.getJobs()) {
            if (jobName.equals(((JDFJob) job).getName())) {
                return (JDFJob) job;
            }
        }
        return null;
    }
}
