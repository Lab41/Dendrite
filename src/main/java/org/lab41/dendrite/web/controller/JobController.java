package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.models.GraphMetadata;
import org.lab41.dendrite.models.JobMetadata;
import org.lab41.dendrite.models.ProjectMetadata;
import org.lab41.dendrite.services.MetadataService;
import org.lab41.dendrite.services.MetadataTx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class JobController {

    @Autowired
    MetadataService metadataService;

    @RequestMapping(value = "/jobs", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getJobs() {

        MetadataTx tx = metadataService.newTransaction();

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
        MetadataTx tx = metadataService.newTransaction();
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
        MetadataTx tx = metadataService.newTransaction();
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
        MetadataTx tx = metadataService.newTransaction();
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
