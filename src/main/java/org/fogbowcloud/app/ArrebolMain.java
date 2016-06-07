package org.fogbowcloud.app;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.AppPropertiesConstants;

public class ArrebolMain {

	public static final Logger LOGGER = Logger.getLogger(ArrebolMain.class);
	
	/**
	 * This method receives a JDF file as input and requests the mapping of
	 * its attributes to JDL attributes, generating a JDL file at the end
	 * @param args 
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Incomplete arguments. Necessary pass two args. (1) arrebol.conf path and (2) sheduler.conf path.");
			System.exit(1);			
		} 
		
		Properties properties = new Properties();
		
		final String ARREBOL_CONF_PATH = args[0];
		properties.load(new FileInputStream(ARREBOL_CONF_PATH));
		
		final String SCHEDULER_CONF_PATH = args[1];
		properties.load(new FileInputStream(SCHEDULER_CONF_PATH));
		
		if (!checkProperties(properties)) {
			System.err.println("Missing required property, check Log for more information.");
			System.exit(1);
		}
	
		JDFSchedulerApplication app = new JDFSchedulerApplication(new ArrebolController(properties));
		app.startServer();		
	}
	
	private static boolean checkProperties(Properties properties) {
		if (!properties.containsKey(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME)){
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME + " was not set");
			return false;
		}
		if(!properties.containsKey(AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME)){
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_ORDER_SERVICE_TIME + " was not set");
			return false;
		}
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
		}
		if (!properties.containsKey(AppPropertiesConstants.REST_SERVER_PORT)){
			LOGGER.error("Required property " + AppPropertiesConstants.REST_SERVER_PORT + " was not set");
			return false;
		}
		if (!properties.containsKey(AppPropertiesConstants.EXECUTION_MONITOR_PERIOD)){
			LOGGER.error("Required property " + AppPropertiesConstants.EXECUTION_MONITOR_PERIOD + " was not set");
			return false;
		};
		if (!properties.containsKey(AppPropertiesConstants.INFRA_IS_STATIC)){
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_IS_STATIC + " was not set");
			return false;
		}
		if (!properties.containsKey(AppPropertiesConstants.INFRA_FOGBOW_USERNAME)) {
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_FOGBOW_USERNAME + " was not set");
			return false;
		}
		if (!properties.containsKey(AppPropertiesConstants.PUBLIC_KEY_CONSTANT)){
			LOGGER.error("Required property " + AppPropertiesConstants.PUBLIC_KEY_CONSTANT + " was not set");
			return false;
		}
		if (!properties.containsKey(AppPropertiesConstants.PRIVATE_KEY_FILEPATH)){
			LOGGER.error("Required property " + AppPropertiesConstants.PRIVATE_KEY_FILEPATH + " was not set");
			return false;
		}
		if (!properties.containsKey(AppPropertiesConstants.REMOTE_OUTPUT_FOLDER)){
			LOGGER.error("Required property " + AppPropertiesConstants.REMOTE_OUTPUT_FOLDER + " was not set");
			return false;
		}
		if (!properties.containsKey(AppPropertiesConstants.LOCAL_OUTPUT_FOLDER)) {
			LOGGER.error("Required property " + AppPropertiesConstants.LOCAL_OUTPUT_FOLDER + " was not set");
			return false;
		}
		LOGGER.debug("All properties are set");
		return true;
	}
	
}