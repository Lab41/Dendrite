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
    protected String jobId;

    public AbstractJob(MetaGraph metaGraph, String jobId) {
        this.metaGraph = metaGraph;
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    protected void setJobName(String jobId, String name) {
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

    protected void setJobState(String jobId, String state) {
        setJobState(jobId, state, null);
    }

    protected void setJobState(String jobId, String state, String msg) {
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

    protected void setJobProgress(String jobId, float progress) {
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
