package org.lab41.dendrite.services;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.gremlingroovy.GremlinGroovyModule;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import com.tinkerpop.frames.modules.typedgraph.TypedGraphModuleBuilder;
import org.lab41.dendrite.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class MetadataService {

    static Logger logger = LoggerFactory.getLogger(MetadataService.class);

    static String METADATA_GRAPH_NAME = "metadata";

    private DendriteGraphFactory dendriteGraphFactory;
    private TitanGraph metadataGraph;
    private FramedGraphFactory metadataFrameFactory;

    @Autowired
    public MetadataService(DendriteGraphFactory dendriteGraphFactory) {
        this.dendriteGraphFactory = dendriteGraphFactory;

        // Get or create the metadata graph.
        this.metadataGraph = dendriteGraphFactory.openGraph(METADATA_GRAPH_NAME);

        // Create a FramedGraphFactory, which we'll use to wrap our metadata graph vertices and edges.
        this.metadataFrameFactory = new FramedGraphFactory(
                new GremlinGroovyModule(),
                new JavaHandlerModule(),
                new TypedGraphModuleBuilder()
                        //.withClass(NamedMetadata.class)
                        .withClass(ProjectMetadata.class)
                        .withClass(GraphMetadata.class)
                        .withClass(JobMetadata.class)
                                //.withClass(HadoopJobMetadata.class)
                        .build()
        );

        createMetadataGraphKeys();
        loadGraphs();
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
        if (metadataGraph.getType("backend") == null) {
            metadataGraph.makeKey("backend")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (metadataGraph.getType("directory") == null) {
            metadataGraph.makeKey("directory")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (metadataGraph.getType("hostname") == null) {
            metadataGraph.makeKey("hostname")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (metadataGraph.getType("port") == null) {
            metadataGraph.makeKey("port")
                    .dataType(Integer.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (metadataGraph.getType("tablename") == null) {
            metadataGraph.makeKey("tablename")
                    .dataType(String.class)
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
        MetadataTx tx = newTransaction();

        for(GraphMetadata graphMetadata: tx.getGraphs()) {
            dendriteGraphFactory.openGraph(graphMetadata.getId());
        }

        tx.commit();
    }

    public TitanGraph getGraph(String id) {
        TitanGraph graph = dendriteGraphFactory.getGraph(id);
        if (graph == null) {
            // Check if we have a graph metadata for this id. If so, open up the graph.

            MetadataTx tx = newTransaction();
            GraphMetadata graphMetadata = tx.getGraph(id);
            if (graphMetadata != null) {
                graph = dendriteGraphFactory.openGraph(id);
            }

            tx.commit();
        }

        return graph;
    }

    public void createGraph(String id) {
        dendriteGraphFactory.getGraph(id);
    }

    public MetadataTx newTransaction() {
        return new MetadataTx(metadataGraph, metadataFrameFactory);
    }
}
