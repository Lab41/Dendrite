package org.lab41.dendrite.jobs.jung;

import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import org.lab41.dendrite.jobs.AbstractGraphUpdateJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;

public class EigenvectorCentralityJob extends AbstractGraphUpdateJob {

    private static String EIGENVECTOR_KEY = "jungEigenvector";

    public EigenvectorCentralityJob(MetaGraph metaGraph, JobMetadata.Id jobId, DendriteGraph graph) {
        super(metaGraph, jobId, graph);
    }

    @Override
    protected void updateGraph() {
        GraphJung<DendriteGraph> jungGraph = new GraphJung<>(graph);
        EigenvectorCentrality<Vertex, Edge> eigenvectorCentrality = new EigenvectorCentrality<>(jungGraph);
        eigenvectorCentrality.acceptDisconnectedGraph(true);
        eigenvectorCentrality.evaluate();

        for (Vertex vertex: graph.getVertices()) {
            Double score = eigenvectorCentrality.getVertexScore(vertex);
            vertex.setProperty(EIGENVECTOR_KEY, score);
        }
    }

    @Override
    protected void createIndices() {
        createVertexIndex(EIGENVECTOR_KEY, FullDouble.class);
    }
}
