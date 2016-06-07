package org.fogbowcloud.app.resource;

import java.util.Properties;

import org.fogbowcloud.app.ArrebolController;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.AppPropertiesConstants;
import org.mockito.Mockito;

public class ResourceTestUtil {
	public static final String DEFAULT_SERVER_PORT = "30023";	
	public static final String DEFAULT_PREFIX_URL = "http://localhost:" + DEFAULT_SERVER_PORT;
	public static final String JOB_RESOURCE_SUFIX = "/arrebol/job";
	public static final String JOB_ENDPOINT_SUFIX = "/arrebol/job/ui";
	public static final String TASK_RESOURCE_SUFIX = "/arrebol/task/";	
	
	private JDFSchedulerApplication jdfSchedulerApplication;	
	private ArrebolController arrebolController;

	public ResourceTestUtil() throws Exception {
		
		this.arrebolController = Mockito.mock(ArrebolController.class);
		Mockito.doNothing().when(this.arrebolController).init();
		
		Properties properties = new Properties();
		properties.put(AppPropertiesConstants.REST_SERVER_PORT, DEFAULT_SERVER_PORT);
		Mockito.when(this.arrebolController.getProperties()).thenReturn(properties);
		
		this.jdfSchedulerApplication = new JDFSchedulerApplication(this.arrebolController);
	}
	
	public JDFSchedulerApplication getJdfSchedulerApplication() {
		return jdfSchedulerApplication;
	}
	
	public ArrebolController getArrebolController() {
		return arrebolController;
	}
	
}
