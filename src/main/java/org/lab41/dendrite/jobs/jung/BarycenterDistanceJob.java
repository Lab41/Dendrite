package org.lab41.dendrite.jobs.jung;

import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.scoring.BarycenterScorer;
import org.lab41.dendrite.jobs.AbstractGraphUpdateJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;

public class BarycenterDistanceJob extends AbstractGraphUpdateJob {

    private static String BARYCENTER_KEY = "jungBarycenter";

    public BarycenterDistanceJob(MetaGraph metaGraph, String jobId, DendriteGraph graph) {
        super(metaGraph, jobId, graph);
    }

    @Override
    protected void updateGraph() {
        GraphJung<DendriteGraph> jungGraph = new GraphJung<>(graph);
        BarycenterScorer<Vertex, Edge> barycenterScorer = new BarycenterScorer<>(jungGraph);

        for (Vertex vertex: jungGraph.getVertices()) {
            Double score = barycenterScorer.getVertexScore(vertex);
            vertex.setProperty(BARYCENTER_KEY, score);
        }
    }

    @Override
    protected void createIndices() {
        createVertexIndex(BARYCENTER_KEY, FullDouble.class);
    }
}
