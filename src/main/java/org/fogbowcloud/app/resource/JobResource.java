package org.fogbowcloud.app.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.app.model.JDFJob;
import org.fogbowcloud.app.restlet.JDFSchedulerApplication;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.ourgrid.common.specification.main.CompilerException;
import org.restlet.data.MediaType;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;

public class JobResource extends ServerResource {
    private static final String JOB_LIST = "Jobs";

    private static final String JOB_TASKS = "Tasks";

    private static final String JOB_ID = "id";

    private static final String JOB_FRIENDLY = "name";

    private static final String STATE = "state";

    private static final String TASK_ID = "taskid";

    private static final String JOBPATH = "jobpath";

    private static final String FRIENDLY = "friendly";

    private static final String SCHED_PATH = "schedpath";

    private static final String JDF_FILE_PATH = "jdffilepath";

    private static final Logger LOGGER = Logger.getLogger(JobResource.class);

    JSONArray jobTasks = new JSONArray();


    @Get
    public Representation fetch() throws Exception {
        LOGGER.info("Getting Jobs...");
        String jobId = (String) getRequest().getAttributes().get(JOBPATH);
        LOGGER.debug("JobId is " + jobId);
        JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();
        JSONObject jsonJob = new JSONObject();

        JSONArray jobs = new JSONArray();

        if (jobId == null) {
            for (JDFJob job : application.getAllJobs()) {
                JSONObject jJob = new JSONObject();
                if (job.getName() != null) {
                    jJob.put("id", job.getId());
                    jJob.put("name", job.getName());
                    jJob.put("readytasks", job.getByState(TaskState.READY).size());
                } else {
                    jJob.put("id: ", job.getId());
                    jJob.put("readytasks", job.getByState(TaskState.READY).size());
                }
                jobs.put(jJob);
            }

            jsonJob.put(JOB_LIST, jobs);

            LOGGER.debug("My info Is: " + jsonJob.toString());

            StringRepresentation result = new StringRepresentation(jsonJob.toString(), MediaType.TEXT_PLAIN);

            return result;
        }

        JDFJob job = application.getJobById(jobId);
        if (job == null) {
            job = application.getJobByName(jobId);
            if (job == null) {
                throw new ResourceException(404);
            }
            jsonJob.put(JOB_FRIENDLY, jobId);
            jsonJob.put(JOB_ID, job.getId());
        } else {
            jsonJob.put(JOB_ID, jobId);
            jsonJob.put(JOB_FRIENDLY, job.getName());
        }
        LOGGER.debug("JobID " + jobId + " is of job " + job);


        for (Task task : job.getByState(TaskState.READY)) {
            LOGGER.debug("Task Id is:" + task.getId());
            JSONObject jTask = new JSONObject();
            jTask.put(TASK_ID, task.getId());
            jTask.put(STATE, TaskState.READY);
            jobTasks.put(jTask);
        }
        ;
        for (Task task : job.getByState(TaskState.RUNNING)) {
            JSONObject jTask = new JSONObject();
            jTask.put(TASK_ID, task.getId());
            jTask.put(STATE, TaskState.RUNNING);
            jobTasks.put(jTask);
        }
        ;
        for (Task task : job.getByState(TaskState.COMPLETED)) {
            JSONObject jTask = new JSONObject();
            jTask.put(TASK_ID, task.getId());
            jTask.put(STATE, TaskState.COMPLETED);
            jobTasks.put(jTask);
        }
        ;
        for (Task task : job.getByState(TaskState.FAILED)) {
            JSONObject jTask = new JSONObject();
            jTask.put(TASK_ID, task.getId());
            jTask.put(STATE, TaskState.FAILED);
            jobTasks.put(jTask);
        }
        ;

        jsonJob.put(JOB_TASKS, jobTasks);
        return new StringRepresentation(jsonJob.toString(), MediaType.TEXT_PLAIN);
    }

    @Post
    public StringRepresentation addJob(Representation entity) throws IOException, FileUploadException {
    	if (entity == null && !MediaType.MULTIPART_FORM_DATA
    			.equals(entity.getMediaType(), true)) {
    		throw new ResourceException(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
    	}
    	
    	DiskFileItemFactory factory = new DiskFileItemFactory();
    	factory.setSizeThreshold(1000240);
    	RestletFileUpload upload = new RestletFileUpload(factory);
    	FileItemIterator fileIterator = upload.getItemIterator(entity);
    	
    	Map<String, File> fileMap = new HashMap<String, File>();
    	fileMap.put(JDF_FILE_PATH, null);
    	
    	Map<String, String> fieldMap = new HashMap<String, String>();
    	fieldMap.put(SCHED_PATH, null);
    	fieldMap.put(FRIENDLY, null);
    	
    	loadFields(fileIterator, fieldMap, fileMap);
    	
    	File jdf = fileMap.get(JDF_FILE_PATH);
		if (jdf == null) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
		}
		
		String friendlyName = fieldMap.get(FRIENDLY);
		String schedPath = fieldMap.get(SCHED_PATH);

        JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();

        if (application.getJobByName(friendlyName) != null) {
            throw new ResourceException(HttpStatus.SC_NOT_ACCEPTABLE, "Friendly name already in use", 
            		HttpStatus.getStatusText(HttpStatus.SC_NOT_ACCEPTABLE), friendlyName);
        }

        String jdfAbsolutePath = jdf.getAbsolutePath();
        
        try {
        	String jobId;
            if (friendlyName != null) {
            	LOGGER.debug("jdfpath <" + jdfAbsolutePath + ">" + " friendlyName <"+ friendlyName + ">" + " schedPath <" + schedPath +">");
                jobId = application.addJob(jdfAbsolutePath, schedPath, friendlyName);
            } else {
            	LOGGER.debug("jdfpath <" + jdfAbsolutePath + ">" + " schedPath <" + schedPath +">");
                jobId = application.addJob(jdfAbsolutePath, schedPath);
            }
            return new StringRepresentation(jobId, MediaType.TEXT_PLAIN);
        } catch (CompilerException ce) {
        	LOGGER.debug(ce.getMessage(), ce);
        	throw new ResourceException(HttpStatus.SC_INTERNAL_SERVER_ERROR, ce);
        } catch (IllegalArgumentException iae) {
        	LOGGER.debug(iae.getMessage(), iae);
        	throw new ResourceException(HttpStatus.SC_BAD_REQUEST, iae);
        }
        
        
    }
    
    private void loadFields(FileItemIterator fileIterator, Map<String, 
    		String> formFieldstoLoad, Map<String, File> filesToLoad) throws FileUploadException, IOException {
    	
    	while (fileIterator.hasNext()) {
    		FileItemStream fi = fileIterator.next();
    		String fieldName = fi.getFieldName();
    		if (fi.isFormField()) {
    			if (formFieldstoLoad.containsKey(fieldName)) {
    				formFieldstoLoad.put(fieldName, IOUtils.toString(fi.openStream()));
    			}
    		} else {
    			if (filesToLoad.containsKey(fieldName)) {
        			String fileContent = IOUtils.toString(fi.openStream());
        			String fileName = fi.getName();
        			File file = createTmpFile(fileContent, fileName);
    				filesToLoad.put(fieldName, file);
        		}
    		}
    	}
    }
    
    private File createTmpFile(String content, String fileName) throws IOException {
		File tempFile = File.createTempFile(fileName, null);
		IOUtils.write(content, new FileOutputStream(tempFile));
		return tempFile;
	}

    @Delete
    public StringRepresentation stopJob() {
        JDFSchedulerApplication application = (JDFSchedulerApplication) getApplication();

        String JDFString = (String) getRequest().getAttributes().get(JOBPATH);

        LOGGER.debug("Got JDF File: " + JDFString);

        String jobId = application.stopJob(JDFString);

        if (jobId == null) {
            throw new ResourceException(404);
        }

        return new StringRepresentation(jobId, MediaType.TEXT_PLAIN);
    }
}
