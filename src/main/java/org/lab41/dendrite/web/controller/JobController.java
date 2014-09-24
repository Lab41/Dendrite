package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.NotFound;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.metagraph.models.UserMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.responses.DeleteJobResponse;
import org.lab41.dendrite.web.responses.GetJobResponse;
import org.lab41.dendrite.web.responses.GetJobsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code JobController} is the user interface for creating and monitoring jobs.
 */
@Controller
@RequestMapping("/api")
public class JobController extends AbstractController {

    @Autowired
    MetaGraphService metaGraphService;

    // Note this doesn't use @PreAuthorize on purpose because it'll only show the user's projects.
    @RequestMapping(value = "/jobs", method = RequestMethod.GET)
    @ResponseBody
    public GetJobsResponse getJobs(Principal principal) {

        // This needs to be a read/write transaction as we might make a user.
        MetaGraphTx tx = metaGraphService.buildTransaction().start();
        GetJobsResponse getJobsResponse;

        try {
            UserMetadata userMetadata = tx.getOrCreateUser(principal);

            List<GetJobResponse> jobs = new ArrayList<>();

            for (ProjectMetadata projectMetadata : userMetadata.getProjects()) {
                for (JobMetadata jobMetadata : projectMetadata.getJobs()) {
                    jobs.add(new GetJobResponse(jobMetadata));
                }
            }

            getJobsResponse = new GetJobsResponse(jobs);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        // Commit must come after all graph access.
        tx.commit();

        return getJobsResponse;
    }

    @PreAuthorize("hasPermission(#jobId, 'job', 'admin')")
    @RequestMapping(value = "/jobs/{jobId}", method = RequestMethod.GET)
    @ResponseBody
    public GetJobResponse getJob(@PathVariable String jobId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            JobMetadata jobMetadata = tx.getJob(jobId);
            if (jobMetadata == null) {
                throw new NotFound(JobMetadata.class, jobId);
            }

            return new GetJobResponse(jobMetadata);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/jobs", method = RequestMethod.GET)
    @ResponseBody
    public GetJobsResponse getJobs(@PathVariable String projectId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata project = tx.getProject(projectId);
            if (project == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            List<GetJobResponse> jobs = new ArrayList<>();
            for (JobMetadata jobMetadata : project.getJobs()) {
                jobs.add(new GetJobResponse(jobMetadata));
            }

            return new GetJobsResponse(jobs);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#jobId, 'job', 'admin')")
    @RequestMapping(value = "/jobs/{jobId}", method = RequestMethod.DELETE)
    @ResponseBody
    public DeleteJobResponse deleteJob(@PathVariable String jobId) throws NotFound {
        MetaGraphTx tx = metaGraphService.newTransaction();
        DeleteJobResponse deleteJobResponse;

        try {
            JobMetadata jobMetadata = tx.getJob(jobId);
            if (jobMetadata == null) {
                throw new NotFound(JobMetadata.class, jobId);
            }

            tx.deleteJob(jobMetadata);

            deleteJobResponse = new DeleteJobResponse();
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        // Commit must come after all graph access.
        tx.commit();

        return deleteJobResponse;
    }
}
