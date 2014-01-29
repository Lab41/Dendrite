package org.lab41.dendrite.services.analysis.jung;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.graph.Graph;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.services.analysis.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EigenvectorCentralityService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(EigenvectorCentralityService.class);

    @Async
    public void jungEigenvectorCentrality(DendriteGraph graph, String jobId) {

        logger.debug("Starting analysis on "
                + graph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "jungEigenvectorCentrality");
        setJobState(jobId, JobMetadata.RUNNING);

        try {
            createIndices(graph);

            TitanTransaction tx = graph.newTransaction();

            try {
                Graph<Vertex, Edge> jungGraph = new GraphJung<>(tx);
                EigenvectorCentrality<Vertex, Edge> eigenvectorCentrality = new EigenvectorCentrality<>(jungGraph);
                eigenvectorCentrality.acceptDisconnectedGraph(true);
                eigenvectorCentrality.evaluate();

                for (Vertex vertex: jungGraph.getVertices()) {
                    Double score = eigenvectorCentrality.getVertexScore(vertex);
                    vertex.setProperty("jungEigenvectorCentrality", score);
                }
            } catch (Throwable t) {
                tx.rollback();
                throw t;
            }

            tx.commit();
        } catch (Throwable t) {
            logger.debug("Error:", t);
            setJobState(jobId, JobMetadata.ERROR, t.getMessage());
            throw t;
        }

        setJobState(jobId, JobMetadata.DONE);

        logger.debug("Finished analysis job: " + jobId);
    }

    private void createIndices(DendriteGraph graph) {
        TitanTransaction tx = graph.newTransaction();

        if (tx.getType("jungEigenvectorCentrality") == null) {
            tx.makeKey("jungEigenvectorCentrality")
                    .dataType(FullDouble.class)
                    .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                    .make();
        }

        tx.commit();
    }

}
