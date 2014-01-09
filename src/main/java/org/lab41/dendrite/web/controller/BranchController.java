package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.beans.UpdateCurrentBranchBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class BranchController {

    @Autowired
    MetaGraphService metaGraphService;

    @RequestMapping(value = "/branches", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getBranches() {

        MetaGraphTx tx = metaGraphService.newTransaction();

        List<Map<String, Object>> branches = new ArrayList<>();
        for (BranchMetadata branchMetadata: tx.getBranches()) {
            branches.add(getBranchMap(branchMetadata));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("branches", branches);

        // Commit must come after all branch access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/branches/{branchId}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getBranch(@PathVariable String branchId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();

        BranchMetadata branchMetadata = tx.getBranch(branchId);

        if (branchMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find branch '" + branchId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.put("branch", getBranchMap(branchMetadata));

        // Commit must come after all branch access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/branches/{branchId}", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteBranch(@PathVariable String branchId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();
        BranchMetadata branchMetadata = tx.getBranch(branchId);

        if (branchMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find branch '" + branchId + "'");
            tx.rollback();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            tx.deleteBranch(branchMetadata);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("msg", e.toString());
            tx.rollback();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        response.put("msg", "deleted");

        // Commit must come after all branch access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/branches", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getBranches(@PathVariable String projectId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();
        ProjectMetadata projectMetadata = tx.getProject(projectId);

        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> branches = new ArrayList<>();
        for (BranchMetadata branchMetadata: projectMetadata.getBranches()) {
            branches.add(getBranchMap(branchMetadata));
        }

        // Commit must come after all branch access.
        tx.commit();

        response.put("branches", branches);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/branches/{branchName}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getBranch(@PathVariable String projectId,
                                                         @PathVariable String branchName) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();

        ProjectMetadata projectMetadata = tx.getProject(projectId);
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        BranchMetadata branchMetadata = projectMetadata.getBranchByName(branchName);
        if (branchMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find branch '" + branchName + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.put("branch", getBranchMap(branchMetadata));

        // Commit must come after all branch access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @RequestMapping(value = "/projects/{projectId}/branches/{branchName}", method = RequestMethod.PUT)
    public ResponseEntity<Map<String, Object>> createBranch(@PathVariable String projectId,
                                                            @PathVariable String branchName,
                                                            UriComponentsBuilder builder) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();
        ProjectMetadata projectMetadata = tx.getProject(projectId);

        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        BranchMetadata branchMetadata = tx.createBranch(branchName, projectMetadata);
        response.put("branch", getBranchMap(branchMetadata));

        // Commit must come after all branch access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/current-branch", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getCurrentBranch(@PathVariable String projectId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();

        ProjectMetadata projectMetadata = tx.getProject(projectId);
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
        if (branchMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find branch");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.put("branch", getBranchMap(branchMetadata));

        // Commit must come after all branch access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/current-branch", method = RequestMethod.PUT)
    public ResponseEntity<Map<String, Object>> getCurrentBranch(@PathVariable String projectId,
                                                               @Valid @RequestBody UpdateCurrentBranchBean item,
                                                               BindingResult result) {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("error", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String branchName = item.getBranchName();

        MetaGraphTx tx = metaGraphService.newTransaction();

        ProjectMetadata projectMetadata = tx.getProject(projectId);
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        BranchMetadata branchMetadata = projectMetadata.getBranchByName(branchName);
        if (branchMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find branch '" + branchName + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        projectMetadata.setCurrentBranch(branchMetadata);

        response.put("msg", "current branch changed");

        // Commit must come after all branch access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Map<String, Object> getBranchMap(BranchMetadata branchMetadata) {
        Map<String, Object> branch = new HashMap<>();

        branch.put("_id", branchMetadata.getId());
        branch.put("name", branchMetadata.getName());

        ProjectMetadata projectMetadata = branchMetadata.getProject();
        if (projectMetadata != null) {
            branch.put("projectId", branchMetadata.getProject().getId());
        }

        GraphMetadata graphMetadata = branchMetadata.getGraph();
        if (graphMetadata != null) {
            branch.put("graphId", branchMetadata.getGraph().getId());
        }

        return branch;
    }
}
