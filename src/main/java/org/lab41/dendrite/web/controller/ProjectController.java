package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.beans.CreateProjectBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/api")
public class ProjectController {

    Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    MetaGraphService metaGraphService;

    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getProjects() {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        Map<String, Object> response = new HashMap<>();
        ArrayList<Object> projects = new ArrayList<>();
        response.put("projects", projects);

        for(ProjectMetadata projectMetadata: tx.getProjects()) {
            projects.add(getProjectMap(projectMetadata));
        }

        // Commit must come after all graph access.
        tx.commit();

        return response;
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getProject(@PathVariable String projectId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();
        ProjectMetadata projectMetadata = tx.getProject(projectId);

        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.put("project", getProjectMap(projectMetadata));

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @RequestMapping(value = "/projects", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createProject(@Valid @RequestBody CreateProjectBean item,
                                                             BindingResult result,
                                                             UriComponentsBuilder builder) {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("status", "error");
            response.put("msg", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String name = item.getName();

        MetaGraphTx tx = metaGraphService.newTransaction();

        ProjectMetadata projectMetadata = tx.createProject(name, item.createGraph());

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(builder.path("/{projectId}").buildAndExpand(projectMetadata.getId()).toUri());

        response.put("project", getProjectMap(projectMetadata));

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteProject(@PathVariable String projectId) {

        MetaGraphTx tx = metaGraphService.newTransaction();

        Map<String, Object> response = new HashMap<>();

        ProjectMetadata projectMetadata = tx.getProject(projectId);
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        try {
            tx.deleteProject(projectMetadata);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("msg", e.toString());
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        response.put("msg", "deleted");

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Map<String, Object> getProjectMap(ProjectMetadata projectMetadata) {
        Map<String, Object> project = new HashMap<>();

        String id = projectMetadata.getId();
        project.put("_id", id);
        project.put("name", projectMetadata.getName());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date creationTime = projectMetadata.getCreationTime();
        if (creationTime != null) { project.put("creationTime", df.format(creationTime)); }

        BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
        if (branchMetadata != null) {
            project.put("current_branch", branchMetadata.getId());

            GraphMetadata graphMetadata = branchMetadata.getGraph();
            if (graphMetadata != null) {
                project.put("current_graph", graphMetadata.getId());
            }
        }

        return project;
    }
}
