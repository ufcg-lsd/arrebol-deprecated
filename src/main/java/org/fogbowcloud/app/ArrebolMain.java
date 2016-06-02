package org.fogbowcloud.app;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.scheduler.core.ManagerTimer;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureManager;
import org.fogbowcloud.scheduler.infrastructure.InfrastructureProvider;
import org.mapdb.DB;
import org.mapdb.DBMaker;
/**
 * Arrebol entry point
 */
public class ArrebolMain {

	public static final Logger LOGGER = Logger.getLogger(ArrebolMain.class);

	//FIXME: there is no need to declare these variables as class members. everything should be local to the main method
	private static boolean blockWhileInitializing;
	private static boolean isElastic;

	public static final String PUBLIC_KEY_CONSTANT = "public_key";
	
	private static final String PRIVATE_KEY_FILEPATH = "private_key_filepath";

	private static final String LOCAL_OUTPUT_FOLDER = "local_output";

	private static final String REMOTE_OUTPUT_FOLDER = "remote_output_folder";
	
	//FIXME: two things: i) i wish we can remove all these threading complexity;
	// ii) at least, move them to the bussiness logic code, not the ArrebolMain (it should handle mainly args parsing)
	private static ManagerTimer executionMonitorTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));
	private static ManagerTimer schedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1));

	//FIXME: move to the bussiness logic?
	private static ConcurrentMap<String, JDFJob> jobMapDB;
	
	private static Properties properties;

	/**
	 * //FIXME: doc the args instead of this JDF stuff
	 *
	 * This method receives a JDF file as input and requests the mapping of
	 * its attributes to JDL attributes, generating a JDL file at the end
	 * @param args 
	 * @throws Exception 
	 */
	public static void main( String[ ] args ) throws Exception {
		properties = new Properties();
		//FIXME: assign args[0] to a variable with a proper name
		//FIXME: input is a bad name
		//FIXME: handle file opening problems
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		//FIXME: use a constant to scheduler_conf_path
		//FIXME: now I think that using a ref to a file within another config file is not ok. instead, use two file directly as args
		FileInputStream schedconfiguration = new FileInputStream(properties.getProperty("scheduler_conf_path"));
		properties.load(schedconfiguration);
		if (!checkProperties(properties)) {
			System.err.println("Missing required property, check Log for more information");
			System.exit(1);
		}

		//FIXME: this methods is odd
		loadConfigFromProperties();
		

		// Initialize a MapDB database
		//FIXME: move this block to a method
		final File pendingImageDownloadFile = new File(AppPropertiesConstants.DB_FILE_NAME);
		final DB pendingImageDownloadDB = DBMaker.newFileDB(pendingImageDownloadFile).make();
		pendingImageDownloadDB.checkShouldCreate(AppPropertiesConstants.DB_MAP_NAME);
		jobMapDB = pendingImageDownloadDB.getHashMap(AppPropertiesConstants.DB_MAP_NAME);
		
		InfrastructureProvider infraProvider = createInfraProvaiderInstance();
		InfrastructureManager infraManager = new InfrastructureManager(null, isElastic, infraProvider,
				properties);
		infraManager.start(blockWhileInitializing);

		//FIXME: this block should be a method as well
		ArrayList<JDFJob> legacyJobs = new ArrayList<JDFJob>();
		
		for (String key : jobMapDB.keySet()) {
			JDFJob recoveredJob = (JDFJob) jobMapDB.get(key);
			for (Task task : recoveredJob.getByState(TaskState.RUNNING)) {
				recoveredJob.restart(task);
			}
			legacyJobs.add((JDFJob) jobMapDB.get(key));
		}

		LOGGER.info("init spec path: " + properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_FILE_PATH));
			
		Scheduler scheduler = new Scheduler(infraManager, legacyJobs.toArray(new JDFJob[legacyJobs.size()]));

		
		LOGGER.info("rest server port: " + properties.getProperty(AppPropertiesConstants.REST_SERVER_PORT));
		ExecutionMonitorWithDB executionMonitor = new ExecutionMonitorWithDB(scheduler, pendingImageDownloadDB);
		JDFSchedulerApplication app = new JDFSchedulerApplication(scheduler, properties, pendingImageDownloadDB);
		app.startServer();

		
		//FIXME: it is not using the property value
		LOGGER.info("monitor period: " + properties.getProperty(AppPropertiesConstants.EXECUTION_MONITOR_PERIOD));
		schedulerTimer.scheduleAtFixedRate(scheduler, 0, 30000);
		executionMonitorTimer.scheduleAtFixedRate(executionMonitor, 0, 30000);
	}

	private static void loadConfigFromProperties() {

		blockWhileInitializing = new Boolean(
				properties.getProperty(AppPropertiesConstants.INFRA_INITIAL_SPECS_BLOCK_CREATING)).booleanValue();
		isElastic = new Boolean(properties.getProperty(AppPropertiesConstants.INFRA_IS_STATIC)).booleanValue();

	}

	private static InfrastructureProvider createInfraProvaiderInstance() throws Exception {

		String providerClassName = properties.getProperty(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME);

		Object clazz = Class.forName(providerClassName).getConstructor(Properties.class).newInstance(properties);
		if (!(clazz instanceof InfrastructureProvider)) {
			throw new Exception("Provider Class Name is not a InfrastructureProvider implementation");
		}

		return (InfrastructureProvider) clazz;
	}
	

	private static boolean checkProperties(Properties properties) {
		if (!properties.containsKey(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME)){
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME + " was not set");
			return false;
		};
		if(!properties.containsKey(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME)){
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME + " was not set");
			return false;
		};

		if (!properties.containsKey(AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME)){
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_RESOURCE_SERVICE_TIME + " was not set");
			return false;
		};

		if (!properties.containsKey(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME)){
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME + " was not set");
			return false;
		};

		if (!properties.containsKey(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT)){
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT + " was not set");
			return false;
		};

		if (!properties.containsKey(AppPropertiesConstants.REST_SERVER_PORT)){
			LOGGER.error("Required property " + AppPropertiesConstants.REST_SERVER_PORT + " was not set");
			return false;
		};
		if (!properties.containsKey(AppPropertiesConstants.EXECUTION_MONITOR_PERIOD)){
			LOGGER.error("Required property " + AppPropertiesConstants.EXECUTION_MONITOR_PERIOD + " was not set");
			return false;
		};
		if (!properties.containsKey(AppPropertiesConstants.INFRA_IS_STATIC)){
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_IS_STATIC + " was not set");
			return false;
		};
		if (!properties.containsKey(AppPropertiesConstants.INFRA_FOGBOW_USERNAME)) {
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_FOGBOW_USERNAME + " was not set");
			return false;
		}
		if (!properties.containsKey(PUBLIC_KEY_CONSTANT)){
			LOGGER.error("Required property " + PUBLIC_KEY_CONSTANT + " was not set");
			return false;
		}
		if (!properties.containsKey(PRIVATE_KEY_FILEPATH)){
			LOGGER.error("Required property " + PRIVATE_KEY_FILEPATH + " was not set");
			return false;
		}
		if (!properties.containsKey(REMOTE_OUTPUT_FOLDER)){
			LOGGER.error("Required property " + REMOTE_OUTPUT_FOLDER + " was not set");
			return false;
		}
		
		if (!properties.containsKey(LOCAL_OUTPUT_FOLDER)) {
			LOGGER.error("Required property " + LOCAL_OUTPUT_FOLDER + " was not set");
			return false;
		}
		LOGGER.debug("All properties are set");
		return true;
	}
	
}