package org.lab41.dendrite.jobs;

import com.tinkerpop.blueprints.Vertex;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGraphUpdateJob extends AbstractJob<Void> {

    static Logger logger = LoggerFactory.getLogger(AbstractGraphUpdateJob.class);

    protected DendriteGraph graph;

    public AbstractGraphUpdateJob(MetaGraph metaGraph, JobMetadata.Id jobId, DendriteGraph graph) {
        super(metaGraph, jobId);

        this.graph = graph;
    }

    @Override
    public Void call() throws Exception {
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

        return null;
    }

    protected abstract void updateGraph() throws Exception;

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
