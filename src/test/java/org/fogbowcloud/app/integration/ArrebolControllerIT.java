package org.fogbowcloud.app.integration;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.app.ArrebolController;
import org.fogbowcloud.app.resource.JobResource;
import org.fogbowcloud.app.resource.ResourceTestUtil;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.app.utils.authenticator.Credential;
import org.fogbowcloud.blowout.core.util.AppPropertiesConstants;
import org.junit.Test;

public class ArrebolControllerIT {
	
	public ArrebolController ac;

	public JDFSchedulerApplication app;
	
	public static final String RESOURCE_DIR = "test" + File.separator + "resources";
	
	public static final String EXSIMPLE_JOB = RESOURCE_DIR + File.separator + "sleepjob.jdf";

	private static final String DEFAULT_SERVER_PORT = "44444";
	
	public static final String DEFAULT_PREFIX_URL = "http://localhost:" + DEFAULT_SERVER_PORT;
	
	@Test
	public void testNormalExecution() {
		/*
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
		properties.setProperty(ArrebolPropertiesConstants.REMOVE_PREVIOUS_RESOURCES, "true");
		properties.setProperty(ArrebolPropertiesConstants.DEFAULT_SPECS_FILE_PATH, "defaulResource.json");
		properties.setProperty(ArrebolPropertiesConstants.REST_SERVER_PORT, "44444");
		properties.setProperty(ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD, "30000");
		properties.setProperty(ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN, "org.fogbowcloud.app.integration.FakeAuthenticationPlugin");
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
		File file = new File("/tmp/testdatastore");
		file.delete();
		this.ac = new ArrebolController(properties);
		try {
			this.app = new JDFSchedulerApplication(ac);
			this.app.startServer();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String owner = ResourceTestUtil.DEFAULT_OWNER;
		HttpPost post = new HttpPost(DEFAULT_PREFIX_URL + ResourceTestUtil.JOB_RESOURCE_SUFIX);
		post.addHeader(new BasicHeader(ArrebolPropertiesConstants.X_AUTH_USER, owner));
		Credential cred = new Credential(owner, "blabla", this.ac.getNonce());
		post.addHeader(new BasicHeader(ArrebolPropertiesConstants.X_CREDENTIALS, cred.toJSON().toString()));
		
		
		
		String jdfFilePath = EXSIMPLE_JOB;
		String schedPath = "schedPath";
		String friendlyName = "friendly";
		
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.addTextBody(JobResource.SCHED_PATH, schedPath, ContentType.TEXT_PLAIN);
		builder.addTextBody(JobResource.FRIENDLY, friendlyName, ContentType.TEXT_PLAIN);
		builder.addTextBody(JobResource.JDF_FILE_PATH, jdfFilePath, ContentType.TEXT_PLAIN);
		HttpEntity multipart = builder.build();		
		post.setEntity(multipart);
		
		HttpResponse response;
		try {
			Thread.sleep(2000);
			response = HttpClients.createMinimal().execute(post);
			String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
			assertTrue(this.ac.getJobById(responseStr, ResourceTestUtil.DEFAULT_OWNER) != null);
		
			System.out.println("Chegou aqui");
			this.app.stop();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
