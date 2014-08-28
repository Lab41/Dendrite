package org.lab41.dendrite.jobs;

import com.thinkaurelius.titan.core.TitanException;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJob {

    static Logger logger = LoggerFactory.getLogger(AbstractJob.class);

    protected MetaGraph metaGraph;
    protected JobMetadata.Id jobId;

    public AbstractJob(MetaGraph metaGraph, JobMetadata.Id jobId) {
        this.metaGraph = metaGraph;
        this.jobId = jobId;
    }

    public JobMetadata.Id getJobId() {
        return jobId;
    }

    protected void setJobName(JobMetadata.Id jobId, String name) {
        MetaGraphTx tx = metaGraph.newTransaction();
        try {
            JobMetadata jobMetadata = tx.getJob(jobId);
            jobMetadata.setName(name);
            tx.commit();
        } catch (TitanException e) {
            logger.debug("exception", e);
            throw e;
        }
    }

    protected void setJobState(JobMetadata.Id jobId, String state) {
        setJobState(jobId, state, null);
    }

    protected void setJobState(JobMetadata.Id jobId, String state, String msg) {
        MetaGraphTx tx = metaGraph.newTransaction();
        try {
            JobMetadata jobMetadata = tx.getJob(jobId);
            jobMetadata.setState(state);
            jobMetadata.setMessage(msg);

            if (state.equals(JobMetadata.DONE)) {
                jobMetadata.setProgress(1.0f);
            }

            tx.commit();
        } catch (TitanException e) {
            logger.debug("exception", e);
            throw e;
        }
    }

    protected void setJobProgress(JobMetadata.Id jobId, float progress) {
        MetaGraphTx tx = metaGraph.newTransaction();
        try {
            JobMetadata jobMetadata = tx.getJob(jobId);
            jobMetadata.setProgress(progress);
            tx.commit();
        } catch (TitanException e) {
            logger.debug("exception", e);
            throw e;
        }
    }

}
