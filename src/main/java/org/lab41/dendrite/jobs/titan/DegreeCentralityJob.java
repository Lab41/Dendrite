package org.lab41.dendrite.jobs.titan;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.lab41.dendrite.jobs.AbstractGraphUpdateJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;

public class DegreeCentralityJob extends AbstractGraphUpdateJob {

    private static String IN_DEGREES_KEY = "titanInDegrees";
    private static String OUT_DEGREES_KEY = "titanOutDegrees";
    private static String DEGREES_KEY = "titanDegrees";

    public DegreeCentralityJob(MetaGraph metaGraph, String jobId, DendriteGraph graph) {
        super(metaGraph, jobId, graph);
    }

    @Override
    protected void updateGraph() {
        for (Vertex vertex: graph.getVertices()) {
            int inDegrees = 0;
            int outDegrees = 0;

            for (Edge ignored : vertex.getEdges(Direction.IN)) {
                inDegrees += 1;
            }

            for (Edge ignored : vertex.getEdges(Direction.OUT)) {
                outDegrees += 1;
            }

            vertex.setProperty(IN_DEGREES_KEY, inDegrees);
            vertex.setProperty(OUT_DEGREES_KEY, outDegrees);
            vertex.setProperty(DEGREES_KEY, inDegrees + outDegrees);
        }
    }

    @Override
    protected void createIndices() {
        createVertexIndex(IN_DEGREES_KEY, Integer.class);
        createVertexIndex(OUT_DEGREES_KEY, Integer.class);
        createVertexIndex(DEGREES_KEY, Integer.class);
    }
}
