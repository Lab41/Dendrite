package org.lab41.dendrite.services.analysis.jung;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.graph.Graph;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.services.analysis.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BetweennessCentralityService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(BetweennessCentralityService.class);

    @Async
    public void jungBetweennessCentrality(DendriteGraph graph, String jobId) {

        logger.debug("Starting analysis on "
                + graph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "jungBetweennessCentrality");
        setJobState(jobId, JobMetadata.RUNNING);

        try {
            createIndices(graph);

            TitanTransaction tx = graph.newTransaction();

            try {
                Graph<Vertex, Edge> jungGraph = new GraphJung<>(tx);
                BetweennessCentrality<Vertex, Edge> betweennessCentrality = new BetweennessCentrality<>(jungGraph);
                // Instructs the ranker that it should not remove the rank scores from
                //  the nodes (or edges) once the ranks have been computed.
                betweennessCentrality.setRemoveRankScoresOnFinalize(false);

                // Performs the iterative process. Note: this method does not return
                // anything because Java does not allow mixing double, int, or objects
                // TODO: change this to step() so that insight into the process can be
                // exposed
                betweennessCentrality.evaluate();

                for (Vertex vertex: jungGraph.getVertices()) {
                    Double score = betweennessCentrality.getVertexRankScore(vertex);
                    vertex.setProperty("jungBetweennessCentrality", score);
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

        if (tx.getType("jungBetweennessCentrality") == null) {
            tx.makeKey("jungBetweennessCentrality")
                    .dataType(Double.class)
                    .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                    .make();
        }

        tx.commit();
    }
}
