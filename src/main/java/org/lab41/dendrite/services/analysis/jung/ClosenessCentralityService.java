package org.lab41.dendrite.services.analysis.jung;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.graph.Graph;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.services.analysis.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ClosenessCentralityService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(ClosenessCentralityService.class);

    @Async
    public void jungClosenessCentrality(DendriteGraph graph, String jobId) {

        logger.debug("Starting analysis on "
                + graph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "jungClosenessCentrality");
        setJobState(jobId, JobMetadata.RUNNING);

        try {
            createIndices(graph);

            TitanTransaction tx = graph.newTransaction();

            try {
                Graph<Vertex, Edge> jungGraph = new GraphJung<>(tx);
                ClosenessCentrality<Vertex, Edge> closenessCentrality = new ClosenessCentrality<>(jungGraph);

                for (Vertex vertex: jungGraph.getVertices()) {
                    Double score = closenessCentrality.getVertexScore(vertex);
                    vertex.setProperty("jungClosenessCentrality", score);
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

        logger.debug("Finished analysis job: " + jobId);
    }

    private void createIndices(DendriteGraph graph) {
        TitanTransaction tx = graph.newTransaction();

        if (tx.getType("jungClosenessCentrality") == null) {
            tx.makeKey("jungClosenessCentrality")
                    .dataType(FullDouble.class)
                    .indexed("search", Vertex.class)
                    .make();
        }

        tx.commit();
    }
}
