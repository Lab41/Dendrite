package org.lab41.dendrite.metagraph;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.gremlingroovy.GremlinGroovyModule;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import com.tinkerpop.frames.modules.typedgraph.TypedGraphModuleBuilder;
import org.lab41.dendrite.graph.DendriteGraph;
import org.lab41.dendrite.graph.DendriteGraphFactory;
import org.lab41.dendrite.metagraph.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class MetaGraph {

    static Logger logger = LoggerFactory.getLogger(MetaGraph.class);

    static String METADATA_GRAPH_NAME = "metadata";

    private DendriteGraph metadataGraph;
    private DendriteGraphFactory graphFactory;
    private FramedGraphFactory frameFactory;

    public MetaGraph(DendriteGraphFactory graphFactory) {

        this.graphFactory = graphFactory;

        // Get or create the metadata graph.
        this.metadataGraph = graphFactory.openGraph(METADATA_GRAPH_NAME, null, true);

        // Create a FramedGraphFactory, which we'll use to wrap our metadata graph vertices and edges.
        this.frameFactory = new FramedGraphFactory(
                new GremlinGroovyModule(),
                new JavaHandlerModule(),
                new TypedGraphModuleBuilder()
                        .withClass(ProjectMetadata.class)
                        .withClass(GraphMetadata.class)
                        .withClass(JobMetadata.class)
                        .build()
        );

        createMetadataGraphKeys();
        loadGraphs();
    }

    public DendriteGraph getGraph(String id) {
        DendriteGraph graph = graphFactory.getGraph(id);
        if (graph == null) {
            // Check if we have a graph metadata for this id. If so, open up the graph.

            MetaGraphTx tx = newTransaction();
            GraphMetadata graphMetadata = tx.getGraph(id);
            if (graphMetadata != null) {
                graph = graphFactory.openGraph(id, graphMetadata.getConfiguration());
            }

            tx.commit();
        }

        return graph;
    }

    public MetaGraphTx newTransaction() {
        return new MetaGraphTx(metadataGraph, frameFactory);
    }

    private void createMetadataGraphKeys() {
        // Metadata keys
        if (metadataGraph.getType("type") == null) {
            metadataGraph.makeKey("type")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .indexed(Edge.class)
                    .make();
        }

        // NamedMetadata keys
        if (metadataGraph.getType("name") == null) {
            metadataGraph.makeKey("name")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .indexed(Edge.class)
                    .make();
        }

        if (metadataGraph.getType("typeAndName") == null) {
            metadataGraph.makeKey("typeAndName")
                    .dataType(String.class)
                    .unique()
                    .indexed(Vertex.class)
                    .make();
        }

        // ProjectMetadata keys
        if (metadataGraph.getType("graphHead") == null) {
            metadataGraph.makeLabel("graphHead").oneToOne().make();
        }

        if (metadataGraph.getType("ownsJob") == null) {
            metadataGraph.makeLabel("ownsJob").oneToMany().make();
        }

        if (metadataGraph.getType("ownsJob") == null) {
            metadataGraph.makeLabel("ownsJob").oneToMany().make();
        }

        // GraphMetadata keys
        if (metadataGraph.getType("properties") == null) {
            metadataGraph.makeKey("properties")
                    .dataType(Properties.class)
                    .indexed(Vertex.class)
                    .make();
        }

        // JobMetadata keys
        if (metadataGraph.getType("state") == null) {
            metadataGraph.makeKey("state")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (metadataGraph.getType("progress") == null) {
            metadataGraph.makeKey("progress")
                    .dataType(Float.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (metadataGraph.getType("mapreduceJobId") == null) {
            metadataGraph.makeKey("mapreduceJobId")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (metadataGraph.getType("childJob") == null) {
            metadataGraph.makeLabel("childJob").oneToMany().make();
        }

        if (metadataGraph.getType("parentJob") == null) {
            metadataGraph.makeLabel("parentJob").manyToOne().make();
        }

        metadataGraph.commit();
    }

    private void loadGraphs() {
        MetaGraphTx tx = newTransaction();

        for(GraphMetadata graphMetadata: tx.getGraphs()) {
            graphFactory.openGraph(graphMetadata.getId(), graphMetadata.getConfiguration());
        }

        tx.commit();
    }
}
