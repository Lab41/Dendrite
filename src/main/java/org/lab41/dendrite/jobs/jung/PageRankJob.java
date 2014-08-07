package org.lab41.dendrite.jobs.jung;

import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import org.lab41.dendrite.jobs.AbstractGraphUpdateJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;

public class PageRankJob extends AbstractGraphUpdateJob {

    private static String PAGERANK_KEY = "jungPagerank";
    private double alpha;

    public PageRankJob(MetaGraph metaGraph, JobMetadata.Id jobId, DendriteGraph graph, double alpha) {
        super(metaGraph, jobId, graph);

        this.alpha = alpha;
    }

    @Override
    protected void updateGraph() {
        GraphJung<DendriteGraph> jungGraph = new GraphJung<>(graph);
        PageRank<Vertex, Edge> pageRank = new PageRank<>(jungGraph, alpha);
        pageRank.evaluate();

        for (Vertex vertex: graph.getVertices()) {
            Double score = pageRank.getVertexScore(vertex);
            vertex.setProperty(PAGERANK_KEY, score);
        }
    }

    @Override
    protected void createIndices() {
        createVertexIndex(PAGERANK_KEY, FullDouble.class);
    }
}
