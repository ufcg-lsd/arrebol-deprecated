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
import org.ourgrid.common.specification.main.CompilerException;
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

public class JDFSchedulerApplication extends Application {

    private final Properties properties;
    private final Scheduler scheduler;
    private final DB jobDB;
    private final ConcurrentMap<String, JDFJob> jobMap;

    private Component restletComponent;

    private static final Logger LOGGER = Logger.getLogger(JDFSchedulerApplication.class);

    private int restServerPort;

    public JDFSchedulerApplication(Scheduler scheduler, Properties properties, DB jobDB) {

        //FIXME: remember to handle the IAE exception in ArrebolMain

        //FIXME: load properties in a single method
        //FIXME: raise error if we miss any important property

        //FIXME; I do not like to pass the properties around I guess at this point we should use the variables.
        // The problem is that task creation abuses using it
        
        if (scheduler == null) {
            throw new IllegalArgumentException("scheduler cannot be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties cannot be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("jobDB cannot be null");
        }

        this.properties = properties;
        loadProperties();

        this.scheduler = scheduler;

        this.jobDB = jobDB;
        this.jobMap = this.jobDB.getHashMap(AppPropertiesConstants.DB_MAP_NAME);
    }

    private void loadProperties() {

        if (!properties.containsKey(AppPropertiesConstants.REST_SERVER_PORT)) {
            throw new IllegalArgumentException(AppPropertiesConstants.REST_SERVER_PORT + "is missing on properties.");
        }
        this.restServerPort = Integer.valueOf((String) properties.get(AppPropertiesConstants.REST_SERVER_PORT));
    }

    public void startServer() throws Exception {

        LOGGER.info("Starting service on port: " + restServerPort);

        ConnectorService corsService = new ConnectorService();
        this.getServices().add(corsService);

        restletComponent = new Component();
        restletComponent.getServers().add(Protocol.HTTP, restServerPort);
        restletComponent.getDefaultHost().attach(this);

        restletComponent.start();
    }

    public void stopServer() throws Exception {
        //FIXME: it is odd nobody is calling this
        restletComponent.stop();
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

    public JDFJob getJobById(String jobId) {
        return (JDFJob) this.scheduler.getJobById(jobId);
    }

    public String addJob(String jdfFilePath, String schedPath) throws CompilerException {
        return addJob(jdfFilePath, schedPath, "");
    }

    public String addJob(String jdfFilePath, String schedPath, String friendlyName) throws CompilerException {

        JDFJob job = new JDFJob(schedPath, friendlyName);

        List<Task> taskList = JDFTasks.getTasksFromJDFFile(job.getId(), jdfFilePath, schedPath, properties);

        for (Task task : taskList) {
            job.addTask(task);
        }

        this.scheduler.addJob(job);
        return job.getId();
    }

    public ArrayList<JDFJob> getAllJobs() {
        ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();
        for (Job job : this.scheduler.getJobs()) {
            jobList.add((JDFJob) job);
            updateJob((JDFJob) job);
        }
        return jobList;
    }

    private void updateJob(JDFJob job) {
        this.jobMap.put(job.getId(), job);
        this.jobDB.commit();
    }

    public String stopJob(String jobId) {
        Job jobToRemove = getJobByName(jobId);
        if (jobToRemove != null) {
            jobMap.remove(jobId);
            this.jobDB.commit();
            return scheduler.removeJob(jobToRemove.getId()).getId();
        } else {
            jobToRemove = getJobById(jobId);
            if (jobToRemove != null) {
                jobMap.remove(jobId);
                this.jobDB.commit();
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
