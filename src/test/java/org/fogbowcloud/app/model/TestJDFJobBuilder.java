package org.fogbowcloud.app.model;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.app.jdfcompiler.main.CompilerException;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.blowout.core.model.Command;
import org.fogbowcloud.blowout.core.model.Task;
import org.junit.Test;

public class TestJDFJobBuilder {
	
	public static final String RESOURCE_DIR = "test" + File.separator + "resources";

	public static final String EXSIMPLE_JOB = RESOURCE_DIR + File.separator + "SimpleJob2.jdf";

	@Test
	public void testJDFCompilation () throws CompilerException, IOException, InterruptedException {
		Properties properties = new Properties();
		properties.setProperty(ArrebolPropertiesConstants.INFRA_RESOURCE_USERNAME, "infraname");
		properties.setProperty(ArrebolPropertiesConstants.PUBLIC_KEY_CONSTANT, "public_key");
		properties.setProperty(ArrebolPropertiesConstants.PRIVATE_KEY_FILEPATH, "file path");
		User owner = new LDAPUser("arrebolservice", "arrebolservice");
		JDFJob testJob = new JDFJob(owner.getUser(), new ArrayList<Task>(), owner.getUsername());
		JDFJobBuilder.createJobFromJDFFile(
				testJob,
				EXSIMPLE_JOB,
				properties
		);
		List<Task> tasks = testJob.getTasks();
		
		assertEquals(tasks.size(), 3);
		for (Command command : tasks.get(0).getAllCommands()) {
			System.out.println(command.getCommand());
		}
		assertEquals(tasks.get(0).getUUID(), "1417");
	}
}
