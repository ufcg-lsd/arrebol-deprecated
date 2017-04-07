package org.fogbowcloud.app.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.jdfcompiler.job.JobSpecification;
import org.fogbowcloud.app.jdfcompiler.job.TaskSpecification;
import org.fogbowcloud.app.jdfcompiler.main.CommonCompiler;
import org.fogbowcloud.app.jdfcompiler.main.CommonCompiler.FileType;
import org.fogbowcloud.app.jdfcompiler.main.CompilerException;
import org.fogbowcloud.app.jdfcompiler.semantic.IOCommand;
import org.fogbowcloud.app.jdfcompiler.semantic.JDLCommand;
import org.fogbowcloud.app.jdfcompiler.semantic.JDLCommand.JDLCommandType;
import org.fogbowcloud.app.jdfcompiler.semantic.RemoteCommand;
import org.fogbowcloud.app.utils.ArrebolPropertiesConstants;
import org.fogbowcloud.app.utils.PropertiesConstants;
import org.fogbowcloud.blowout.core.model.Command;
import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.blowout.infrastructure.provider.fogbow.FogbowRequirementsHelper;
import org.fogbowcloud.blowout.pool.AbstractResource;

public class JDFTasks {

	// FIXME: what is this?
	private static final String SANDBOX = "sandbox";

	// FIXME: what is this?
	private static final String LOCAL_OUTPUT_FOLDER = "local_output";

	private static final String REMOTE_OUTPUT_FOLDER = "remote_output_folder";

	protected static final String PRIVATE_KEY_FILEPATH = "private_key_filepath";

	private static String standardImage = "fogbow-ubuntu";

	protected static final String PUBLIC_KEY_CONSTANT = "public_key";
	private final static String SSH_SCP_PRECOMMAND = "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no";

	private static final Logger LOGGER = Logger.getLogger(JDFTasks.class);

	/**
	 *
	 * @param job
	 * @param jdfFilePath
	 * @param schedPath
	 * @param properties
	 * @return
	 * @throws IllegalArgumentException
	 * @throws CompilerException
	 */
	public static List<Task> getTasksFromJDFFile(JDFJob job, String jdfFilePath, Properties properties)
			throws CompilerException {

		ArrayList<Task> taskList = new ArrayList<Task>();

		if (jdfFilePath == null) {
			throw new IllegalArgumentException("jdfFilePath cannot be null");
		}

		File file = new File(jdfFilePath);

		if (file.exists()) {
			if (file.canRead()) {
				// Compiling JDF
				CommonCompiler commonCompiler = new CommonCompiler();
				commonCompiler.compile(jdfFilePath, FileType.JDF);

				JobSpecification jobSpec = (JobSpecification) commonCompiler.getResult().get(0);

					
				job.setFriendlyName(jobSpec.getLabel());

				String schedPath = jobSpec.getSchedPath();

				// Mapping attributes

				// FIXME: what does this block do?
				String jobRequirementes = jobSpec.getRequirements();
				LOGGER.debug("JobReq: " + jobRequirementes);
				
				jobRequirementes = jobRequirementes.replace("(", "").replace(")", "");
				String image = standardImage;
				for (String req : jobRequirementes.split("and")) {
					if (req.trim().startsWith("image")) {
						image = req.split("==")[1].trim();
					}
				}

				Specification spec = new Specification(image,
						properties.getProperty(PropertiesConstants.INFRA_RESOURCE_USERNAME),
						properties.getProperty(PUBLIC_KEY_CONSTANT), properties.getProperty(PRIVATE_KEY_FILEPATH), "",
						"");
				LOGGER.debug(properties.getProperty(PropertiesConstants.INFRA_RESOURCE_USERNAME));

				int i = 0;
				for (String req : jobRequirementes.split("and")) {
				if (i == 0 && !req.trim().startsWith("image")) {
						i++;
						LOGGER.debug("NEW REQUIREMENT: " +req);
						spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, req);

					} else if (!req.trim().startsWith("image")) {
						spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS,
								spec.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS) + " && "
										+ req);
					}
				}

				spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUEST_TYPE, "one-time");
				int taskID = 0;
				for (TaskSpecification taskSpec : jobSpec.getTaskSpecs()) {

					Task task = new TaskImpl("TaskNumber" + "-" + taskID + "-" + UUID.randomUUID(), spec);
					task.putMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER,
							properties.getProperty(REMOTE_OUTPUT_FOLDER));
					task.putMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER,
							schedPath + properties.getProperty(LOCAL_OUTPUT_FOLDER));
					task.putMetadata(TaskImpl.METADATA_SANDBOX, SANDBOX);
					task.putMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH,
							properties.getProperty(REMOTE_OUTPUT_FOLDER) + "/exit");
					task.putMetadata(ArrebolPropertiesConstants.JOB_ID, job.getId());
					task.putMetadata(ArrebolPropertiesConstants.OWNER, job.getOwner());

					parseInitCommands(job.getId(), taskSpec, task, schedPath);
					parseTaskCommands(job.getId(), taskSpec, task, schedPath);
					parseFinalCommands(job.getId(), taskSpec, task, schedPath);

					taskList.add(task);
					LOGGER.debug("Task spec: " + task.getSpecification().toString());
				
					taskID++;
				}

			} else {
				throw new IllegalArgumentException(
						"Unable to read file: " + file.getAbsolutePath() + " check your permissions.");
			}
		} else {
			throw new IllegalArgumentException("File: " + file.getAbsolutePath() + " does not exists.");
		}

		return taskList;
	}

	/**
	 * This method translates the JDF remote executable command into the JDL
	 * format
	 *
	 * @param jobID
	 * @param taskSpec
	 *            The task specification {@link TaskSpecification}
	 * @param task
	 *            The output expression containing the JDL job
	 * @throws IllegalArgumentException
	 */
	private static void parseTaskCommands(String jobId, TaskSpecification taskSpec, Task task, String schedPath)
			throws IllegalArgumentException {

		List<JDLCommand> initBlocks = taskSpec.getTaskBlocks();
		if (initBlocks == null) {
			return;
		}
		for (JDLCommand jdlCommand : initBlocks) {
			if (jdlCommand.getBlockType().equals(JDLCommandType.IO)) {
				addIOCommand(jobId, task, (IOCommand) jdlCommand, schedPath);
			} else {
				addRemoteCommand(jobId, task, (RemoteCommand) jdlCommand, schedPath);
			}
		}
	}

	/**
<<<<<<< HEAD
	 * This method translates the Ourgrid input IOBlocks to JDL InputSandbox
=======
	 * It translates the input IOBlocks to JDL InputSandbox
>>>>>>> Fixed most of the FIXMEs
	 *
	 * @param jobId
	 * @param taskSpec
	 *            The task specification {@link TaskSpecification}
	 * @param task
	 *            The output expression containing the JDL job
<<<<<<< HEAD
=======
	 * @param schedPath
>>>>>>> Fixed most of the FIXMEs
	 */
	private static void parseInitCommands(String jobId, TaskSpecification taskSpec, Task task, String schedPath) {

		List<JDLCommand> initBlocks = taskSpec.getInitBlocks();
		if (initBlocks == null) {
			return;
		}
		for (JDLCommand jdlCommand : initBlocks) {
			if (jdlCommand.getBlockType().equals(JDLCommandType.IO)) {
				addIOCommand(jobId, task, (IOCommand) jdlCommand, schedPath);
			} else {
				addRemoteCommand(jobId, task, (RemoteCommand) jdlCommand, schedPath);
			}
		}
	}

	public static void addIOCommand(String jobId, Task task, IOCommand command, String schedPath) {
		String sourceFile = command.getEntry().getSourceFile();
		String destination = command.getEntry().getDestination();
		String IOType = command.getEntry().getCommand();
		if (IOType.toUpperCase().equals("PUT") || IOType.toUpperCase().equals("STORE")) {
			task.addCommand(mkdirRemoteFolder(getDirectoryTree(destination)));
			if (sourceFile.startsWith("/")) {
				task.addCommand(stageInCommand(sourceFile, destination));
			} else {
				task.addCommand(stageInCommand(schedPath + sourceFile, destination));
			}
			LOGGER.debug("JobId: " + jobId + " task: " + task.getId() + " input command:"
					+ stageInCommand(schedPath + sourceFile, destination).getCommand());
		} else {
			task.addCommand(mkdirLocalFolder(getDirectoryTree(destination)));
			if (sourceFile.startsWith("/")) {
				task.addCommand(stageOutCommand(sourceFile, destination));
			} else {
				task.addCommand(stageOutCommand(schedPath + sourceFile, destination));
			}
			LOGGER.debug("JobId: " + jobId + " task: " + task.getId() + " output command:"
					+ stageOutCommand(schedPath + sourceFile, destination).getCommand());
		}

	}

	public static void addRemoteCommand(String jobId, Task task, RemoteCommand remCommand, String schedPath) {
		Command command = new Command("\"" + remCommand.getContent() + "\"", Command.Type.REMOTE);
		LOGGER.debug("JobId: " + jobId + " task: " + task.getId() + " remote command: " + remCommand.getContent());
		task.addCommand(command);
	}

	public static String getDirectoryTree(String destination) {
		int lastDir = destination.lastIndexOf(File.separator);
		if (lastDir == -1 ) {
			return "";
		}
		return destination.substring(0, lastDir);
	}

	private static Command mkdirRemoteFolder(String folder) {
		String mkdirCommand = "mkdir -p " + folder;
		return new Command(mkdirCommand, Command.Type.REMOTE);
	}

	private static Command stageInCommand(String localFile, String remoteFile) {
		String scpCommand = "su $UUID ; " + "scp " + SSH_SCP_PRECOMMAND + " -P $" + AbstractResource.ENV_SSH_PORT
				+ " -i $" + AbstractResource.ENV_PRIVATE_KEY_FILE + " " + localFile + " $"
				+ AbstractResource.ENV_SSH_USER + "@" + "$" + AbstractResource.ENV_HOST + ":" + remoteFile;
		return new Command(scpCommand, Command.Type.LOCAL);
	}

	/**
	 * This method translates the Ourgrid output IOBlocks to JDL InputSandbox
	 *
	 * @param jobID
	 * @param taskSpec
	 *            The task specification {@link TaskSpecification}
	 * @param task
	 *            The output expression containing the JDL job
	 */
	private static void parseFinalCommands(String jobId, TaskSpecification taskSpec, Task task, String schedPath) {

		List<JDLCommand> initBlocks = taskSpec.getFinalBlocks();
		if (initBlocks == null) {
			return;
		}
		for (JDLCommand jdlCommand : initBlocks) {
			if (jdlCommand.getBlockType().equals(JDLCommandType.IO)) {
				addIOCommand(jobId, task, (IOCommand) jdlCommand, schedPath);
			} else {
				addRemoteCommand(jobId, task, (RemoteCommand) jdlCommand, schedPath);
			}
		}
	}

	private static Command stageOutCommand(String remoteFile, String localFile) {
		String scpCommand = "su $UUID ; " + "scp " + SSH_SCP_PRECOMMAND + " -P $" + AbstractResource.ENV_SSH_PORT
				+ " -i $" + AbstractResource.ENV_PRIVATE_KEY_FILE + " $" + AbstractResource.ENV_SSH_USER + "@" + "$"
				+ AbstractResource.ENV_HOST + ":" + remoteFile + " " + localFile;
		return new Command(scpCommand, Command.Type.LOCAL);
	}

	private static Command mkdirLocalFolder(String folder) {
		String mkdirCommand = "su $UserID ; " + "mkdir -p " + folder;
		return new Command(mkdirCommand, Command.Type.LOCAL);
	}
}
