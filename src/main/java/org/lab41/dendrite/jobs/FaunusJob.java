package org.lab41.dendrite.jobs;

import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.mapreduce.FaunusCompiler;
import com.thinkaurelius.faunus.mapreduce.FaunusJobControl;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.JobStatus;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.JobMetadata;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FaunusJob extends AbstractJob implements Callable<Object> {

    private enum State { ACTIVE, DONE, ERROR }

    private Map<JobID, JobMetadata.Id> jobMap = new HashMap<>();
    private Set<JobID> doneJobs = new HashSet<>();
    private FaunusPipeline faunusPipeline;
    private Lock lock = new ReentrantLock();
    private Condition finished = lock.newCondition();
    private State state = State.ACTIVE;

    public FaunusJob(MetaGraph metaGraph, JobMetadata.Id jobId, FaunusPipeline faunusPipeline) {
        super(metaGraph, jobId);

        this.faunusPipeline = faunusPipeline;
    }

    @Override
    public Object call() throws Exception {
        FaunusCompiler compiler = faunusPipeline.getCompiler();
        FaunusJobControl jobControl = new FaunusJobControl(faunusPipeline.getGraph(), compiler.getJobs());

        Thread thread = new Thread(jobControl);
        thread.start();

        logger.debug("Submitted job");

        try {
            while (!jobControl.allFinished()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ignore
                }

                checkJobControl(jobControl);
            }

            checkJobControl(jobControl);
        } catch (Exception e) {
            setJobState(jobId, JobMetadata.ERROR, e.toString());
            throw e;
        }

        return null;
    }

    private void checkJobControl(FaunusJobControl jobControl) throws Exception {
        logger.debug("checking jobs");

        List<Job> jobsInProgress = jobControl.getJobsInProgress();
        List<Job> successfulJobs = jobControl.getSuccessfulJobs();
        List<Job> failedJobs = jobControl.getFailedJobs();

        for (Job hadoopJob: successfulJobs) {
            JobID hadoopJobId = hadoopJob.getJobID();
            logger.debug("found successful hadoop job:", hadoopJobId.toString());

            if (!doneJobs.contains(hadoopJobId)) {
                doneJobs.add(hadoopJobId);

                if (jobMap.containsKey(hadoopJobId)) {
                    setJobState(jobMap.get(hadoopJobId), JobMetadata.DONE);
                } else {
                    MetaGraphTx tx = metaGraph.newTransaction();
                    JobMetadata jobMetadata = tx.getJob(jobId);
                    JobMetadata childJobMetadata = tx.createJob(jobMetadata);
                    childJobMetadata.setName("faunusHadoopJob");
                    childJobMetadata.setState(JobMetadata.DONE);
                    childJobMetadata.setProgress(1.0f);
                    childJobMetadata.setMapreduceJobId(hadoopJobId.toString());
                    tx.commit();

                    jobMap.put(hadoopJobId, childJobMetadata.getId());
                }
            }
        }

        for (Job hadoopJob: failedJobs) {
            JobID hadoopJobId = hadoopJob.getJobID();
            logger.debug("found failed hadoop job:", hadoopJobId.toString());

            if (jobMap.containsKey(hadoopJobId)) {
                setJobState(jobMap.get(hadoopJobId), JobMetadata.ERROR);
            } else {
                MetaGraphTx tx = metaGraph.newTransaction();
                JobMetadata jobMetadata = tx.getJob(jobId);
                JobMetadata childJobMetadata = tx.createJob(jobMetadata);
                childJobMetadata.setName("faunusHadoopJob");
                childJobMetadata.setMapreduceJobId(hadoopJobId.toString());
                childJobMetadata.setState(JobMetadata.ERROR);
                tx.commit();

                jobMap.put(hadoopJobId, childJobMetadata.getId());
            }
        }

        float totalProgress;

        // Don't divide by zero if we don't have any jobs in progress.
        if (jobsInProgress.isEmpty()) {
            totalProgress = 1;
        } else {
            totalProgress = ((float) successfulJobs.size()) / ((float) jobsInProgress.size());
        }

        Job hadoopRunningJob = jobControl.getRunningJob();
        if (hadoopRunningJob != null) {
            JobID hadoopJobId = hadoopRunningJob.getJobID();
            logger.debug("found active hadoop job:", hadoopJobId.toString());

            JobStatus status = hadoopRunningJob.getStatus();
            float progress = 0.25f * (
                    status.getSetupProgress() +
                            status.getMapProgress() +
                            status.getReduceProgress() +
                            status.getCleanupProgress());

            logger.debug("found progress: "
                    + status.getSetupProgress() + " "
                    + status.getMapProgress() + " "
                    + status.getReduceProgress() + " "
                    + status.getCleanupProgress() + " "
                    + progress);

            if (jobMap.containsKey(hadoopJobId)) {
                setJobProgress(jobMap.get(hadoopJobId), progress);
            } else {
                MetaGraphTx tx = metaGraph.newTransaction();
                JobMetadata jobMetadata = tx.getJob(jobId);
                JobMetadata childJobMetadata = tx.createJob(jobMetadata);
                childJobMetadata.setName("faunusHadoopJob");
                childJobMetadata.setProgress(progress);
                childJobMetadata.setState(JobMetadata.RUNNING);
                childJobMetadata.setMapreduceJobId(hadoopJobId.toString());
                tx.commit();

                jobMap.put(hadoopJobId, childJobMetadata.getId());
            }

            JobMetadata.Id jobMetadataId = jobMap.get(hadoopJobId);
            setJobProgress(jobMetadataId, progress);
            totalProgress += (progress / ((float) jobsInProgress.size()));

            if (!failedJobs.isEmpty()) {
                setJobState(jobMetadataId, JobMetadata.ERROR);
            }
        }

        setJobProgress(jobId, totalProgress);

        if (!failedJobs.isEmpty()) {
            throw new Exception("hadoop job failed");
        }
    }
}
