package org.fogbowcloud.app.resource;

import java.util.ArrayList;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.json.JSONArray;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.data.MediaType;

public class TestJobEndpoint {

	ResourceTestUtil resourceTestUtil;
	
	@Before
	public void setUp() throws Exception {
		this.resourceTestUtil = new ResourceTestUtil();
		
		JDFSchedulerApplication jdfSchedulerApplication = resourceTestUtil.getJdfSchedulerApplication();
		jdfSchedulerApplication.startServer();
	}
	
	@After
	public void tearDown() throws Exception {
		resourceTestUtil.getJdfSchedulerApplication().stopServer();
	}
	
	@Test
	public void testGetJobs() throws Exception {
		HttpGet get = new HttpGet(ResourceTestUtil.DEFAULT_PREFIX_URL + ResourceTestUtil.JOB_ENDPOINT_SUFIX);
		
		ArrayList<JDFJob> jobs = new ArrayList<JDFJob>();
		String jobNameOne = "jobNameOne";
		JDFJob jdfJob = new JDFJob("jobPathOne", jobNameOne, "owner");
		jobs.add(jdfJob);
		jobs.add(new JDFJob("jobPathTwo", "jobNameTwo", "owner"));
		jobs.add(new JDFJob("jobPathThree", "jobNameThree", "owner"));		
		
		Mockito.when(resourceTestUtil.getArrebolController().getAllJobs("owner")).thenReturn(jobs);
		
		HttpResponse response = HttpClients.createMinimal().execute(get);
		String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
		
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		Assert.assertTrue(response.getEntity().getContentType().getValue()
				.contains(MediaType.TEXT_PLAIN.getName()));
		JSONArray jobsJsonArray = new JSONArray(responseStr);
		Assert.assertEquals(jobs.size(), jobsJsonArray.length());
	}
	
	//TODO test auxiliar methods
	
}
