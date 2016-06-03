package org.fogbowcloud.app.resource;

import java.util.Properties;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.app.ArrebolController;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class TestJobResource {

	private static final String DEFAULT_PREFIX_URL = "http://localhost:";
	private static final String JOB_RESOURCE_SUFIX = "arrebol/job";
	private static final String DEFAULT_SERVER_PORT = "21380";
	
	private JDFSchedulerApplication jdfSchedulerApplication;	
	private ArrebolController arrebolController;
	private String urlPrefix;
	
	@Before
	public void setUp() throws Exception {
		this.arrebolController = Mockito.mock(ArrebolController.class);
		Mockito.doNothing().when(this.arrebolController).init();
		
		Properties properties = new Properties();
		properties.put(AppPropertiesConstants.REST_SERVER_PORT, DEFAULT_SERVER_PORT);
		Mockito.when(this.arrebolController.getProperties()).thenReturn(properties);
		
		this.jdfSchedulerApplication = new JDFSchedulerApplication(this.arrebolController);
		this.jdfSchedulerApplication.startServer();
		
		this.urlPrefix = DEFAULT_PREFIX_URL + DEFAULT_SERVER_PORT + "/";
	}
	
	@After
	public void tearDown() throws Exception {
		this.jdfSchedulerApplication.stopServer();
	}
	
	@Test
	public void testGetJobNotFound() throws Exception {
		String jobId = "nof_found";
		HttpGet get = new HttpGet(this.urlPrefix + JOB_RESOURCE_SUFIX + "/" + jobId);
		
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);
		
		Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
	}
	
	@Ignore
	@Test
	public void testSpecificGetJob() throws Exception {
		String jobName = "jobName00";
		HttpGet get = new HttpGet(this.urlPrefix + JOB_RESOURCE_SUFIX + "/" + jobName);
		
		JDFJob job = new JDFJob("schedPath", jobName);
		Mockito.when(this.arrebolController.getJobByName(Mockito.eq(jobName))).thenReturn(job);
		
		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);
		String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
		System.out.println(responseStr);
		
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		// TODO refactor 
		JSONObject jsonObject = new JSONObject(responseStr);
		Assert.assertEquals(job.getName(), jobName);
		Assert.assertEquals(job.getId(), jsonObject.optString("id"));
	}	
	
}
