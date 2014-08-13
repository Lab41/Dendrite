package org.lab41.dendrite.jobs.jung;

import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import org.lab41.dendrite.jobs.AbstractGraphUpdateJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;

public class BetweennessCentralityJob extends AbstractGraphUpdateJob {

    private static String BETWEENNESS_KEY = "jungBetweenness";

    public BetweennessCentralityJob(MetaGraph metaGraph, JobMetadata.Id jobId, DendriteGraph graph) {
        super(metaGraph, jobId, graph);
    }

    @Override
    protected void updateGraph() {
        GraphJung<DendriteGraph> jungGraph = new GraphJung<>(graph);
        BetweennessCentrality<Vertex, Edge> betweennessCentrality = new BetweennessCentrality<>(jungGraph);

        // Instructs the ranker that it should not remove the rank scores from
        //  the nodes (or edges) once the ranks have been computed.
        betweennessCentrality.setRemoveRankScoresOnFinalize(false);

        // Performs the iterative process. Note: this method does not return
        // anything because Java does not allow mixing double, int, or objects
        // TODO: change this to step() so that insight into the process can be
        // exposed
        betweennessCentrality.evaluate();

        for (Vertex vertex: graph.getVertices()) {
            Double score = betweennessCentrality.getVertexRankScore(vertex);
            vertex.setProperty(BETWEENNESS_KEY, score);
        }
    }

    @Override
    protected void createIndices() {
        createVertexIndex(BETWEENNESS_KEY, FullDouble.class);
    }
}
