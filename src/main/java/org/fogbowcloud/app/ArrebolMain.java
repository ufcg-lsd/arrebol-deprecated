package org.fogbowcloud.app;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;

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
					"Incomplete arguments. Necessary pass two args. (1) arrebol.conf path.");
			System.exit(1);
		}

		Properties properties = new Properties();

		final String ARREBOL_CONF_PATH = args[0];
		final String SCHED_CONF_PATH = args[1];
		properties.load(new FileInputStream(ARREBOL_CONF_PATH));
		
		properties.load(new FileInputStream(SCHED_CONF_PATH));

		if (!checkProperties(properties)) {
			System.err.println("Missing required property, check Log for more information.");
			System.exit(1);
		}

		JDFSchedulerApplication app = new JDFSchedulerApplication(new ArrebolController(properties));
		app.startServer();
	}

	//TODO: Maybe this method should be separate in some utils classes, one to each plugin in use
	// Each plugin must responsible to check the value of its properties
	private static boolean checkProperties(Properties properties) {
		
		//TODO validate the properties
		
		return true;
	}
}