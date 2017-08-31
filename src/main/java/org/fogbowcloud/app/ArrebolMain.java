package org.fogbowcloud.app;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.blowout.core.util.AppPropertiesConstants;

public class ArrebolMain {

	public static final Logger LOGGER = Logger.getLogger(ArrebolMain.class);

	/**
	 * This method receives a JDF file as input and requests the mapping of its
	 * attributes to JDL attributes, generating a JDL file at the end
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println(
					"Incomplete arguments. Necessary to pass two args. (1) arrebol.conf path and (2) sheduler.conf path.");
			System.exit(1);
		}

		Properties properties = new Properties();

		String arrebolConfPath = args[0];
		String schedConfPath = args[1];

		properties.load(new FileInputStream(arrebolConfPath));
		properties.load(new FileInputStream(schedConfPath));

		if (!checkProperties(properties)) {
			System.err.println("Missing required property, check Log for more information.");
			System.exit(1);
		}

		final JDFSchedulerApplication app = new JDFSchedulerApplication(new ArrebolController(properties));
		final Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(
				new Thread() {
					@Override
					public void run() {
						try {
							app.stopServer();
							mainThread.join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
		);
		app.startServer();
	}

	//TODO: Maybe this method should be separate in some utils classes, one to each plugin in use
	// Each plugin must responsible to check the value of its properties
	private static boolean checkProperties(Properties properties) {
		if (!properties.containsKey(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME)) {
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME + " was not set");
			return false;
		}
		if (!properties.containsKey(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME)) {
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME + " was not set");
			return false;
		}
		if (!properties.containsKey(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT)) {
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT + " was not set");
			return false;
		}
		if (!properties.containsKey(AppPropertiesConstants.INFRA_IS_STATIC)) {
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_IS_STATIC + " was not set");
			return false;
		}

		if (!properties.containsKey(ArrebolPropertiesConstants.REST_SERVER_PORT)) {
			LOGGER.error("Required property " + ArrebolPropertiesConstants.REST_SERVER_PORT + " was not set");
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD)) {
			LOGGER.error("Required property " + ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD + " was not set");
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.PUBLIC_KEY_CONSTANT)) {
			LOGGER.error("Required property " + ArrebolPropertiesConstants.PUBLIC_KEY_CONSTANT + " was not set");
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.PRIVATE_KEY_FILEPATH)) {
			LOGGER.error("Required property " + ArrebolPropertiesConstants.PRIVATE_KEY_FILEPATH + " was not set");
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.REMOTE_OUTPUT_FOLDER)) {
			LOGGER.error("Required property " + ArrebolPropertiesConstants.REMOTE_OUTPUT_FOLDER + " was not set");
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.LOCAL_OUTPUT_FOLDER)) {
			LOGGER.error("Required property " + ArrebolPropertiesConstants.LOCAL_OUTPUT_FOLDER + " was not set");
			return false;
		}
		if (!properties.containsKey(ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN)) {
			LOGGER.error("Required property " + ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN+ " was not set");
			return false;
		} else {
			String authenticationPlugin = properties.getProperty(ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN);
			if (authenticationPlugin.equals("org.fogbowcloud.app.utils.LDAPAuthenticator")) {
				if (!properties.containsKey(ArrebolPropertiesConstants.LDAP_AUTHENTICATION_URL)) {
					LOGGER.error(
							"Required property " + ArrebolPropertiesConstants.LDAP_AUTHENTICATION_URL + " was not set");
					return false;
				}
				if (!properties.containsKey(ArrebolPropertiesConstants.LDAP_AUTHENTICATION_BASE)) {
					LOGGER.error(
							"Required property " + ArrebolPropertiesConstants.LDAP_AUTHENTICATION_BASE + " was not set");
					return false;
				}
			}
		}
		LOGGER.debug("All properties are set");
		return true;
	}
}