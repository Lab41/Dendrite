package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import org.lab41.dendrite.graph.DendriteGraph;
import org.lab41.dendrite.models.JobMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class BetweennessCentralityService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(BetweennessCentralityService.class);

    @Async
    public void jungBetweennessCentrality(DendriteGraph graph, String jobId) {

        logger.debug("Starting Titan betweenness centrality analysis on "
                + graph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "jung-betweennesscentrality");
        setJobState(jobId, JobMetadata.RUNNING);

        createIndices(graph);

        TitanTransaction tx = graph.newTransaction();

        Graph<Vertex, Edge> jungGraph = new GraphJung<>(tx);
        BetweennessCentrality<Vertex, Edge> betweennessCentrality = new BetweennessCentrality<>(jungGraph);
        betweennessCentrality.setRemoveRankScoresOnFinalize(false);
        betweennessCentrality.evaluate();

        for (Vertex vertex: jungGraph.getVertices()) {
            double score = betweennessCentrality.getVertexRankScore(vertex);
            vertex.setProperty("betweennesscentrality", score);
        }
        tx.commit();

        setJobState(jobId, JobMetadata.DONE);

        logger.debug("titanBetweenessCentrality: finished job: " + jobId);
    }

    private void createIndices(DendriteGraph graph) {
        TitanTransaction tx = graph.newTransaction();

        if (tx.getType("betweennesscentrality") == null) {
            tx.makeKey("betweennesscentrality")
                    .dataType(Double.class)
                    .indexed(Vertex.class)
                    .make();
        }

        tx.commit();
    }
}
