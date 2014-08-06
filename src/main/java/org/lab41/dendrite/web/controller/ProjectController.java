package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.metagraph.models.UserMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.requests.AddUserToProjectRequest;
import org.lab41.dendrite.web.requests.CreateProjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/api")
public class ProjectController {

    Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    MetaGraphService metaGraphService;

    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getProjects(Principal principal) {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        UserMetadata userMetadata = tx.getUser(principal.getName());

        Map<String, Object> response = new HashMap<>();
        ArrayList<Object> projects = new ArrayList<>();
        response.put("projects", projects);

        Iterable<ProjectMetadata> itprojects = userMetadata.getOwnedProjects();
        for(ProjectMetadata projectMetadata: itprojects) {
            projects.add(getProjectMap(projectMetadata));
        }

        // Commit must come after all graph access.
        tx.commit();

        return response;
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
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

        // FIXME: Temporary hack to force loading the graph until the UI can handle it occurring asynchronously.
        metaGraphService.getGraph(projectMetadata.getCurrentGraph().getId());

        response.put("project", getProjectMap(projectMetadata));

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @RequestMapping(value = "/projects", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createProject(@Valid @RequestBody CreateProjectRequest item,
                                                             BindingResult result,
                                                             UriComponentsBuilder builder,
                                                             Principal principal) {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("status", "error");
            response.put("msg", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String name = item.getName();

        MetaGraphTx tx = metaGraphService.newTransaction();

        UserMetadata userMetadata = tx.getUserByName(principal.getName());
        if (userMetadata == null) {
            response.put("status", "error");
            response.put("msg", "user is authenticated, but cannot find user record");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ProjectMetadata projectMetadata = tx.createProject(name, userMetadata, item.createGraph());

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(builder.path("/{projectId}").buildAndExpand(projectMetadata.getId()).toUri());

        response.put("project", getProjectMap(projectMetadata));

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
    }


    @PreAuthorize(value="hasPermissions(#projectId, 'project', 'read')")
    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteProject(@PathVariable String projectId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();

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

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/users", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addUser(@PathVariable String projectId,
                                                       @Valid @RequestBody AddUserToProjectRequest item,
                                                       BindingResult result,
                                                       UriComponentsBuilder builder) {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("status", "error");
            response.put("msg", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        MetaGraphTx tx = metaGraphService.newTransaction();

        ProjectMetadata projectMetadata = tx.getProject(projectId);
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        UserMetadata otherUserMetadata = tx.getUser(item.getUserId());
        if (otherUserMetadata == null) {
            response.put("status", "error");
            response.put("msg", "unknown user '" + item.getUserId() + "'");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        projectMetadata.addUser(otherUserMetadata);

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
