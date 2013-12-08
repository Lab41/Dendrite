package org.lab41.dendrite.web.controller;

import org.codehaus.jettison.json.JSONException;
import org.lab41.dendrite.models.GraphMetadata;
import org.lab41.dendrite.models.JobMetadata;
import org.lab41.dendrite.models.ProjectMetadata;
import org.lab41.dendrite.rexster.DendriteRexsterApplication;
import org.lab41.dendrite.services.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class ProjectController {

    Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    MetadataService metadataService;

    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getProjects() throws JSONException {

        Map<String, Object> response = new HashMap<>();
        ArrayList<Object> projects = new ArrayList<>();
        response.put("projects", projects);

        for(ProjectMetadata projectMetadata: metadataService.getProjects()) {
            projects.add(getProjectMap(projectMetadata));
        }

        return response;
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getProject(@PathVariable String projectId) throws Exception {

        ProjectMetadata projectMetadata = metadataService.getProject(projectId);

        if (projectMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("project", getProjectMap(projectMetadata));

        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @RequestMapping(value = "/projects", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createProject(@Valid @RequestBody ProjectBean item, BindingResult result, UriComponentsBuilder builder) throws Exception {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("status", "error");
            response.put("msg", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String name = item.getName();
        GraphBean graph = item.getGraph();

        ProjectMetadata projectMetadata = metadataService.createProject();
        projectMetadata.setName(name);

        GraphMetadata graphMetadata = projectMetadata.getCurrentGraph();
        graphMetadata.setName(graph.getName());
        graphMetadata.setBackend(graph.getName());
        graphMetadata.setDirectory(graph.getDirectory());
        graphMetadata.setHostname(graph.getHostname());
        graphMetadata.setPort(graph.getPort());
        graphMetadata.setTablename(graph.getTablename());

        metadataService.commit();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(builder.path("/{projectId}").buildAndExpand(projectMetadata.getId()).toUri());

        response.put("project", getProjectMap(projectMetadata));

        return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteProject(@PathVariable String projectId) {

        ProjectMetadata projectMetadata = metadataService.getProject(projectId);
        if (projectMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        metadataService.deleteProject(projectMetadata);
        metadataService.commit();

        Map<String, Object> response = new HashMap<>();
        response.put("msg", "deleted");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Map<String, Object> getProjectMap(ProjectMetadata projectMetadata) {
        Map<String, Object> project = new HashMap<>();

        String id = projectMetadata.getId();
        project.put("_id", id);
        project.put("name", projectMetadata.getName());
        project.put("current_graph", projectMetadata.getCurrentGraph().getId());

        return project;
    }
}
