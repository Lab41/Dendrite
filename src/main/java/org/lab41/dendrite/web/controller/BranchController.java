package org.lab41.dendrite.web.controller;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.lab41.dendrite.jobs.BranchCommitJob;
import org.lab41.dendrite.jobs.BranchCommitSubsetJob;
import org.lab41.dendrite.metagraph.CannotDeleteCurrentBranchException;
import org.lab41.dendrite.metagraph.models.*;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.services.HistoryService;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.requests.CreateBranchRequest;
import org.lab41.dendrite.web.requests.CreateBranchSubsetNStepsRequest;
import org.lab41.dendrite.web.requests.ExportProjectSubsetRequest;
import org.lab41.dendrite.web.requests.UpdateCurrentBranchRequest;
import org.lab41.dendrite.web.responses.DeleteBranchResponse;
import org.lab41.dendrite.web.responses.GetBranchResponse;
import org.lab41.dendrite.web.responses.GetBranchesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/api")
public class BranchController extends AbstractController {

    @Autowired
    HistoryService historyService;

    @RequestMapping(value = "/branches", method = RequestMethod.GET)
    @ResponseBody
    public GetBranchesResponse getBranches() {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            List<GetBranchResponse> branches = new ArrayList<>();

            for (BranchMetadata branchMetadata : tx.getBranches()) {
                branches.add(new GetBranchResponse(branchMetadata));
            }

            return new GetBranchesResponse(branches);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/branches/{branchId}", method = RequestMethod.GET)
    @ResponseBody
    public GetBranchResponse getBranch(@PathVariable String branchId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            BranchMetadata branchMetadata = tx.getBranch(branchId);
            if (branchMetadata == null) {
                throw new NotFound("branch", branchId);
            }

            return new GetBranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#branchId, 'branchId','admin')")
    @RequestMapping(value = "/branches/{branchId}", method = RequestMethod.DELETE)
    @ResponseBody
    public DeleteBranchResponse deleteBranch(@PathVariable String branchId) throws IOException, GitAPIException, CannotDeleteCurrentBranchException, NotFound {

        MetaGraphTx tx = metaGraphService.newTransaction();

        try {
            BranchMetadata branchMetadata = tx.getBranch(branchId);
            if (branchMetadata == null) {
                throw new NotFound("branch", branchId);
            }

            ProjectMetadata projectMetadata = branchMetadata.getProject();
            if (projectMetadata == null) {
                throw new NotFound("project");
            }

            Git git = historyService.projectGitRepository(projectMetadata);

            String branchName = branchMetadata.getName();

            tx.deleteBranch(branchMetadata);

            try {
                git.branchDelete()
                        .setBranchNames(branchName)
                        .call();
            } finally {
                git.close();
            }

        } catch (Throwable e) {
            tx.rollback();
            throw e;
        }

        tx.commit();

        return new DeleteBranchResponse();
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/branches", method = RequestMethod.GET)
    @ResponseBody
    public GetBranchesResponse getBranches(@PathVariable String projectId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound("project", projectId);
            }

            List<GetBranchResponse> branches = new ArrayList<>();
            for (BranchMetadata branchMetadata: projectMetadata.getBranches()) {
                branches.add(new GetBranchResponse(branchMetadata));
            }

            return new GetBranchesResponse(branches);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/branches/{branchName}", method = RequestMethod.GET)
    @ResponseBody
    public GetBranchResponse getBranch(@PathVariable String projectId, @PathVariable String branchName) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound("project", projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getBranchByName(branchName);
            if (branchMetadata == null) {
                throw new NotFound("branch", branchName);
            }

            return new GetBranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/branches/{branchName}", method = RequestMethod.PUT)
    @ResponseBody
    public GetBranchResponse createBranch(@PathVariable String projectId,
                                       @PathVariable String branchName,
                                       @Valid @RequestBody CreateBranchRequest item,
                                       BindingResult result) throws GitAPIException, IOException, BindingException, NotFound {

        if (result.hasErrors()) {
            throw new BindingException(result.toString());
        }

        MetaGraphTx tx = metaGraphService.newTransaction();
        BranchCommitJob branchCommitJob;
        GetBranchResponse getBranchResponse;

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound("project", projectId);
            }

            String graphId = item.getGraphId();
            BranchMetadata branchMetadata;

            if (graphId == null) {
                branchMetadata = tx.createBranch(branchName, projectMetadata);
            } else {
                GraphMetadata graphMetadata = tx.getGraph(graphId);
                if (graphMetadata == null) {
                    throw new NotFound("graph", graphId);
                }

                branchMetadata = tx.createBranch(branchName, graphMetadata);
            }

            JobMetadata jobMetadata = tx.createJob(projectMetadata);

            Git git = historyService.projectGitRepository(projectMetadata);
            try {
                git.branchCreate()
                        .setName(branchName)
                        .call();
            } finally {
                git.close();
            }

            // We can't pass the values directly because they'll live in a separate thread.
            branchCommitJob = new BranchCommitJob(
                    metaGraphService.getMetaGraph(),
                    jobMetadata.getId(),
                    branchMetadata.getId());

            getBranchResponse = new GetBranchResponse(branchMetadata, jobMetadata);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        // Commit must come after all branch access.
        tx.commit();

        //taskExecutor.execute(branchCommitJob);
        branchCommitJob.run();

        return getBranchResponse;
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch", method = RequestMethod.GET)
    @ResponseBody
    public GetBranchResponse getCurrentBranch(@PathVariable String projectId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound("project", projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
            if (branchMetadata == null) {
                throw new NotFound("branch");
            }

            return new GetBranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setCurrentBranch(@PathVariable String projectId,
                                                                @Valid @RequestBody UpdateCurrentBranchRequest item,
                                                                BindingResult result) throws GitAPIException, IOException, BindingException {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            throw new BindingException(result.toString());
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

        Git git = historyService.projectGitRepository(projectMetadata);
        try {
            git.checkout()
                    .setName(branchName)
                    .call();
        } finally {
            git.close();
        }

        response.put("msg", "current branch changed");

        // Commit must come after all branch access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch/commit", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> commitBranch(@PathVariable String projectId) throws GitAPIException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

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

        try {
            Git git = historyService.projectGitRepository(projectMetadata);
            try {
                git.commit()
                        .setAuthor(authentication.getName(), "")
                        .setMessage("commit")
                        .call();
            } finally {
                git.close();
            }

        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        BranchCommitJob branchCommitJob = new BranchCommitJob(
                metaGraphService.getMetaGraph(),
                jobMetadata.getId(),
                branchMetadata.getId());

        //taskExecutor.execute(branchCommitJob);
        branchCommitJob.run();

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());
        response.put("graphId", branchCommitJob.getDstGraphId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /*
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
    */

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch/commit-subset", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> commitSubsetBranch(@PathVariable String projectId,
                                                                  @Valid @RequestBody CreateBranchSubsetNStepsRequest item,
                                                                  BindingResult result) throws BindingException {
        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            throw new BindingException(result.toString());
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

        //taskExecutor.execute(branchCommitSubsetJob);
        branchCommitSubsetJob.run();

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());
        response.put("graphId", branchCommitSubsetJob.getDstGraphId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch/export-subset", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> exportSubset(@PathVariable String projectId,
                                                            @Valid @RequestBody ExportProjectSubsetRequest item,
                                                            Principal principal,
                                                            BindingResult result) throws NotFound {
        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("error", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String name = item.getName();
        String query = item.getQuery();
        int steps = item.getSteps();

        MetaGraphTx tx = metaGraphService.newTransaction();

        String srcGraphId;
        String dstProjectId;
        String dstBranchId;
        String dstGraphId;
        String jobId;

        try {
            UserMetadata userMetadata = tx.getUserByName(principal.getName());
            if (userMetadata == null) {
                throw new NotFound("user", principal.getName());
            }

            ProjectMetadata srcProjectMetadata = tx.getProject(projectId);
            if (srcProjectMetadata == null) {
                throw new NotFound("project", projectId);
            }

            BranchMetadata srcBranchMetadata = srcProjectMetadata.getCurrentBranch();
            if (srcBranchMetadata == null) {
                throw new NotFound("branch");
            }

            GraphMetadata srcGraphMetadata = srcBranchMetadata.getGraph();
            if (srcGraphMetadata == null) {
                throw new NotFound("graph");
            }
            srcGraphId = srcGraphMetadata.getId();

            ProjectMetadata dstProjectMetadata = tx.createProject(name, userMetadata);
            dstProjectId = dstProjectMetadata.getId();

            BranchMetadata dstBranchMetadata = dstProjectMetadata.getCurrentBranch();
            dstBranchId = dstBranchMetadata.getId();

            GraphMetadata dstGraphMetadata = dstBranchMetadata.getGraph();
            dstGraphId = dstGraphMetadata.getId();

            JobMetadata jobMetadata = tx.createJob(srcProjectMetadata);
            jobId = jobMetadata.getId();

        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        BranchCommitSubsetJob branchCommitSubsetJob = new BranchCommitSubsetJob(
                metaGraphService.getMetaGraph(),
                jobId,
                dstBranchId,
                srcGraphId,
                dstGraphId,
                query,
                steps);

        //taskExecutor.execute(branchCommitSubsetJob);
        branchCommitSubsetJob.run();

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobId);
        response.put("graphId", dstGraphId);
        response.put("projectId", dstProjectId);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
