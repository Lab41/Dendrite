package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.jobs.BranchCommitJob;
import org.lab41.dendrite.jobs.BranchCommitSubsetJob;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.beans.CreateBranchBean;
import org.lab41.dendrite.web.beans.CreateGraphSubsetNSteps;
import org.lab41.dendrite.web.beans.UpdateCurrentBranchBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/api")
public class BranchController {

    @Autowired
    MetaGraphService metaGraphService;

    @Autowired
    TaskExecutor taskExecutor;

    @RequestMapping(value = "/branches", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getBranches() {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

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
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

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
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();
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
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

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
                                                            @Valid @RequestBody CreateBranchBean item,
                                                            BindingResult result) {

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

        String graphId = item.getGraphId();

        BranchMetadata branchMetadata;

        if (graphId == null) {
            branchMetadata = tx.createBranch(branchName, projectMetadata);
        } else {
            GraphMetadata graphMetadata = tx.getGraph(graphId);
            if (graphMetadata == null) {
                response.put("status", "error");
                response.put("msg", "could not find project '" + projectId + "'");
                tx.rollback();
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            branchMetadata = tx.createBranch(branchName, graphMetadata);
        }

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        // Commit must come after all branch access.
        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        BranchCommitJob branchCommitJob = new BranchCommitJob(
                metaGraphService.getMetaGraph(),
                jobMetadata.getId(),
                branchMetadata.getId());

        taskExecutor.execute(branchCommitJob);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());
        response.put("branchId", branchMetadata.getId());
        response.put("graphId", branchCommitJob.getDstGraphId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/current-branch", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getCurrentBranch(@PathVariable String projectId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

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

    @RequestMapping(value = "/projects/{projectId}/current-branch/commit", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> commitBranch(@PathVariable String projectId) {
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
            response.put("msg", "could not find current branch");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        BranchCommitJob branchCommitJob = new BranchCommitJob(
                metaGraphService.getMetaGraph(),
                jobMetadata.getId(),
                branchMetadata.getId());

        taskExecutor.execute(branchCommitJob);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());
        response.put("graphId", branchCommitJob.getDstGraphId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/branches/{branchName}/commit", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> commitBranch(@PathVariable String projectId,
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

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        BranchCommitJob branchCommitJob = new BranchCommitJob(
                metaGraphService.getMetaGraph(),
                jobMetadata.getId(),
                branchMetadata.getId());

        taskExecutor.execute(branchCommitJob);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());
        response.put("graphId", branchCommitJob.getDstGraphId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/current-branch/commit-subset", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> commitSubsetBranch(@PathVariable String projectId,
                                                                  @Valid @RequestBody CreateGraphSubsetNSteps item,
                                                                  BindingResult result) {
        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("error", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String query = item.getQuery();
        int steps = item.getSteps();

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
            response.put("msg", "could not find current branch");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        BranchCommitSubsetJob branchCommitSubsetJob = new BranchCommitSubsetJob(
                metaGraphService.getMetaGraph(),
                jobMetadata.getId(),
                branchMetadata.getId(),
                query,
                steps);

        taskExecutor.execute(branchCommitSubsetJob);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());
        response.put("graphId", branchCommitSubsetJob.getDstGraphId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Map<String, Object> getBranchMap(BranchMetadata branchMetadata) {
        Map<String, Object> branch = new HashMap<>();

        branch.put("_id", branchMetadata.getId());
        branch.put("name", branchMetadata.getName());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date creationTime = branchMetadata.getCreationTime();
        if (creationTime != null) { branch.put("creationTime", df.format(creationTime)); }

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
