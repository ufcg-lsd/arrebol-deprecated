package org.fogbowcloud.app.resource;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.AppPropertiesConstants;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.data.MediaType;

public class TestTaskResouce4JDF {

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
	public void testGetTask() throws Exception {
		String taskId = "taskId00";
		HttpGet get = new HttpGet(ResourceTestUtil.DEFAULT_PREFIX_URL + ResourceTestUtil.TASK_RESOURCE_SUFIX + taskId);
		String owner = ResourceTestUtil.DEFAULT_OWNER;
		get.addHeader(new BasicHeader(AppPropertiesConstants.X_AUTH_USER, owner));
		
		Task task = new TaskImpl(taskId, new Specification("image", "username", "publicKey", "privateKeyFilePath", "", ""));
		task.putMetadata("keyOne", "metadataOne");
		task.putMetadata("keyTwo", "metadataTwo");
		Mockito.when(resourceTestUtil.getArrebolController().getTaskById(Mockito.eq(taskId), Mockito.eq(owner))).thenReturn(task);
		Mockito.when(resourceTestUtil.getArrebolController().getTaskState(Mockito.eq(taskId), Mockito.eq(owner))).thenReturn(TaskState.RUNNING);
		
		HttpResponse response = HttpClients.createMinimal().execute(get);
		String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
		
		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		Assert.assertTrue(response.getEntity().getContentType().getValue()
				.contains(MediaType.TEXT_PLAIN.getName()));
		JSONObject jsonObject = new JSONObject(responseStr);
		Assert.assertEquals(TaskState.RUNNING.name(), jsonObject.get("state"));
	}
	
	@Test
	public void testGetTaskNotfound() throws Exception {
		HttpGet get = new HttpGet(ResourceTestUtil.DEFAULT_PREFIX_URL + ResourceTestUtil.TASK_RESOURCE_SUFIX + "notfound");
		get.addHeader(new BasicHeader(AppPropertiesConstants.X_AUTH_USER, ResourceTestUtil.DEFAULT_OWNER));
		Mockito.when(resourceTestUtil.getArrebolController().getTaskById(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
		
		HttpResponse response = HttpClients.createMinimal().execute(get);
	
		Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
	}	
		
}
