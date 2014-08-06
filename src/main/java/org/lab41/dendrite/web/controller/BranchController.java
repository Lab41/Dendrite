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
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/api")
public class BranchController {

    @Autowired
    MetaGraphService metaGraphService;

    @Autowired
    TaskExecutor taskExecutor;

    @Autowired
    HistoryService historyService;

    @RequestMapping(value = "/branches", method = RequestMethod.GET)
    @ResponseBody
    public BranchesResponse getBranches() {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            List<BranchResponse> branches = new ArrayList<>();

            for (BranchMetadata branchMetadata : tx.getBranches()) {
                branches.add(new BranchResponse(branchMetadata));
            }

            return new BranchesResponse(branches);
        } finally {
            tx.commit();
        }
    }

    class BranchesResponse {
        List<BranchResponse> branches;

        public BranchesResponse(List<BranchResponse> branches) {
            this.branches = branches;
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/branches/{branchId}", method = RequestMethod.GET)
    @ResponseBody
    public BranchResponse getBranch(@PathVariable String branchId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            BranchMetadata branchMetadata = tx.getBranch(branchId);
            if (branchMetadata == null) {
                throw new NotFound("branch", branchId);
            }

            return new BranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    class BranchResponse {
        public String _id;
        public String name;
        public String creationTime;
        public String projectId;
        public String graphId;
        public String jobId;

        public BranchResponse(BranchMetadata branchMetadata) {
            this(branchMetadata, null);
        }

        public BranchResponse(BranchMetadata branchMetadata, JobMetadata jobMetadata) {
            this._id = branchMetadata.getId();
            this.name = branchMetadata.getName();

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date creationTime = branchMetadata.getCreationTime();
            if (creationTime != null) {
                this.creationTime = df.format(creationTime);
            }

            ProjectMetadata projectMetadata = branchMetadata.getProject();
            if (projectMetadata != null) {
                this.projectId = branchMetadata.getProject().getId();
            }

            GraphMetadata graphMetadata = branchMetadata.getGraph();
            if (graphMetadata != null) {
                this.graphId = branchMetadata.getGraph().getId();
            }

            if (jobMetadata != null) {
                this.jobId = jobMetadata.getId();
            }
        }
    }

    @PreAuthorize("hasPermission(#branchId, 'branchId','admin')")
    @RequestMapping(value = "/branches/{branchId}", method = RequestMethod.DELETE)
    @ResponseBody
    public BranchDeletedResponse deleteBranch(@PathVariable String branchId) throws IOException, GitAPIException, CannotDeleteCurrentBranchException, NotFound {

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

        return new BranchDeletedResponse();
    }

    class BranchDeletedResponse {
        String msg;

        public BranchDeletedResponse() {
            this.msg = "deleted";
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/branches", method = RequestMethod.GET)
    @ResponseBody
    public BranchesResponse getBranches(@PathVariable String projectId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound("project", projectId);
            }

            List<BranchResponse> branches = new ArrayList<>();
            for (BranchMetadata branchMetadata: projectMetadata.getBranches()) {
                branches.add(new BranchResponse(branchMetadata));
            }

            return new BranchesResponse(branches);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/branches/{branchName}", method = RequestMethod.GET)
    @ResponseBody
    public BranchResponse getBranch(@PathVariable String projectId, @PathVariable String branchName) throws NotFound {

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

            return new BranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/branches/{branchName}", method = RequestMethod.PUT)
    @ResponseBody
    public BranchResponse createBranch(@PathVariable String projectId,
                                       @PathVariable String branchName,
                                       @Valid @RequestBody CreateBranchRequest item,
                                       BindingResult result) throws GitAPIException, IOException, BindingException, NotFound {

        if (result.hasErrors()) {
            throw new BindingException(result.toString());
        }

        MetaGraphTx tx = metaGraphService.newTransaction();
        BranchCommitJob branchCommitJob;
        BranchResponse branchResponse;

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

            branchResponse = new BranchResponse(branchMetadata, jobMetadata);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        // Commit must come after all branch access.
        tx.commit();

        //taskExecutor.execute(branchCommitJob);
        branchCommitJob.run();

        return branchResponse;
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch", method = RequestMethod.GET)
    @ResponseBody
    public BranchResponse getCurrentBranch(@PathVariable String projectId) throws NotFound {

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

            return new BranchResponse(branchMetadata);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project','admin')")
    @RequestMapping(value = "/projects/{projectId}/current-branch", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setCurrentBranch(@PathVariable String projectId,
                                                                @Valid @RequestBody UpdateCurrentBranchRequest item,
                                                                BindingResult result) throws GitAPIException, IOException {

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

    class SetCurrentBranchResponse {
        String msg;

        SetCurrentBranchResponse() {
            this.msg = "current branch changed";
        }
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
                                                                  BindingResult result) {
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
                                                            BindingResult result) {
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

    class NotFound extends Exception {
        private String type;
        private String id;

        public NotFound(String type) {
            super("could not find " + type);
            this.type = type;
            this.id = null;
        }

        public NotFound(String type, String id) {
            super("could not find " + type + " '" + id + "'");
            this.type = type;
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }
    }

    class BindingException extends Exception {
        public BindingException(String msg) {
            super(msg);
        }
    }

    class ErrorInfo {
        private String status;
        private String msg;

        public ErrorInfo(String msg) {
            this.status = "error";
            this.msg = msg;
        }

        public String getStatus() {
            return status;
        }

        public String getMsg() {
            return msg;
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFound.class)
    public ErrorInfo handleNotFound(NotFound notFound) {
        return new ErrorInfo(notFound.getLocalizedMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindingException.class)
    public ErrorInfo handleBindingException(BindingException bindingException) {
        return new ErrorInfo(bindingException.getLocalizedMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(CannotDeleteCurrentBranchException.class)
    public ErrorInfo handleCannotDeleteCurrentBranch() {
        return new ErrorInfo("cannot delete current branch");
    }
}
