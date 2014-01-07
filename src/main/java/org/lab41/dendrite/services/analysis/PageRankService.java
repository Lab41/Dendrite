package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.Hypergraph;
import org.lab41.dendrite.graph.DendriteGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PageRankService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(PageRankService.class);

    @Async
    public void jungPageRank(DendriteGraph graph, String jobId, double alpha) {

        logger.debug("Starting Titan degree counting analysis on "
                + graph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "jung-pagerank");
        setJobState(jobId, JobMetadata.RUNNING);

        createIndices(graph);

        TitanTransaction tx = graph.newTransaction();

        Hypergraph<Vertex, Edge> jungGraph = new GraphJung<>(tx);
        PageRank<Vertex, Edge> pageRank = new PageRank<>(jungGraph, alpha);
        pageRank.evaluate();

        for (Vertex vertex: jungGraph.getVertices()) {
            double score = pageRank.getVertexScore(vertex);
            vertex.setProperty("pagerank", score);
        }
        tx.commit();

        setJobState(jobId, JobMetadata.DONE);

        logger.debug("titanCountDegrees: finished job: " + jobId);
    }

    private void createIndices(DendriteGraph graph) {
        TitanTransaction tx = graph.newTransaction();

        if (tx.getType("pagerank") == null) {
            tx.makeKey("pagerank")
                    .dataType(Double.class)
                    .indexed(Vertex.class)
                    .make();
        }

        tx.commit();
    }
}
