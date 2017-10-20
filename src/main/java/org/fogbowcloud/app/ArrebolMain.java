package org.fogbowcloud.app;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.blowout.core.exception.BlowoutException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ArrebolMain {

    public static final Logger LOGGER = Logger.getLogger(ArrebolMain.class);

    /**
     * This method receives a JDF file as input and requests the mapping of its
     * attributes to JDL attributes, generating a JDL file at the end
     *
     * @param args Path to the config files
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println(
                    "Incomplete arguments. Necessary to pass two args. (1) arrebol.conf path and (2) blowout.conf path.");
            System.exit(1);
        }

        Properties properties = new Properties();

        String arrebolConfPath = args[0];
        String schedConfPath = args[1];

        try {
            properties.load(new FileInputStream(arrebolConfPath));
            properties.load(new FileInputStream(schedConfPath));
        } catch (IOException e) {
            LOGGER.error("Failed to read configuration file.", e);
            System.exit(1);
        }

<<<<<<< HEAD
		JDFSchedulerApplication app = new JDFSchedulerApplication(new ArrebolController(properties));
		app.startServer();
	}

	//TODO: Maybe this method should be separate in some utils classes, one to each plugin in use
	// Each plugin must responsible to check the value of its properties
	private static boolean checkProperties(Properties properties) {

		if (!properties.containsKey(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME)) {
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME + " was not set");
			return false;
		}
		;
		if (!properties.containsKey(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME)) {
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME + " was not set");
			return false;
		}
		;
		if (!properties.containsKey(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT)) {
			LOGGER.error(
					"Required property " + AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT + " was not set");
			return false;
		}
		if (!properties.containsKey(PropertiesConstants.REST_SERVER_PORT)) {
			LOGGER.error("Required property " + PropertiesConstants.REST_SERVER_PORT + " was not set");
			return false;
		}
		if (!properties.containsKey(PropertiesConstants.EXECUTION_MONITOR_PERIOD)) {
			LOGGER.error("Required property " + PropertiesConstants.EXECUTION_MONITOR_PERIOD + " was not set");
			return false;
		}
		;
		if (!properties.containsKey(AppPropertiesConstants.INFRA_IS_STATIC)) {
			LOGGER.error("Required property " + AppPropertiesConstants.INFRA_IS_STATIC + " was not set");
			return false;
		}
		
		if (!properties.containsKey(PropertiesConstants.PUBLIC_KEY_CONSTANT)) {
			LOGGER.error("Required property " + PropertiesConstants.PUBLIC_KEY_CONSTANT + " was not set");
			return false;
		}
		if (!properties.containsKey(PropertiesConstants.PRIVATE_KEY_FILEPATH)) {
			LOGGER.error("Required property " + PropertiesConstants.PRIVATE_KEY_FILEPATH + " was not set");
			return false;
		}
		if (!properties.containsKey(PropertiesConstants.REMOTE_OUTPUT_FOLDER)) {
			LOGGER.error("Required property " + PropertiesConstants.REMOTE_OUTPUT_FOLDER + " was not set");
			return false;
		}
		if (!properties.containsKey(PropertiesConstants.LOCAL_OUTPUT_FOLDER)) {
			LOGGER.error("Required property " + PropertiesConstants.LOCAL_OUTPUT_FOLDER + " was not set");
			return false;
		}
		if (!properties.containsKey(PropertiesConstants.AUTHENTICATION_PLUGIN)) {

			LOGGER.error("Required property " + PropertiesConstants.AUTHENTICATION_PLUGIN+ " was not set");
			return false;
		} else {
			String authenticationPlugin = properties.getProperty(PropertiesConstants.AUTHENTICATION_PLUGIN);
			if (authenticationPlugin.equals("org.fogbowcloud.app.utils.LDAPAuthenticator")) {
				if (!properties.containsKey(PropertiesConstants.LDAP_AUTHENTICATION_URL)) {
					LOGGER.error(
							"Required property " + PropertiesConstants.LDAP_AUTHENTICATION_URL + " was not set");
					return false;
				}
				if (!properties.containsKey(PropertiesConstants.LDAP_AUTHENTICATION_BASE)) {
					LOGGER.error(
							"Required property " + PropertiesConstants.LDAP_AUTHENTICATION_BASE + " was not set");
					return false;
				}
			}
		}
		LOGGER.info("All properties are set");
		return true;
	}
=======
        try {
            final JDFSchedulerApplication app = new JDFSchedulerApplication(
                    new ArrebolController(properties)
            );
            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(
                    new Thread() {
                        @Override
                        public void run() {
                            System.out.println("Exiting server");
                            try {
                                app.stopServer();
                                mainThread.join();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );
            app.startServer();
        } catch (BlowoutException e) {
            LOGGER.error("Failed to initialize Blowout.", e);
            System.exit(1);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Arrebol.", e);
            System.exit(1);
        }
    }
>>>>>>> refs/remotes/origin/master
}
