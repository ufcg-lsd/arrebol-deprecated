package org.fogbowcloud.app;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.blowout.core.util.AppPropertiesConstants;

import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class ArrebolMain {

    public static final Logger LOGGER = Logger.getLogger(ArrebolMain.class);

    /**
     * This method receives a JDF file as input and requests the mapping of its
     * attributes to JDL attributes, generating a JDL file at the end
     *
     * @param args Path to the config files
     * @throws Exception Throws at failure reading configuration files or initializing Arrebol Controller or Blowout Controller
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
    }

    private static String requiredPropertyMessage(String property) {
        return "Required property " + property + " was not set";
    }

    //TODO: Maybe this method should be separate in some utils classes, one to each plugin in use
    // Each plugin must responsible to check the value of its properties
    private static boolean checkProperties(Properties properties) {
        // Blowout required properties
        if (!properties.containsKey(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME)) {
            LOGGER.error(requiredPropertyMessage(AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME));
            return false;
        }
        if (!properties.containsKey(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME)) {
            LOGGER.error(requiredPropertyMessage(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME));
            return false;
        }
        if (!properties.containsKey(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT)) {
            LOGGER.error(requiredPropertyMessage(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT));
            return false;
        }
        if (!properties.containsKey(AppPropertiesConstants.INFRA_IS_STATIC)) {
            LOGGER.error(requiredPropertyMessage(AppPropertiesConstants.INFRA_IS_STATIC));
            return false;
        }

        // Arrebol required properties
        if (!properties.containsKey(ArrebolPropertiesConstants.REST_SERVER_PORT)) {
            LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.REST_SERVER_PORT));
            return false;
        }
        if (!properties.containsKey(ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD)) {
            LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD));
            return false;
        }
        if (!properties.containsKey(ArrebolPropertiesConstants.PUBLIC_KEY_CONSTANT)) {
            LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.PUBLIC_KEY_CONSTANT));
            return false;
        }
        if (!properties.containsKey(ArrebolPropertiesConstants.PRIVATE_KEY_FILEPATH)) {
            LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.PRIVATE_KEY_FILEPATH));
            return false;
        }
        if (!properties.containsKey(ArrebolPropertiesConstants.REMOTE_OUTPUT_FOLDER)) {
            LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.REMOTE_OUTPUT_FOLDER));
            return false;
        }
        if (!properties.containsKey(ArrebolPropertiesConstants.LOCAL_OUTPUT_FOLDER)) {
            LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.LOCAL_OUTPUT_FOLDER));
            return false;
        }
        if (properties.containsKey(ArrebolPropertiesConstants.ENCRYPTION_TYPE)) {
            try {
                MessageDigest.getInstance(properties.getProperty(ArrebolPropertiesConstants.ENCRYPTION_TYPE));
            } catch (NoSuchAlgorithmException e) {
                String builder = "Property " +
                        ArrebolPropertiesConstants.ENCRYPTION_TYPE +
                        "(" +
                        properties.getProperty(ArrebolPropertiesConstants.ENCRYPTION_TYPE) +
                        ") does not refer to a valid encryption algorithm." +
                        " Valid options are 'MD5', 'SHA-1' and 'SHA-256'.";
                LOGGER.error(builder);
                return false;
            }
        }
        if (!properties.containsKey(ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN)) {
            LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN));
            return false;
        } else {
            String authenticationPlugin = properties.getProperty(ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN);
            if (authenticationPlugin.equals("org.fogbowcloud.app.utils.LDAPAuthenticator")) {
                if (!properties.containsKey(ArrebolPropertiesConstants.LDAP_AUTHENTICATION_URL)) {
                    LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.LDAP_AUTHENTICATION_URL));
                    return false;
                }
                if (!properties.containsKey(ArrebolPropertiesConstants.LDAP_AUTHENTICATION_BASE)) {
                    LOGGER.error(requiredPropertyMessage(ArrebolPropertiesConstants.LDAP_AUTHENTICATION_BASE));
                    return false;
                }
            }
        }
        LOGGER.debug("All properties are set");
        return true;
    }
}