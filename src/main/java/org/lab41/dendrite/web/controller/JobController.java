package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/api")
public class JobController {

    @Autowired
    MetaGraphService metaGraphService;

    @RequestMapping(value = "/jobs", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getJobs() {

        MetaGraphTx tx = metaGraphService.newTransaction();

        List<Map<String, Object>> jobs = new ArrayList<>();
        for (JobMetadata jobMetadata: tx.getJobs()) {
            jobs.add(getJobMap(jobMetadata));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("jobs", jobs);

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/jobs/{jobId}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable String jobId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();
        JobMetadata jobMetadata = tx.getJob(jobId);

        if (jobMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find job '" + jobId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.put("job", getJobMap(jobMetadata));

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/jobs", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getJobs(@PathVariable String projectId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();
        ProjectMetadata project = tx.getProject(projectId);

        if (project == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> jobs = new ArrayList<>();
        for (JobMetadata jobMetadata: project.getJobs()) {
            jobs.add(getJobMap(jobMetadata));
        }

        response.put("jobs", jobs);

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/jobs/{jobId}", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable String jobId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();
        JobMetadata jobMetadata = tx.getJob(jobId);

        if (jobMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find job '" + jobId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        tx.deleteJob(jobMetadata);

        response.put("msg", "deleted");

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Map<String, Object> getJobMap(JobMetadata jobMetadata) {
        Map<String, Object> job = new HashMap<>();

        String id = jobMetadata.getId();
        job.put("_id", id);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date creationTime = jobMetadata.getCreationTime();
        if (creationTime != null) { job.put("creationTime", df.format(creationTime)); }

        Date doneTime = jobMetadata.getDoneTime();
        if (doneTime != null) { job.put("doneTime", df.format(doneTime)); }

        JobMetadata parentJobMetadata = jobMetadata.getParentJob();
        if (parentJobMetadata != null) {
            job.put("parentJob", parentJobMetadata.getId());
        }
        job.put("name", jobMetadata.getName());
        job.put("state", jobMetadata.getState());
        job.put("progress", jobMetadata.getProgress());
        job.put("msg", jobMetadata.getMessage());
        job.put("mapreduceJobId", jobMetadata.getMapreduceJobId());

        return job;
    }
}
