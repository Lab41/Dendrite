package org.lab41.dendrite.web.controller;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.lab41.dendrite.jobs.BranchCommitJob;
import org.lab41.dendrite.jobs.BranchCommitSubsetJob;
import org.lab41.dendrite.metagraph.CannotDeleteCurrentBranchException;
import org.lab41.dendrite.metagraph.NotFound;
import org.lab41.dendrite.metagraph.models.*;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.services.HistoryService;
import org.lab41.dendrite.web.requests.CreateBranchRequest;
import org.lab41.dendrite.web.requests.CreateBranchSubsetNStepsRequest;
import org.lab41.dendrite.web.requests.ExportProjectSubsetRequest;
import org.lab41.dendrite.web.requests.UpdateCurrentBranchRequest;
import org.lab41.dendrite.web.responses.*;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * The {@code BranchController} implements the user interface for manipulating a graph's branches.
 */
@Controller
@RequestMapping("/api")
public class BranchController extends AbstractController {

    @Autowired
    HistoryService historyService;

    /**
     * Return all branches tracked by dendrite that this user is allowed to see.
     *
     * @param principal the authentication principal.
     * @return the response.
     */
    // Note this doesn't use @PreAuthorize on purpose because it'll only show the user's projects.
    @RequestMapping(value = "/branches", method = RequestMethod.GET)
    @ResponseBody
    public GetBranchesResponse getBranches(Principal principal) {

        // This needs to be a read/write transaction as we might make a user.
        MetaGraphTx tx = metaGraphService.buildTransaction().start();
        GetBranchesResponse getBranchesResponse;

        try {
            UserMetadata userMetadata = tx.getOrCreateUser(principal);

            List<GetBranchResponse> branches = new ArrayList<>();

            for (ProjectMetadata projectMetadata : userMetadata.getProjects()) {
                for (BranchMetadata branchMetadata : projectMetadata.getBranches()) {
                    branches.add(new GetBranchResponse(branchMetadata));
                }
            }

            getBranchesResponse = new GetBranchesResponse(branches);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        return getBranchesResponse;
    }

    /**
     * Get a specific branch if this user is allowed to see this branch.
     *
     * @param branchId the id of the branch
     * @return The branch response.
     * @throws NotFound
     */
    @PreAuthorize("hasPermission(#branchId, 'branch', 'admin')")
    @RequestMapping(value = "/branches/{branchId}", method = RequestMethod.GET)
    @ResponseBody
    public GetBranchResponse getBranch(@PathVariable String branchId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            BranchMetadata branchMetadata = tx.getBranch(branchId);
            if (branchMetadata == null) {
                throw new NotFound(BranchMetadata.class, branchId);
            }

            return new GetBranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    /**
     * Delete a branch.
     *
     * @param branchId the id of the branch.
     * @return The delete branch response.
     * @throws IOException
     * @throws GitAPIException
     * @throws CannotDeleteCurrentBranchException
     * @throws NotFound
     */
    @PreAuthorize("hasPermission(#branchId, 'branch', 'admin')")
    @RequestMapping(value = "/branches/{branchId}", method = RequestMethod.DELETE)
    @ResponseBody
    public DeleteBranchResponse deleteBranch(@PathVariable String branchId) throws IOException, GitAPIException, CannotDeleteCurrentBranchException, NotFound {

        MetaGraphTx tx = metaGraphService.newTransaction();

        try {
            BranchMetadata branchMetadata = tx.getBranch(branchId);
            if (branchMetadata == null) {
                throw new NotFound(BranchMetadata.class, branchId);
            }

            ProjectMetadata projectMetadata = branchMetadata.getProject();
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class);
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

    /**
     * Get all the branches in a project.
     *
     * @param projectId the project id
     * @return the branches
     * @throws NotFound
     */
    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/branches", method = RequestMethod.GET)
    @ResponseBody
    public GetBranchesResponse getBranches(@PathVariable String projectId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
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

    /**
     * Get a branch named {@code branchName} in the project.
     *
     * @param projectId the project id
     * @param branchName the branch name
     * @return the branch
     * @throws NotFound
     */
    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/branches/{branchName}", method = RequestMethod.GET)
    @ResponseBody
    public GetBranchResponse getBranch(@PathVariable String projectId, @PathVariable String branchName) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getBranchByName(branchName);
            if (branchMetadata == null) {
                throw new NotFound(BranchMetadata.class, branchName);
            }

            return new GetBranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    /**
     * Create a branch named @{code branchName} in the project.
     *
     * @param projectId The id of the project.
     * @param branchName The name of the branch.
     * @param createBranchRequest The branch creation metadata.
     * @param result
     * @return the branch.
     * @throws GitAPIException
     * @throws IOException
     * @throws BindingException
     * @throws NotFound
     */
    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/branches/{branchName}", method = RequestMethod.PUT)
    @ResponseBody
    public GetBranchResponse createBranch(@PathVariable String projectId,
                                       @PathVariable String branchName,
                                       @Valid @RequestBody CreateBranchRequest createBranchRequest,
                                       BindingResult result) throws GitAPIException, IOException, BindingException, NotFound {

        if (result.hasErrors()) {
            throw new BindingException(result);
        }

        String graphId = createBranchRequest.getGraphId();

        MetaGraphTx tx = metaGraphService.newTransaction();
        BranchCommitJob branchCommitJob;
        GetBranchResponse getBranchResponse;

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata;

            if (graphId == null) {
                branchMetadata = tx.createBranch(branchName, projectMetadata);
            } else {
                GraphMetadata graphMetadata = tx.getGraph(graphId);
                if (graphMetadata == null) {
                    throw new NotFound(GraphMetadata.class, graphId);
                }

                branchMetadata = tx.createBranch(branchName, graphMetadata);
            }

            GraphMetadata srcGraphMetadata = branchMetadata.getGraph();
            if (srcGraphMetadata == null) {
                throw new NotFound(GraphMetadata.class);
            }

            GraphMetadata dstGraphMetadata = tx.createGraph(srcGraphMetadata);

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
                    projectMetadata.getId(),
                    branchMetadata.getId(),
                    srcGraphMetadata.getId(),
                    dstGraphMetadata.getId());

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

    /**
     * Get the current branch of a project.
     *
     * @param projectId the project id
     * @return the branch
     * @throws NotFound
     */
    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch", method = RequestMethod.GET)
    @ResponseBody
    public GetBranchResponse getCurrentBranch(@PathVariable String projectId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
            if (branchMetadata == null) {
                throw new NotFound(BranchController.class);
            }

            return new GetBranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    /**
     * Set the current project's branch.
     *
     * @param projectId the project id
     * @param item
     * @param result
     * @return
     * @throws GitAPIException
     * @throws IOException
     * @throws BindingException
     * @throws NotFound
     */
    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch", method = RequestMethod.PUT)
    @ResponseBody
    public SetCurrentBranchResponse setCurrentBranch(@PathVariable String projectId,
                                                     @Valid @RequestBody UpdateCurrentBranchRequest item,
                                                     BindingResult result) throws GitAPIException, IOException, BindingException, NotFound {

        if (result.hasErrors()) {
            throw new BindingException(result);
        }

        String branchName = item.getBranchName();

        MetaGraphTx tx = metaGraphService.newTransaction();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getBranchByName(branchName);
            if (branchMetadata == null) {
                throw new NotFound(BranchController.class, branchName);
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

    /**
     * Commit the current branch.
     * @param projectId
     * @return
     * @throws GitAPIException
     * @throws IOException
     * @throws NotFound
     */
    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch/commit", method = RequestMethod.POST)
    @ResponseBody
    public BranchJobResponse commitBranch(@PathVariable String projectId) throws GitAPIException, IOException, NotFound {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        MetaGraphTx tx = metaGraphService.newTransaction();
        BranchCommitJob branchCommitJob;

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
            if (branchMetadata == null) {
                throw new NotFound(BranchMetadata.class);
            }

            GraphMetadata srcGraphMetadata = branchMetadata.getGraph();
            if (srcGraphMetadata == null) {
                throw new NotFound(GraphMetadata.class);
            }

            GraphMetadata dstGraphMetadata = tx.createGraph(srcGraphMetadata);

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
                    projectMetadata.getId(),
                    branchMetadata.getId(),
                    srcGraphMetadata.getId(),
                    dstGraphMetadata.getId());

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

    /**
     * Commit a subset of the graph.
     * @param projectId
     * @param item
     * @param result
     * @return
     * @throws BindingException
     * @throws NotFound
     */
    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch/commit-subset", method = RequestMethod.POST)
    @ResponseBody
    public BranchJobResponse commitSubsetBranch(@PathVariable String projectId,
                                                   @Valid @RequestBody CreateBranchSubsetNStepsRequest item,
                                                   BindingResult result) throws BindingException, NotFound {

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
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
            if (branchMetadata == null) {
                throw new NotFound(BranchMetadata.class);
            }

            GraphMetadata srcGraphMetadata = branchMetadata.getGraph();
            if (srcGraphMetadata == null) {
                throw new NotFound(GraphMetadata.class);
            }

            GraphMetadata dstGraphMetadata = tx.createGraph(srcGraphMetadata);

            JobMetadata jobMetadata = tx.createJob(projectMetadata);

            // We can't pass the values directly because they'll live in a separate thread.
            branchCommitSubsetJob = new BranchCommitSubsetJob(
                    metaGraphService.getMetaGraph(),
                    jobMetadata.getId(),
                    projectMetadata.getId(),
                    branchMetadata.getId(),
                    srcGraphMetadata.getId(),
                    dstGraphMetadata.getId(),
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

    /**
     * Export a subset of the project into a new project.
     * @param projectId
     * @param item
     * @param principal
     * @param result
     * @return
     * @throws NotFound
     * @throws BindingException
     */
    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch/export-subset", method = RequestMethod.POST)
    @ResponseBody
    public BranchJobResponse exportSubset(@PathVariable String projectId,
                                          @Valid @RequestBody ExportProjectSubsetRequest item,
                                          Principal principal,
                                          BindingResult result) throws NotFound, BindingException {

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
                throw new NotFound(UserMetadata.class, principal.getName());
            }

            ProjectMetadata srcProjectMetadata = tx.getProject(projectId);
            if (srcProjectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            BranchMetadata srcBranchMetadata = srcProjectMetadata.getCurrentBranch();
            if (srcBranchMetadata == null) {
                throw new NotFound(BranchMetadata.class);
            }

            GraphMetadata srcGraphMetadata = srcBranchMetadata.getGraph();
            if (srcGraphMetadata == null) {
                throw new NotFound(GraphMetadata.class);
            }

            ProjectMetadata dstProjectMetadata = tx.createProject(name, userMetadata);

            BranchMetadata dstBranchMetadata = dstProjectMetadata.getCurrentBranch();
            if (dstBranchMetadata == null) {
                throw new NotFound(BranchMetadata.class);
            }

            GraphMetadata dstGraphMetadata = dstBranchMetadata.getGraph();
            if (dstGraphMetadata == null) {
                throw new NotFound(GraphMetadata.class);
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
