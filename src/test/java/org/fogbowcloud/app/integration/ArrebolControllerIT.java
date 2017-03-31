package org.fogbowcloud.app.integration;

import java.util.Properties;

import org.fogbowcloud.app.ArrebolController;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.PropertiesConstants;
import org.fogbowcloud.blowout.core.util.AppPropertiesConstants;
import org.junit.Test;

public class ArrebolControllerIT {
	
	public ArrebolController ac;

	public JDFSchedulerApplication app;
	
	@Test
	public void testNormalExecution() {
		/**
		 * Required properties to be set:
		 * 
		 * (app) DB_DATASTORE_URL 
		 * (p.const) REMOVE_PREVIOUS_RESOURCES 
		 * (p.const) DEFAULT_SPECS_FILE_PATH
		 * (p.const) REST_SERVER_PORT 
		 * (p.const) EXECUTION_MONITOR_PERIOD
		 * (p.const) AUTHENTICATION_PLUGIN 
		 * (app) IMPLEMENTATION_BLOWOUT_POOL
		 * (app) IMPLEMENTATION_INFRA_PROVIDER 
		 * (app) IMPLEMENTATION_INFRA_MANAGER
		 * (app) IMPLEMENTATION_SCHEDULER 
		 * (app) INFRA_RESOURCE_IDLE_LIFETIME 
		 * (app) INFRA_RESOURCE_CONNECTION_TIMEOUT
		 * (app) INFRA_IS_STATIC 
		 * (app) INFRA_AUTH_TOKEN_UPDATE_PLUGIN 
		 * (app) INFRA_MONITOR_PERIOD 
		 * (app) INFRA_RESOURCE_CONECTION_RETRY 
		 * (app) INFRA_RESOURCE_REUSE_TIMES
		 */
		Properties properties = new Properties();
		properties.setProperty(PropertiesConstants.REMOVE_PREVIOUS_RESOURCES, "true");
		properties.setProperty(PropertiesConstants.DEFAULT_SPECS_FILE_PATH, "defaulResource.json");
		properties.setProperty(PropertiesConstants.REST_SERVER_PORT, "44444");
		properties.setProperty(PropertiesConstants.EXECUTION_MONITOR_PERIOD, "30000");
		properties.setProperty(PropertiesConstants.AUTHENTICATION_PLUGIN, "org.fogbowcloud.app.integration.FakeAuthenticationPlugin");
		properties.setProperty(AppPropertiesConstants.DB_DATASTORE_URL, "jdbc:sqlite:/tmp/testdatastore");
		properties.setProperty(AppPropertiesConstants.IMPLEMENTATION_BLOWOUT_POOL, "org.fogbowcloud.blowout.pool.DefaultBlowoutPool");
		properties.setProperty(AppPropertiesConstants.IMPLEMENTATION_INFRA_PROVIDER, "org.fogbowcloud.blowout.infrastructure.provider.fogbow.FogbowInfrastructureProvider");
		properties.setProperty(AppPropertiesConstants.IMPLEMENTATION_INFRA_MANAGER, "org.fogbowcloud.blowout.infrastructure.manager.DefaultInfrastructureManager");
		properties.setProperty(AppPropertiesConstants.IMPLEMENTATION_SCHEDULER, "org.fogbowcloud.blowout.core.StandardScheduler");
		properties.setProperty(AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME, "0");
		properties.setProperty(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT, "20000");
		properties.setProperty(AppPropertiesConstants.INFRA_IS_STATIC, "false");
		properties.setProperty(AppPropertiesConstants.INFRA_AUTH_TOKEN_UPDATE_PLUGIN, "org.fogbowcloud.app.integration.FakeTokenUpdatePlugin");
		properties.setProperty(AppPropertiesConstants.INFRA_MONITOR_PERIOD, "30000");
		properties.setProperty(AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_RETRY, "1");
		properties.setProperty(AppPropertiesConstants.INFRA_RESOURCE_REUSE_TIMES, "1");
		this.ac = new ArrebolController(properties);
		try {
			this.app = new JDFSchedulerApplication(ac);
			this.app.startServer();
			this.app.stopServer();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
