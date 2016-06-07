package org.fogbowcloud.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.JDFTasks;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.ourgrid.common.specification.main.CompilerException;

public class ArrebolController {
	
	private static final int DEFAULT_SCHEDULER_INTERVAL = 30000;
	private static final int DEFAULT_EXECUTION_MONITOR_INTERVAL = 30000;

	public static final Logger LOGGER = Logger.getLogger(ArrebolController.class);
	
	private DB jobDB;
	private Scheduler scheduler;
	private Properties properties;	
	private ConcurrentMap<String, JDFJob> jobMap;
	
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));	
	
	public ArrebolController(Properties properties) throws Exception {
		this.properties = properties;
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	protected void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	protected Scheduler getScheduler() {
		return scheduler;
	}
	
	protected DB getJobDB() {
		return jobDB;
	}
	
	protected ConcurrentMap<String, JDFJob> getJobMap() {
		return jobMap;
	}
	
	public void init() throws Exception {
		final File pendingImageDownloadFile = new File(AppPropertiesConstants.DB_FILE_NAME);
		final DB pendingImageDownloadDB = DBMaker.newFileDB(pendingImageDownloadFile).make();
		pendingImageDownloadDB.checkShouldCreate(AppPropertiesConstants.DB_MAP_NAME);
		ConcurrentMap<String, JDFJob> jobMapDB = pendingImageDownloadDB.getHashMap(AppPropertiesConstants.DB_MAP_NAME);
		
		Boolean blockWhileInitializing = new Boolean(this.properties.getProperty(
				AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING)).booleanValue();
		Boolean isElastic = new Boolean(properties.getProperty(
				AppPropertiesConstants.INFRA_IS_STATIC)).booleanValue();		
		
		InfrastructureManager infraManager = getInfraManager(blockWhileInitializing, isElastic);
		
		ArrayList<JDFJob> legacyJobs = getLegacyJobs(jobMapDB);
		LOGGER.debug("Properties: " + properties.getProperty(
				AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH));
			
		this.scheduler = new Scheduler(infraManager, legacyJobs.toArray(new JDFJob[legacyJobs.size()]));		
		LOGGER.debug("Application to be started on port: " + properties
				.getProperty(AppPropertiesConstants.REST_SERVER_PORT));
		ExecutionMonitorWithDB executionMonitor = new ExecutionMonitorWithDB(this.scheduler, pendingImageDownloadDB);		
		
        this.jobDB = pendingImageDownloadDB;
        this.jobMap = this.jobDB.getHashMap(AppPropertiesConstants.DB_MAP_NAME);		
		
		LOGGER.debug("Starting Scheduler and Execution Monitor, execution monitor period: " + properties
				.getProperty(AppPropertiesConstants.EXECUTION_MONITOR_PERIOD));
		schedulerTimer.scheduleAtFixedRate(this.scheduler, 0, DEFAULT_SCHEDULER_INTERVAL);
		executionMonitorTimer.scheduleAtFixedRate(executionMonitor, 0, DEFAULT_EXECUTION_MONITOR_INTERVAL);		
	}

	protected InfrastructureManager getInfraManager(Boolean blockWhileInitializing,
			Boolean isElastic) throws Exception {
		InfrastructureProvider infraProvider = createInfraProvaiderInstance();
		InfrastructureManager infraManager = new InfrastructureManager(null, isElastic, 
				infraProvider, properties);
		infraManager.start(blockWhileInitializing);					
		return infraManager;
	}

	private ArrayList<JDFJob> getLegacyJobs(ConcurrentMap<String, JDFJob> jobMapDB) {
		ArrayList<JDFJob> legacyJobs = new ArrayList<JDFJob>();
		
		for (String key : jobMapDB.keySet()) {
			JDFJob recoveredJob = (JDFJob) jobMapDB.get(key);
			for (Task task : recoveredJob.getByState(TaskState.RUNNING)) {
				recoveredJob.restart(task);
			}
			legacyJobs.add((JDFJob) jobMapDB.get(key));
		}
		return legacyJobs;
	}
	
	private InfrastructureProvider createInfraProvaiderInstance() throws Exception {
		String providerClassName = this.properties.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);
		Class<?> forName = Class.forName(providerClassName);
		Object clazz = forName.getConstructor(Properties.class).newInstance(properties);
		if (!(clazz instanceof InfrastructureProvider)) {
			throw new Exception("Provider Class Name is not a InfrastructureProvider implementation");
		}

		return (InfrastructureProvider) clazz;
	}	
	
    public JDFJob getJobById(String jobId) {
        return (JDFJob) this.scheduler.getJobById(jobId);
    }

    public String addJob(String jdfFilePath, String schedPath) throws CompilerException {
        return addJob(jdfFilePath, schedPath, "");
    }

    public String addJob(String jdfFilePath, String schedPath, String friendlyName) throws CompilerException {
        JDFJob job = new JDFJob(schedPath, friendlyName);

        List<Task> taskList = getTasksFromJDFFile(jdfFilePath, schedPath, job);

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

	public void updateJob(JDFJob job) {
        this.jobMap.put(job.getId(), job);
        this.jobDB.commit();
    }

    public String stopJob(String jobReference) {
        Job jobToRemove = getJobByName(jobReference);
        if (jobToRemove != null) {
            this.jobMap.remove(jobToRemove.getId());
            this.jobDB.commit();
			return scheduler.removeJob(jobToRemove.getId()).getId();
        } else {
            jobToRemove = getJobById(jobReference);
            if (jobToRemove != null) {
                this.jobMap.remove(jobReference);
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
        for (Job job : this.scheduler.getJobs()) {
            if (jobName.equals(((JDFJob) job).getName())) {
                return (JDFJob) job;
            }
        }
        return null;
    }
    
	public Task getTaskById(String taskId){
		for (Job job : getAllJobs()){
			JDFJob jdfJob = (JDFJob) job;
			Task task = jdfJob.getTaskById(taskId);
			if (task != null) {
				return task;
			}
		}
		return null;
	}
	
	public TaskState getTaskState(String taskId) {
		for (Job job : getAllJobs()){
			JDFJob jdfJob = (JDFJob) job;
			TaskState taskState = jdfJob.getTaskState(taskId);
			if (taskState != null) {
				return taskState;
			}
		}
		return null;
	}	
    
    protected List<Task> getTasksFromJDFFile(String jdfFilePath, String schedPath,
    		JDFJob job) throws CompilerException {
    	List<Task> taskList = JDFTasks.getTasksFromJDFFile(job.getId(), jdfFilePath, schedPath, this.properties);
    	return taskList;
    }
	
}
