package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.models.GraphMetadata;
import org.lab41.dendrite.models.JobMetadata;
import org.lab41.dendrite.models.ProjectMetadata;
import org.lab41.dendrite.services.MetadataService;
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
    public ResponseEntity<List<Map<String, Object>>> getJobs() {

        List<Map<String, Object>> jobs = new ArrayList<>();
        for (JobMetadata jobMetadata: metadataService.getJobs()) {
            jobs.add(getJobMap(jobMetadata));
        }

        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @RequestMapping(value = "/jobs/{jobId}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable String jobId) {

        JobMetadata jobMetadata = metadataService.getJob(jobId);

        if (jobMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("job", getJobMap(jobMetadata));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/jobs", method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> getJobs(@PathVariable String projectId) {

        ProjectMetadata project = metadataService.getProject(projectId);

        if (project == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> jobs = new ArrayList<>();
        for (JobMetadata jobMetadata: project.getJobs()) {
            jobs.add(getJobMap(jobMetadata));
        }

        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @RequestMapping(value = "/jobs/{jobId}", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable String jobId) throws Exception {

        JobMetadata jobMetadata = metadataService.getJob(jobId);
        if (jobMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        metadataService.deleteJob(jobMetadata);
        metadataService.commit();

        Map<String, Object> response = new HashMap<>();
        response.put("msg", "deleted");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Map<String, Object> getJobMap(JobMetadata jobMetadata) {
        Map<String, Object> project = new HashMap<>();

        String id = jobMetadata.getId();
        project.put("_id", id);
        project.put("name", jobMetadata.getName());
        project.put("state", jobMetadata.getState().toString());
        project.put("progress", jobMetadata.getProgress());

        return project;
    }
}
