package org.lab41.dendrite.jobs.jung;

import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import org.lab41.dendrite.jobs.AbstractGraphUpdateJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;

public class ClosenessCentralityJob extends AbstractGraphUpdateJob {

    private static String CLOSENESS_KEY = "jungCloseness";

    public ClosenessCentralityJob(MetaGraph metaGraph, JobMetadata.Id jobId, DendriteGraph graph) {
        super(metaGraph, jobId, graph);
    }

    @Override
    protected void updateGraph() {
        GraphJung<DendriteGraph> jungGraph = new GraphJung<>(graph);
        ClosenessCentrality<Vertex, Edge> closenessCentrality = new ClosenessCentrality<>(jungGraph);

        for (Vertex vertex: graph.getVertices()) {
            Double score = closenessCentrality.getVertexScore(vertex);
            vertex.setProperty(CLOSENESS_KEY, score);
        }
    }

    @Override
    protected void createIndices() {
        createVertexIndex(CLOSENESS_KEY, FullDouble.class);
    }
}
