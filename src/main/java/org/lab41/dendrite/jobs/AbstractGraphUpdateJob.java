package org.lab41.dendrite.jobs;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph;
import org.lab41.dendrite.jobs.AbstractJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.DendriteGraphBatchWrapper;
import org.lab41.dendrite.metagraph.DendriteGraphTx;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGraphUpdateJob extends AbstractJob {

    static Logger logger = LoggerFactory.getLogger(AbstractGraphUpdateJob.class);

    protected DendriteGraph graph;

    public AbstractGraphUpdateJob(MetaGraph metaGraph, String jobId, DendriteGraph graph) {
        super(metaGraph, jobId);

        this.graph = graph;
    }

    public void run() {
        logger.debug("Starting " + getClass().getSimpleName() + " analysis on "
                + graph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, getClass().getSimpleName());
        setJobState(jobId, JobMetadata.RUNNING);

        try {
            try {
                createIndices();
                graph.commit();
            } catch (Throwable t) {
                graph.rollback();
                throw t;
            }

            try {
                updateGraph();
                graph.commit();
            } catch (Throwable t) {
                graph.rollback();
                throw t;
            }

            setJobState(jobId, JobMetadata.DONE);
        } catch (Throwable t) {
            setJobState(jobId, JobMetadata.ERROR, t.getMessage());
            throw t;
        }

        logger.debug("Finished analysis on " + jobId);
    }

    protected abstract void updateGraph();

    protected void createIndices() { }

    protected void createVertexIndex(String key, Class cls) {
        if (graph.getType(key) == null) {
            graph.makeKey(key)
                    .dataType(cls)
                    .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                    .make();
        }
    }
}
