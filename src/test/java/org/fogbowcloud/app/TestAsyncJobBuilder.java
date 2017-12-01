package org.fogbowcloud.app;

import org.fogbowcloud.app.datastore.JobDataStore;
import org.fogbowcloud.app.exception.ArrebolException;
import org.fogbowcloud.app.jdfcompiler.main.CompilerException;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.model.LDAPUser;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.blowout.core.BlowoutController;
import org.fogbowcloud.blowout.core.exception.BlowoutException;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.util.AppPropertiesConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class TestAsyncJobBuilder {

    private static final String EXSIMPLE_JOB = "test" + File.separator + "resources" + File.separator + "SimpleJob2.jdf";

    private static final String user = "arrebolservice";
    private static final String username = "arrebolservice";

    private ArrebolController arrebol;
    private BlowoutController blowout;

    @Before
    public void setUp() throws IOException, BlowoutException, ArrebolException {
        Properties properties = new Properties();
        properties.setProperty(
                AppPropertiesConstants.INFRA_AUTH_TOKEN_UPDATE_PLUGIN,
                "org.fogbowcloud.blowout.infrastructure.token.NAFTokenUpdatePlugin"
        );
        properties.setProperty(
                AppPropertiesConstants.INFRA_PROVIDER_CLASS_NAME,
                "org.fogbowcloud.blowout.scheduler.infrastructure.fogbow.FogbowInfrastructureProvider"
        );
        properties.setProperty(
                AppPropertiesConstants.INFRA_RESOURCE_IDLE_LIFETIME,
                "60000"
        );
        properties.setProperty(
                AppPropertiesConstants.INFRA_RESOURCE_CONNECTION_TIMEOUT,
                "60000"
        );
        properties.setProperty(
                AppPropertiesConstants.INFRA_IS_STATIC,
                "true"
        );
        properties.setProperty(
                ArrebolPropertiesConstants.REST_SERVER_PORT,
                "44444"
        );
        properties.setProperty(
                ArrebolPropertiesConstants.EXECUTION_MONITOR_PERIOD,
                "60000"
        );
        properties.setProperty(
                ArrebolPropertiesConstants.PUBLIC_KEY_CONSTANT,
                ""
        );
        properties.setProperty(
                ArrebolPropertiesConstants.PRIVATE_KEY_FILEPATH,
                ""
        );
        properties.setProperty(
                ArrebolPropertiesConstants.REMOTE_OUTPUT_FOLDER,
                ""
        );
        properties.setProperty(
                ArrebolPropertiesConstants.LOCAL_OUTPUT_FOLDER,
                ""
        );
        properties.setProperty(
                ArrebolPropertiesConstants.AUTHENTICATION_PLUGIN,
                "org.fogbowcloud.app.utils.authenticator.LDAPAuthenticator"
        );
        arrebol = Mockito.spy(new ArrebolController(properties));

        JobDataStore dataStore = Mockito.spy(new JobDataStore("jdbc:h2:/tmp/datastores/testfogbowresourcesdatastore"));
        arrebol.setDataStore(dataStore);

        blowout = Mockito.mock(BlowoutController.class);
        arrebol.setBlowoutController(blowout);
    }

    @Test
    public void testAsyncJobCreation() {
        try {
            Mockito.doNothing().when(blowout).addTaskList(Mockito.anyListOf(Task.class));

            String id = arrebol.addJob(EXSIMPLE_JOB, new LDAPUser(user, username));

            JDFJob job = arrebol.getJobById(id, user);
            Assert.assertEquals(JDFJob.JDFJobState.SUBMITTED, job.getState());
            Assert.assertEquals(0, job.getTasks().size());

            arrebol.waitForJobCreation(job.getId());

            job = arrebol.getJobById(id, user);
            Assert.assertEquals(JDFJob.JDFJobState.CREATED, job.getState());
            Assert.assertEquals(3, job.getTasks().size());
        } catch (CompilerException | NameAlreadyInUseException | BlowoutException | IOException | InterruptedException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testDeleteJobWhileBeingCreated() {
        try {
            Mockito.doNothing().when(blowout).addTaskList(Mockito.anyListOf(Task.class));

            String id = arrebol.addJob(EXSIMPLE_JOB, new LDAPUser(user, username));

            JDFJob job = arrebol.getJobById(id, user);
            Assert.assertEquals(JDFJob.JDFJobState.SUBMITTED, job.getState());

            arrebol.stopJob(id, user);

            job = arrebol.getJobById(id, user);
            Assert.assertNull(job);
        } catch (CompilerException | NameAlreadyInUseException | BlowoutException | IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }
}
