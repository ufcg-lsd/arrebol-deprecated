package org.fogbowcloud.app.restlet;

import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.ArrebolController;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.resource.JobEndpoint;
import org.fogbowcloud.app.resource.JobResource;
import org.fogbowcloud.app.resource.TaskResource4JDF;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.ourgrid.common.specification.main.CompilerException;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.service.ConnectorService;

public class JDFSchedulerApplication extends Application {

	private ArrebolController arrebolController;
	private Component restletComponent;
	private static final Logger LOGGER = Logger.getLogger(JDFSchedulerApplication.class);
    
    public JDFSchedulerApplication(ArrebolController arrebolController) throws Exception {
    	this.arrebolController = arrebolController;
    	this.arrebolController.init();
    }

    public void startServer() throws Exception {
        Properties properties = this.arrebolController.getProperties();
		if (!properties.containsKey(AppPropertiesConstants.REST_SERVER_PORT)) {
            throw new IllegalArgumentException(AppPropertiesConstants.REST_SERVER_PORT + " is missing on properties.");
        }
        Integer restServerPort = Integer.valueOf((String) properties.get(AppPropertiesConstants.REST_SERVER_PORT));
    	
        LOGGER.info("Starting service on port: " + restServerPort);

        ConnectorService corsService = new ConnectorService();
        this.getServices().add(corsService);

        this.restletComponent = new Component();
        this.restletComponent.getServers().add(Protocol.HTTP, restServerPort);
        this.restletComponent.getDefaultHost().attach(this);

        this.restletComponent.start();
    }

    public void stopServer() throws Exception {
    	this.restletComponent.stop();
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
    	return this.arrebolController.getJobById(jobId);
    }

    public String addJob(String jdfFilePath, String schedPath) throws CompilerException {
    	return this.arrebolController.addJob(jdfFilePath, schedPath);
    }

    public String addJob(String jdfFilePath, String schedPath, String friendlyName) throws CompilerException {
    	return this.arrebolController.addJob(jdfFilePath, schedPath, friendlyName);
    }

    public ArrayList<JDFJob> getAllJobs() {
    	return this.arrebolController.getAllJobs();
    }

    public String stopJob(String jobId) {
    	return this.arrebolController.stopJob(jobId);
    }

    public JDFJob getJobByName(String jobName) {
    	return this.arrebolController.getJobByName(jobName);
    }
    
    public Task getTaskById(String taskId) {
    	return this.arrebolController.getTaskById(taskId);
    }
    
    public TaskState getTaskState(String taskId) {
    	return this.arrebolController.getTaskState(taskId);
    }
}
