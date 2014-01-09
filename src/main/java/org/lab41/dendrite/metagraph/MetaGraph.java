package org.lab41.dendrite.metagraph;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.gremlingroovy.GremlinGroovyModule;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import com.tinkerpop.frames.modules.typedgraph.TypedGraphModuleBuilder;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.lab41.dendrite.metagraph.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class MetaGraph {

    static Logger logger = LoggerFactory.getLogger(MetaGraph.class);

    static String SYSTEM_GRAPH_NAME_DEFAULT = "system";
    static String GRAPH_NAME_PREFIX_DEFAULT = "dendrite-";

    private Configuration config;

    private DendriteGraph systemGraph;
    private FramedGraphFactory frameFactory;

    private Map<String, DendriteGraph> graphs = new HashMap<>();

    public MetaGraph(Configuration config) {
        this.config = config;

        // Get or create the metadata graph.
        String systemGraphName = config.getString("metagraph.system.name", SYSTEM_GRAPH_NAME_DEFAULT);
        Configuration systemConfig = getGraphConfig(systemGraphName);
        this.systemGraph = new DendriteGraph(systemGraphName, systemConfig);

        // Create a FramedGraphFactory, which we'll use to wrap our metadata graph vertices and edges.
        this.frameFactory = new FramedGraphFactory(
                new GremlinGroovyModule(),
                new JavaHandlerModule(),
                new TypedGraphModuleBuilder()
                        .withClass(ProjectMetadata.class)
                        .withClass(BranchMetadata.class)
                        .withClass(GraphMetadata.class)
                        .withClass(JobMetadata.class)
                        .build()
        );

        createMetadataGraphKeys();
    }

    /**
     * Return all the known graphs.
     *
     * @return a collection of all the graphs.
     */
    public Collection<DendriteGraph> getGraphs() {
        return getGraphs(false);
    }

    /**
     * Return all the known graphs.
     *
     * @param includeSystemGraph Should we include the hidden system graph?
     * @return a collection of all the graphs.
     */
    public Collection<DendriteGraph> getGraphs(boolean includeSystemGraph) {
        List<DendriteGraph> graphs = new ArrayList<>();

        if (includeSystemGraph) {
            graphs.add(systemGraph);
        }

        // We can't just return our graph cache because it's possible another process has created a graph. So to be
        // safe we need fetch all the GraphMetadatas from the database in order to make sure they are loaded.
        MetaGraphTx tx = newTransaction();

        for (GraphMetadata graphMetadata: tx.getGraphs()) {
            graphs.add(getGraph(graphMetadata.getId()));
        }

        tx.commit();

        return graphs;
    }

    /**
     * Get the system graph.
     *
     * @return The system graph.
     */
    public DendriteGraph getSystemGraph() {
        return systemGraph;
    }

    /**
     * Get a graph.
     *
     * @param id The graph id.
     * @return The graph.
     */
    public DendriteGraph getGraph(String id) {
        return getGraph(id, false);
    }

    /**
     * Get a graph.
     *
     * @param id The graph id.
     * @param includeSystemGraph Should we include the hidden system graph?
     * @return The graph or null.
     */
    public DendriteGraph getGraph(String id, boolean includeSystemGraph) {
        DendriteGraph graph = graphs.get(id);
        if (graph == null) {
            // Is it the system graph?
            if (systemGraph.getId().equals(id)) {
                if (includeSystemGraph) {
                    graph = systemGraph;
                }
            } else {
                // Maybe another process has created this graph, so try to load it.
                graph = loadGraph(id);
            }
        }

        return graph;
    }

    /**
     * Open a metagraph transaction.
     *
     * @return The transaction.
     */
    public MetaGraphTx newTransaction() {
        return new MetaGraphTx(systemGraph, frameFactory);
    }

    /**
     * Shut down all the active graphs.
     */
    public void stop() {
        for (DendriteGraph graph: graphs.values()) {
            graph.shutdown();
        }
    }

    /**
     * Create the metagraph indices.
     */
    private void createMetadataGraphKeys() {
        // Metadata keys
        if (systemGraph.getType("type") == null) {
            systemGraph.makeKey("type")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .indexed(Edge.class)
                    .make();
        }

        // NamedMetadata keys
        if (systemGraph.getType("name") == null) {
            systemGraph.makeKey("name")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .indexed(Edge.class)
                    .make();
        }

        if (systemGraph.getType("typeAndName") == null) {
            systemGraph.makeKey("typeAndName")
                    .dataType(String.class)
                    .unique()
                    .indexed(Vertex.class)
                    .make();
        }

        // ProjectMetadata keys
        if (systemGraph.getType("currentBranch") == null) {
            systemGraph.makeLabel("currentBranch").oneToOne().make();
        }

        if (systemGraph.getType("ownsBranch") == null) {
            systemGraph.makeLabel("ownsBranch").oneToMany().make();
        }

        if (systemGraph.getType("ownsGraph") == null) {
            systemGraph.makeLabel("ownsGraph").oneToMany().make();
        }

        if (systemGraph.getType("ownsJob") == null) {
            systemGraph.makeLabel("ownsJob").oneToMany().make();
        }

        // BranchMetadata keys
        if (systemGraph.getType("branchTarget") == null) {
            systemGraph.makeLabel("branchTarget").oneToOne().make();
        }

        // GraphMetadata keys
        if (systemGraph.getType("properties") == null) {
            systemGraph.makeKey("properties")
                    .dataType(Properties.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (systemGraph.getType("childGraph") == null) {
            systemGraph.makeLabel("childGraph").oneToMany().make();
        }

        // JobMetadata keys
        if (systemGraph.getType("state") == null) {
            systemGraph.makeKey("state")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (systemGraph.getType("progress") == null) {
            systemGraph.makeKey("progress")
                    .dataType(Float.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (systemGraph.getType("mapreduceJobId") == null) {
            systemGraph.makeKey("mapreduceJobId")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (systemGraph.getType("childJob") == null) {
            systemGraph.makeLabel("childJob").oneToMany().make();
        }

        systemGraph.commit();
    }

    /**
     * Load a graph. This graph is in a closed state.
     *
     * @param id The graph id.
     * @return The graph or null.
     */
    private DendriteGraph loadGraph(String id) {
        DendriteGraph graph = null;

        MetaGraphTx tx = newTransaction();

        GraphMetadata graphMetadata = tx.getGraph(id);
        if (graphMetadata != null) {
            graph = loadGraph(id, graphMetadata.getConfiguration());
        }

        tx.commit();

        return graph;
    }

    /**
     * Load a graph. This graph is in a closed state.
     *
     * @param id The graph id.
     * @param config If null, load the graph with the default config. Otherwise use this one.
     * @return The graph or null.
     */
    synchronized private DendriteGraph loadGraph(String id, Configuration config) {
        // We don't want to end up with the same graph opened multiple times, so we'll sit behind a mutex.
        // Because of this, we should check again if the graph has been opened up in another thread.
        DendriteGraph graph = graphs.get(id);
        if (graph == null) {
            // Create a default config if we were passed a null config.
            if (config == null) {
                config = getGraphConfig(id);
            }

            graph = new DendriteGraph(id, config);

            graphs.put(id, graph);
        }

        return graph;
    }

    /**
     * Create a default configuration for a graph.
     *
     * @param id The graph id.
     * @return The configuration.
     */
    private Configuration getGraphConfig(String id) {
        Configuration graphConfig = new BaseConfiguration();

        // Add our prefix to the name so we can keep the databases organized.
        String name = config.getString("metagraph.template.name-prefix", GRAPH_NAME_PREFIX_DEFAULT) + id;

        Configuration storage = graphConfig.subset("storage");

        String storageBackend = config.getString("metagraph.template.storage.backend");
        storage.setProperty("backend", storageBackend);
        storage.setProperty("read-only", false);

        String storageDirectory = config.getString("metagraph.template.storage.directory", null);
        if (storageDirectory != null) {
            String dir = (new File(storageDirectory, name)).getPath();
            storage.setProperty("directory", dir);
        }

        storage.setProperty("hostname", config.getString("metagraph.template.storage.hostname", null));
        storage.setProperty("port", config.getString("metadata.template.storage.port", null));

        if (storageBackend.equals("hbase")) {
            storage.setProperty("tablename", name);
        }

        String indexBackend = config.getString("metagraph.template.storage.index.backend", null);
        if (indexBackend != null) {
            Configuration index = storage.subset("index").subset("search");

            index.setProperty("index-name", name);
            index.setProperty("backend", indexBackend);
            index.setProperty("hostname", config.getString("metagraph.template.storage.index.hostname", null));
            index.setProperty("client-only", config.getString("metagraph.template.storage.index.client-only", null));
            index.setProperty("local-mode", config.getString("metagraph.template.storage.index.local-mode", null));
            index.setProperty("cluster-name", config.getString("metagraph.template.storage.index.cluster-name", null));

            String indexDirectory = config.getString("metagraph.template.storage.index.directory", null);
            if (indexDirectory != null) {
                String dir = (new File(indexDirectory, name)).getPath();
                index.setProperty("directory", dir);
            }
        }

        return graphConfig;
    }
}
