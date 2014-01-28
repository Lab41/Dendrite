package org.lab41.dendrite.services.analysis.jung;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.Hypergraph;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.services.analysis.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PageRankService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(PageRankService.class);

    @Async
    public void jungPageRank(DendriteGraph graph, String jobId, double alpha) {

        logger.debug("Starting analysis on "
                + graph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "jungPageRank");
        setJobState(jobId, JobMetadata.RUNNING);

        try {
            createIndices(graph);

            TitanTransaction tx = graph.newTransaction();
            try {
                Hypergraph<Vertex, Edge> jungGraph = new GraphJung<>(tx);
                PageRank<Vertex, Edge> pageRank = new PageRank<>(jungGraph, alpha);
                pageRank.evaluate();

                for (Vertex vertex: jungGraph.getVertices()) {
                    Double score = pageRank.getVertexScore(vertex);
                    vertex.setProperty("jungPageRank", score);
                }
            } catch (Throwable t) {
                tx.rollback();
                throw t;
            }

            tx.commit();
        } catch (Throwable t) {
            logger.error("failed", t);
            setJobState(jobId, JobMetadata.ERROR, t.getMessage());
            throw t;
        }

        setJobState(jobId, JobMetadata.DONE);

        logger.debug("finished job: " + jobId);
    }

    private void createIndices(DendriteGraph graph) {
        TitanTransaction tx = graph.newTransaction();

        if (tx.getType("jungPageRank") == null) {
            tx.makeKey("jungPageRank")
                    .dataType(Double.class)
                    .indexed("search", Vertex.class)
                    .make();
        }

        tx.commit();
    }
}
