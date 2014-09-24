package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.NotFound;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.metagraph.models.UserMetadata;
import org.lab41.dendrite.web.requests.AddUserToProjectRequest;
import org.lab41.dendrite.web.requests.CreateProjectRequest;
import org.lab41.dendrite.web.responses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.*;

/**
 * The {@code ProjectController} is the main user interface for creating and manipulating projects.
 */
@Controller
@RequestMapping("/api")
public class ProjectController extends AbstractController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    // Note this doesn't use @PreAuthorize on purpose because it'll only show the user's projects.
    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    @ResponseBody
    public GetProjectsResponse getProjects(Principal principal) {

        // This needs to be a read/write transaction as we might make a user.
        MetaGraphTx tx = metaGraphService.buildTransaction().start();
        GetProjectsResponse getProjectsResponse;

        try {
            UserMetadata userMetadata = tx.getOrCreateUser(principal);

            List<GetProjectResponse> projects = new ArrayList<>();
            for (ProjectMetadata projectMetadata : userMetadata.getProjects()) {
                projects.add(new GetProjectResponse(projectMetadata));
            }

            getProjectsResponse = new GetProjectsResponse(projects);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        return getProjectsResponse;
    }

    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.GET)
    @ResponseBody
    public GetProjectResponse getProject(@PathVariable String projectId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();
        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            // FIXME: Temporary hack to force loading the graph until the UI can handle it occurring asynchronously.
            metaGraphService.getDendriteGraph(projectMetadata.getCurrentGraph().getId());

            return new GetProjectResponse(projectMetadata);
        } finally {
            tx.commit();
        }
    }

    //@PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    // FIXME: Right now any authenticated user can create a project.
    @RequestMapping(value = "/projects", method = RequestMethod.POST)
    public ResponseEntity<GetProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest item,
                                                             BindingResult result,
                                                             UriComponentsBuilder builder,
                                                             Principal principal) throws BindingException, NotFound {

        if (result.hasErrors()) {
            throw new BindingException(result);
        }

        String name = item.getName();
        boolean createGraph = item.createGraph();

        MetaGraphTx tx = metaGraphService.newTransaction();
        GetProjectResponse getProjectResponse;
        HttpHeaders headers = new HttpHeaders();

        try {
            UserMetadata userMetadata = tx.getOrCreateUser(principal);
            if (userMetadata == null) {
                throw new NotFound(UserMetadata.class, principal.getName());
            }

            ProjectMetadata projectMetadata = tx.createProject(name, userMetadata, createGraph);

            headers.setLocation(builder.path("/projects/{projectId}").buildAndExpand(projectMetadata.getId()).toUri());

            getProjectResponse = new GetProjectResponse(projectMetadata);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(getProjectResponse, headers, HttpStatus.CREATED);
    }


    @PreAuthorize("hasPermissions(#projectId, 'project', 'read')")
    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.DELETE)
    @ResponseBody
    public DeleteProjectResponse deleteProject(@PathVariable String projectId) throws Exception {

        MetaGraphTx tx = metaGraphService.newTransaction();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            tx.deleteProject(projectMetadata);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        return new DeleteProjectResponse();
    }

    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/users", method = RequestMethod.GET)
    @ResponseBody
    public GetUsersResponse addUser(@PathVariable String projectId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            List<GetUserResponse> users = new ArrayList<>();

            for (UserMetadata userMetadata : projectMetadata.getUsers()) {
                users.add(new GetUserResponse(userMetadata));
            }

            return new GetUsersResponse(users);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/users", method = RequestMethod.POST)
    @ResponseBody
    public AddUserToProjectResponse addUser(@PathVariable String projectId,
                                            @Valid @RequestBody AddUserToProjectRequest item,
                                            BindingResult result) throws BindingException, NotFound {

        if (result.hasErrors()) {
            throw new BindingException(result);
        }

        MetaGraphTx tx = metaGraphService.newTransaction();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            UserMetadata otherUserMetadata = tx.getUserByName(item.getName());
            if (otherUserMetadata == null) {
                throw new NotFound(UserMetadata.class);
            }

            projectMetadata.addUser(otherUserMetadata);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        // Commit must come after all graph access.
        tx.commit();

        return new AddUserToProjectResponse();
    }
}
