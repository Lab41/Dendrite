package org.lab41.dendrite.web.controller;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.lab41.dendrite.jobs.BranchCommitJob;
import org.lab41.dendrite.jobs.BranchCommitSubsetJob;
import org.lab41.dendrite.metagraph.CannotDeleteCurrentBranchException;
import org.lab41.dendrite.metagraph.models.*;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.services.HistoryService;
import org.lab41.dendrite.web.requests.CreateBranchRequest;
import org.lab41.dendrite.web.requests.CreateBranchSubsetNStepsRequest;
import org.lab41.dendrite.web.requests.ExportProjectSubsetRequest;
import org.lab41.dendrite.web.requests.UpdateCurrentBranchRequest;
import org.lab41.dendrite.web.responses.*;
import org.springframework.beans.factory.annotation.Autowired;
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
    public GetBranchResponse getBranch(@PathVariable String branchId) throws MetaGraphTx.NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            BranchMetadata branchMetadata = tx.getBranch(branchId);
            if (branchMetadata == null) {
                throw new MetaGraphTx.NotFound(BranchMetadata.class, branchId);
            }

            return new GetBranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#branchId, 'branchId','admin')")
    @RequestMapping(value = "/branches/{branchId}", method = RequestMethod.DELETE)
    @ResponseBody
    public DeleteBranchResponse deleteBranch(@PathVariable String branchId) throws IOException, GitAPIException, CannotDeleteCurrentBranchException, MetaGraphTx.NotFound {

        MetaGraphTx tx = metaGraphService.newTransaction();

        try {
            BranchMetadata branchMetadata = tx.getBranch(branchId);
            if (branchMetadata == null) {
                throw new MetaGraphTx.NotFound(BranchMetadata.class, branchId);
            }

            ProjectMetadata projectMetadata = branchMetadata.getProject();
            if (projectMetadata == null) {
                throw new MetaGraphTx.NotFound(ProjectMetadata.class);
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
    public GetBranchesResponse getBranches(@PathVariable String projectId) throws MetaGraphTx.NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new MetaGraphTx.NotFound(ProjectMetadata.class, projectId);
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
    public GetBranchResponse getBranch(@PathVariable String projectId, @PathVariable String branchName) throws MetaGraphTx.NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new MetaGraphTx.NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getBranchByName(branchName);
            if (branchMetadata == null) {
                throw new MetaGraphTx.NotFound(BranchMetadata.class, branchName);
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
                                       BindingResult result) throws GitAPIException, IOException, BindingException, MetaGraphTx.NotFound {

        if (result.hasErrors()) {
            throw new BindingException(result);
        }

        String graphId = item.getGraphId();

        MetaGraphTx tx = metaGraphService.newTransaction();
        BranchCommitJob branchCommitJob;
        GetBranchResponse getBranchResponse;

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new MetaGraphTx.NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata;

            if (graphId == null) {
                branchMetadata = tx.createBranch(branchName, projectMetadata);
            } else {
                GraphMetadata graphMetadata = tx.getGraph(graphId);
                if (graphMetadata == null) {
                    throw new MetaGraphTx.NotFound(GraphMetadata.class, graphId);
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
                    projectId,
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
    public GetBranchResponse getCurrentBranch(@PathVariable String projectId) throws MetaGraphTx.NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new MetaGraphTx.NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
            if (branchMetadata == null) {
                throw new MetaGraphTx.NotFound(BranchController.class);
            }

            return new GetBranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch", method = RequestMethod.PUT)
    @ResponseBody
    public SetCurrentBranchResponse setCurrentBranch(@PathVariable String projectId,
                                                     @Valid @RequestBody UpdateCurrentBranchRequest item,
                                                     BindingResult result) throws GitAPIException, IOException, BindingException, MetaGraphTx.NotFound {

        if (result.hasErrors()) {
            throw new BindingException(result);
        }

        String branchName = item.getBranchName();

        MetaGraphTx tx = metaGraphService.newTransaction();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new MetaGraphTx.NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getBranchByName(branchName);
            if (branchMetadata == null) {
                throw new MetaGraphTx.NotFound(BranchController.class, branchName);
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
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        // Commit must come after all branch access.
        tx.commit();

        return new SetCurrentBranchResponse();
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch/commit", method = RequestMethod.POST)
    @ResponseBody
    public BranchJobResponse commitBranch(@PathVariable String projectId) throws GitAPIException, IOException, MetaGraphTx.NotFound {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        MetaGraphTx tx = metaGraphService.newTransaction();
        BranchCommitJob branchCommitJob;

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new MetaGraphTx.NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
            if (branchMetadata == null) {
                throw new MetaGraphTx.NotFound(BranchMetadata.class);
            }

            JobMetadata jobMetadata = tx.createJob(projectMetadata);

            Git git = historyService.projectGitRepository(projectMetadata);
            try {
                git.commit()
                        .setAuthor(authentication.getName(), "")
                        .setMessage("commit")
                        .call();
            } finally {
                git.close();
            }

            // We can't pass the values directly because they'll live in a separate thread.
            branchCommitJob = new BranchCommitJob(
                    metaGraphService.getMetaGraph(),
                    jobMetadata.getId(),
                    projectId,
                    branchMetadata.getId());

        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        //taskExecutor.execute(branchCommitJob);
        branchCommitJob.run();

        return new BranchJobResponse(branchCommitJob);
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
    @ResponseBody
    public BranchJobResponse commitSubsetBranch(@PathVariable String projectId,
                                                   @Valid @RequestBody CreateBranchSubsetNStepsRequest item,
                                                   BindingResult result) throws BindingException, MetaGraphTx.NotFound {

        if (result.hasErrors()) {
            throw new BindingException(result);
        }

        String query = item.getQuery();
        int steps = item.getSteps();

        MetaGraphTx tx = metaGraphService.newTransaction();
        BranchCommitSubsetJob branchCommitSubsetJob;

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new MetaGraphTx.NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
            if (branchMetadata == null) {
                throw new MetaGraphTx.NotFound(BranchMetadata.class);
            }

            JobMetadata jobMetadata = tx.createJob(projectMetadata);

            // We can't pass the values directly because they'll live in a separate thread.
            branchCommitSubsetJob = new BranchCommitSubsetJob(
                    metaGraphService.getMetaGraph(),
                    projectId,
                    jobMetadata.getId(),
                    branchMetadata.getId(),
                    query,
                    steps);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        //taskExecutor.execute(branchCommitSubsetJob);
        branchCommitSubsetJob.run();

        return new BranchJobResponse(branchCommitSubsetJob);
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch/export-subset", method = RequestMethod.POST)
    @ResponseBody
    public BranchJobResponse exportSubset(@PathVariable String projectId,
                                          @Valid @RequestBody ExportProjectSubsetRequest item,
                                          Principal principal,
                                          BindingResult result) throws MetaGraphTx.NotFound, BindingException {

        if (result.hasErrors()) {
            throw new BindingException(result);
        }

        String name = item.getName();
        String query = item.getQuery();
        int steps = item.getSteps();

        MetaGraphTx tx = metaGraphService.newTransaction();
        BranchCommitSubsetJob branchCommitSubsetJob;

        try {
            UserMetadata userMetadata = tx.getOrCreateUser(principal);
            if (userMetadata == null) {
                throw new MetaGraphTx.NotFound(UserMetadata.class, principal.getName());
            }

            ProjectMetadata srcProjectMetadata = tx.getProject(projectId);
            if (srcProjectMetadata == null) {
                throw new MetaGraphTx.NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata srcBranchMetadata = srcProjectMetadata.getCurrentBranch();
            if (srcBranchMetadata == null) {
                throw new MetaGraphTx.NotFound(BranchMetadata.class);
            }

            GraphMetadata srcGraphMetadata = srcBranchMetadata.getGraph();
            if (srcGraphMetadata == null) {
                throw new MetaGraphTx.NotFound(GraphMetadata.class);
            }

            ProjectMetadata dstProjectMetadata = tx.createProject(name, userMetadata);

            BranchMetadata dstBranchMetadata = dstProjectMetadata.getCurrentBranch();
            if (dstBranchMetadata == null) {
                throw new MetaGraphTx.NotFound(BranchMetadata.class);
            }

            GraphMetadata dstGraphMetadata = dstBranchMetadata.getGraph();
            if (dstGraphMetadata == null) {
                throw new MetaGraphTx.NotFound(GraphMetadata.class);
            }

            JobMetadata jobMetadata = tx.createJob(srcProjectMetadata);

            // We can't pass the values directly because they'll live in a separate thread.
            branchCommitSubsetJob = new BranchCommitSubsetJob(
                    metaGraphService.getMetaGraph(),
                    jobMetadata.getId(),
                    dstProjectMetadata.getId(),
                    dstBranchMetadata.getId(),
                    srcGraphMetadata.getId(),
                    dstGraphMetadata.getId(),
                    query,
                    steps);

        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        tx.commit();

        //taskExecutor.execute(branchCommitSubsetJob);
        branchCommitSubsetJob.run();

        return new BranchJobResponse(branchCommitSubsetJob);
    }

}
