package org.fogbowcloud.app.model;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.*;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowRequirementsHelper;
import org.ourgrid.common.specification.job.IOEntry;
import org.ourgrid.common.specification.job.JobSpecification;
import org.ourgrid.common.specification.job.TaskSpecification;
import org.ourgrid.common.specification.main.CommonCompiler;
import org.ourgrid.common.specification.main.CommonCompiler.FileType;
import org.ourgrid.common.specification.main.CompilerException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JDFTasks {

    //FIXME: what is this?
    private static final String SANDBOX = "sandbox";

    //FIXME: what is this?
    private static final String LOCAL_OUTPUT_FOLDER = "local_output";
    private static final String REMOTE_OUTPUT_FOLDER = "remote_output_folder";

    private static final String PRIVATE_KEY_FILEPATH = "private_key_filepath";

    private static String standardImage = "fogbow-ubuntu";

    private static final String PUBLIC_KEY_CONSTANT = "public_key";
    private final static String SSH_SCP_PRECOMMAND = "-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no";

    private static final Logger LOGGER = Logger.getLogger(JDFTasks.class);

    /**
     *
     * @param jobID
     * @param jdfFilePath
     * @param schedPath
     * @param properties
     * @return
     * @throws IllegalArgumentException
     * @throws CompilerException
     */
    public static List<Task> getTasksFromJDFFile(String jobID, String jdfFilePath, String schedPath, Properties properties) throws CompilerException {

        ArrayList<Task> taskList = new ArrayList<Task>();

        if (jdfFilePath == null) {
            throw new IllegalArgumentException("jdfFilePath cannot be null");
        }

        File file = new File(jdfFilePath);

        if (file.exists()) {
            if (file.canRead()) {
                //Compiling JDF
                CommonCompiler commonCompiler = new CommonCompiler();
                commonCompiler.compile(jdfFilePath, FileType.JDF);

                JobSpecification jobSpec = (JobSpecification) commonCompiler.getResult().get(0);

                //Mapping attributes

                //FIXME: what does this block do?
                String jobRequirementes = jobSpec.getRequirements();
                jobRequirementes = jobRequirementes.replace("(", "").replace(")", "");
                String image = standardImage;
                for (String req : jobRequirementes.split("and")) {
                    if (req.trim().startsWith("image")) {
                        image = req.split("==")[1].trim();
                    }
                }

                Specification spec = new Specification(image, properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_USERNAME),
                        properties.getProperty(PUBLIC_KEY_CONSTANT), properties.getProperty(PRIVATE_KEY_FILEPATH));
                LOGGER.debug(properties.getProperty(AppPropertiesConstants.INFRA_FOGBOW_USERNAME));

                int i = 0;
                for (String req : jobRequirementes.split("and")) {
                    if (i == 0 && !req.trim().startsWith("image")) {
                        i++;
                        spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, req);

                    } else if (!req.trim().startsWith("image")) {
                        spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, spec.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS) + " && " + req);
                    }
                }

                spec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUEST_TYPE, "one-time");

                int taskID = 0;
                for (TaskSpecification taskSpec : jobSpec.getTaskSpecs()) {

                    Task task = new TaskImpl("TaskNumber" + taskID, spec);
                    task.putMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER, properties.getProperty(REMOTE_OUTPUT_FOLDER));
                    task.putMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER, schedPath + properties.getProperty(LOCAL_OUTPUT_FOLDER));
                    task.putMetadata(TaskImpl.METADATA_SANDBOX, SANDBOX);
                    task.putMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH, properties.getProperty(REMOTE_OUTPUT_FOLDER) + "/exit");

                    parseInputBlocks(jobID, taskSpec, task, schedPath);
                    parseExecutable(jobID, taskSpec, task);
                    parseOutputBlocks(jobID, taskSpec, task, schedPath);
                    parseEpilogue(jobID, taskSpec, task);

                    taskList.add(task);
                    LOGGER.debug("Task spec: " + task.getSpecification().toString());
                    taskID++;
                }

            } else {
                throw new IllegalArgumentException("Unable to read file: " + file.getAbsolutePath() + " check your permissions.");
            }
        } else {
            throw new IllegalArgumentException("File: " + file.getAbsolutePath() + " does not exists.");
        }

        return taskList;
    }

    /**
     * This method translates the JDF remote executable command into the JDL format
     *
     * @param jobID
     * @param taskSpec The task specification {@link TaskSpecification}
     * @param task     The output expression containing the JDL job
     * @throws IllegalArgumentException
     */
    private static void parseExecutable(String jobID, TaskSpecification taskSpec, Task task) throws IllegalArgumentException {

        String exec = taskSpec.getRemoteExec();
        if (exec.contains(";")) {
            throw new IllegalArgumentException("Task \n-------\n" + taskSpec + " \n-------\ncould not be parsed as it contains more than one executable command.");
        }

        exec = parseEnvironmentVariables(jobID, task.getId(), exec);

        Command command = new Command("\"" + exec + " ; echo 0 > " + task.getMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH) + "\"", Command.Type.REMOTE);
        LOGGER.debug("Remote command:" + exec);
        task.addCommand(command);
    }

    /**
     * This method replaces environment variables defined in the JDF to its
     * values.
     *
     * @param string A string representing the remote executable command of the JDF job
     * @return A string with the environment variables replaced
     */
    private static String parseEnvironmentVariables(String jobID, String taskID, String string) {
        //FIXME: do we still need playpen and and storage variables ?
        return string.replaceAll("\\$JOB", jobID).replaceAll("\\$TASK",
                taskID).replaceAll("\\$PLAYPEN", ".").replaceAll("\\$STORAGE", ".");
    }

    /**
     * This method translates the JDF sabotage check command to the
     * JDL epilogue command
     *
     * @param jobID
     * @param taskRec The task specification {@link TaskSpecification}
     * @param task    The output expression containing the JDL job
     */
    private static void parseEpilogue(String jobID, TaskSpecification taskRec, Task task) {

        String sabotageCheck = taskRec.getSabotageCheck();
        if (sabotageCheck == null || (sabotageCheck.trim().length() == 0)) {
            return;
        }
        sabotageCheck = parseEnvironmentVariables(jobID, task.getId(), sabotageCheck);
        LOGGER.debug("Epilogue command:" + sabotageCheck);

        Command command = new Command(sabotageCheck, Command.Type.EPILOGUE);
        task.addCommand(command);
    }

    /**
     * This method translates the Ourgrid input IOBlocks to JDL InputSandbox
     *
     * @param jobID
     * @param taskSpec The task specification {@link TaskSpecification}
     * @param task     The output expression containing the JDL job
     */
    private static void parseInputBlocks(String jobID, TaskSpecification taskSpec, Task task, String schedPath) {

        List<IOEntry> initBlocks = taskSpec.getInitBlock().getEntry("");
        if (initBlocks == null) {
            return;
        }
        for (IOEntry ioEntry : initBlocks) {
            String sourceFile = parseEnvironmentVariables(jobID, task.getId(), ioEntry.getSourceFile());
            String destination = parseEnvironmentVariables(jobID, task.getId(), ioEntry.getDestination());

            task.addCommand(mkdirRemoteFolder(getDirectoryTree(destination)));
            if (sourceFile.startsWith("/")) {
                task.addCommand(stageInCommand(sourceFile, destination));
            } else {
                task.addCommand(stageInCommand(schedPath + sourceFile, destination));
            }
            LOGGER.debug("Input command:" + stageInCommand(schedPath + sourceFile, destination).getCommand());
        }
    }

    public static String getDirectoryTree(String destination) {
        int lastDir = destination.lastIndexOf("/");
        return destination.substring(0, lastDir);
    }

    private static Command mkdirRemoteFolder(String folder) {
        String mkdirCommand = "ssh " + SSH_SCP_PRECOMMAND + " -p $" + Resource.ENV_SSH_PORT + " -i $"
                + Resource.ENV_PRIVATE_KEY_FILE + " $" + Resource.ENV_SSH_USER + "@" + "$" + Resource.ENV_HOST
                + " mkdir -p " + folder;
        return new Command(mkdirCommand, Command.Type.PROLOGUE);
    }

    private static Command stageInCommand(String localFile, String remoteFile) {
        String scpCommand = "scp " + SSH_SCP_PRECOMMAND + " -P $" + Resource.ENV_SSH_PORT + " -i $" + Resource.ENV_PRIVATE_KEY_FILE + " " + localFile + " $"
                + Resource.ENV_SSH_USER + "@" + "$" + Resource.ENV_HOST + ":" + remoteFile;
        return new Command(scpCommand, Command.Type.PROLOGUE);
    }

    /**
     * This method translates the Ourgrid output IOBlocks to JDL InputSandbox
     *
     * @param jobID
     * @param taskSpec The task specification {@link TaskSpecification}
     * @param task     The output expression containing the JDL job
     */
    private static void parseOutputBlocks(String jobID, TaskSpecification taskSpec, Task task, String schedPath) {

        List<IOEntry> finalBlocks = taskSpec.getFinalBlock().getEntry("");
        if (finalBlocks == null) {
            return;
        }
        for (IOEntry ioEntry : finalBlocks) {
            String sourceFile = parseEnvironmentVariables(jobID, task.getId(), ioEntry.getSourceFile());
            String destination = parseEnvironmentVariables(jobID, task.getId(), ioEntry.getDestination());
            task.addCommand(mkdirLocalFolder(getDirectoryTree(schedPath + destination)));
            if (destination.startsWith("/")) {
                task.addCommand(stageOutCommand(sourceFile, destination));
            } else {
                task.addCommand(stageOutCommand(sourceFile, schedPath + destination));
            }
            LOGGER.debug("Output command:" + stageOutCommand(sourceFile, schedPath + destination).getCommand());

        }
    }

    private static Command stageOutCommand(String remoteFile, String localFile) {
        String scpCommand = "scp " + SSH_SCP_PRECOMMAND + " -P $" + Resource.ENV_SSH_PORT + " -i $" + Resource.ENV_PRIVATE_KEY_FILE + " $"
                + Resource.ENV_SSH_USER + "@" + "$" + Resource.ENV_HOST + ":" + remoteFile + " " + localFile;
        return new Command(scpCommand, Command.Type.EPILOGUE);
    }

    private static Command mkdirLocalFolder(String folder) {
        String mkdirCommand = "mkdir -p " + folder;
        return new Command(mkdirCommand, Command.Type.PROLOGUE);
    }

}
