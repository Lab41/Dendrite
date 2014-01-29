package org.lab41.dendrite.metagraph;

import com.thinkaurelius.titan.core.Order;
import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.core.TitanType;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.gremlingroovy.GremlinGroovyModule;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import com.tinkerpop.frames.modules.typedgraph.TypedGraphModuleBuilder;
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

        // Allow the system to overload the properties.
        Properties systemProperties = new Properties();
        loadGraphProperties(systemGraphName, config.subset("metagraph.system"), systemProperties);

        this.systemGraph = new DendriteGraph(systemGraphName, systemProperties);

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
        return new MetaGraphTx(systemGraph.newTransaction(), frameFactory);
    }

    public MetaGraphTransactionBuilder buildTransaction() {
        return new MetaGraphTransactionBuilder(systemGraph.buildTransaction(), frameFactory);
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
        DendriteGraphTx tx = systemGraph.newTransaction();

        try {
            // Metadata keys
            if (tx.getType("type") == null) {
                tx.makeKey("type")
                        .dataType(String.class)
                        .indexed(Vertex.class)
                        .indexed(Edge.class)
                        .make();
            }

            // NamedMetadata keys
            TitanType name = tx.getType("name");
            if (name == null) {
                name = tx.makeKey("name")
                        .dataType(String.class)
                        .indexed(Vertex.class)
                        .indexed(Edge.class)
                        .make();
            }

            if (tx.getType("typeAndName") == null) {
                tx.makeKey("typeAndName")
                        .dataType(String.class)
                        .unique()
                        .indexed(Vertex.class)
                        .make();
            }

            // ProjectMetadata keys
            TitanType creationTime = tx.getType("creationTime");
            if (creationTime == null) {
                creationTime = tx.makeKey("creationTime")
                        .dataType(Date.class)
                        .indexed(Vertex.class)
                        .make();
            }

            if (tx.getType("currentBranch") == null) {
                tx.makeLabel("currentBranch")
                        .oneToOne()
                        .make();
            }

            if (tx.getType("ownsBranch") == null) {
                tx.makeLabel("ownsBranch")
                        .oneToMany()
                        .sortKey(name)
                        .make();
            }

            if (tx.getType("ownsGraph") == null) {
                tx.makeLabel("ownsGraph")
                        .oneToMany()
                        .sortKey(creationTime)
                        .sortOrder(Order.DESC)
                        .make();
            }

            if (tx.getType("ownsJob") == null) {
                tx.makeLabel("ownsJob")
                        .oneToMany()
                        .sortKey(creationTime)
                        .sortOrder(Order.DESC)
                        .make();
            }

            // BranchMetadata keys
            if (tx.getType("branchTarget") == null) {
                tx.makeLabel("branchTarget")
                        .manyToOne()
                        .make();
            }

            // GraphMetadata keys
            if (tx.getType("properties") == null) {
                tx.makeKey("properties")
                        .dataType(Properties.class)
                        .indexed(Vertex.class)
                        .make();
            }

            if (tx.getType("childGraph") == null) {
                tx.makeLabel("childGraph")
                        .oneToMany()
                        .sortKey(creationTime)
                        .sortOrder(Order.DESC)
                        .make();
            }

            // JobMetadata keys
            if (tx.getType("doneTime") == null) {
                tx.makeKey("doneTime")
                        .dataType(Date.class)
                        .indexed(Vertex.class)
                        .make();
            }

            if (tx.getType("state") == null) {
                tx.makeKey("state")
                        .dataType(String.class)
                        .indexed(Vertex.class)
                        .make();
            }

            if (tx.getType("progress") == null) {
                tx.makeKey("progress")
                        .dataType(Float.class)
                        .indexed(Vertex.class)
                        .make();
            }

            if (tx.getType("mapreduceJobId") == null) {
                tx.makeKey("mapreduceJobId")
                        .dataType(String.class)
                        .indexed(Vertex.class)
                        .make();
            }

            if (tx.getType("childJob") == null) {
                tx.makeLabel("childJob")
                        .oneToMany()
                        .sortKey(creationTime)
                        .sortOrder(Order.DESC)
                        .make();
            }
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        tx.commit();
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
            graph = loadGraph(graphMetadata);
        }

        tx.commit();

        return graph;
    }

    /**
     * Load a graph. This graph is in a closed state.
     *
     * @param graphMetadata The graph.
     * @return The graph or null.
     */
    synchronized private DendriteGraph loadGraph(GraphMetadata graphMetadata) {
        // We don't want to end up with the same graph opened multiple times, so we'll sit behind a mutex.
        // Because of this, we should check again if the graph has been opened up in another thread.
        String id = graphMetadata.getId();
        Properties properties = graphMetadata.getProperties();

        DendriteGraph graph = graphs.get(id);
        if (graph == null) {
            // Create a default config if we were passed a null config.
            if (properties == null) {
                properties = getGraphProperties(id);

                graphMetadata.setProperties(properties);
            }

            graph = new DendriteGraph(id, properties);

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
    private Properties getGraphProperties(String id) {
        Properties properties = new Properties();
        loadGraphProperties(id, config.subset("metagraph.template"), properties);
        return properties;
    }

    private void loadGraphProperties(String id, Configuration config, Properties properties) {
        // Add our prefix to the name so we can keep the databases organized.
        String name = config.getString("name-prefix", GRAPH_NAME_PREFIX_DEFAULT) + id;

        String storageBackend = config.getString("storage.backend");
        setProperty(properties, "storage.backend", storageBackend);
        setProperty(properties, "storage.read-only", "false");

        String storageDirectory = config.getString("storage.directory", null);
        if (storageDirectory != null) {
            String dir = (new File(storageDirectory, name)).getPath();
            setProperty(properties, "storage.directory", dir);
        }

        setProperty(properties, "storage.hostname", config.getString("storage.hostname", null));
        setProperty(properties, "storage.port", config.getString("storage.port", null));

        if (storageBackend != null && storageBackend.equals("hbase")) {
            setProperty(properties, "storage.tablename", name);
        }

        String indexBackend = config.getString("storage.index.backend", null);
        if (indexBackend != null) {
            setProperty(properties, "storage.index.search.index-name", name);
            setProperty(properties, "storage.index.search.backend", indexBackend);
            setProperty(properties, "storage.index.search.hostname", config.getString("storage.index.hostname", null));
            setProperty(properties, "storage.index.search.client-only", config.getString("storage.index.client-only", null));
            setProperty(properties, "storage.index.search.local-mode", config.getString("storage.index.local-mode", null));
            setProperty(properties, "storage.index.search.cluster-name", config.getString("storage.index.cluster-name", null));

            String indexDirectory = config.getString("storage.index.directory", null);
            if (indexDirectory != null) {
                String dir = (new File(indexDirectory, name)).getPath();
                setProperty(properties, "storage.index.search.directory", dir);
            }
        }
    }

    private void setProperty(Properties property, String key, String value) {
        if (value != null) {
            property.setProperty(key, value);
        }
    }
}
