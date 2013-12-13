package org.lab41.dendrite.graph;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class DendriteGraphFactory {

    private Logger logger = LoggerFactory.getLogger(DendriteGraphFactory.class);

    @Value("${dendrite-graph-factory.name-prefix:dendrite-}")
    private String namePrefix;

    @Value("${dendrite-graph-factory.storage.backend:#{null}}")
    private String storageBackend;

    @Value("${dendrite-graph-factory.storage.directory:#{null}}")
    private String storageDirectory;

    @Value("${dendrite-graph-factory.storage.hostname:#{null}}")
    private String storageHostname;

    @Value("${dendrite-graph-factory.storage.port:#{null}}")
    private String storagePort;

    @Value("${dendrite-graph-factory.storage.index.backend:#{null}}")
    private String indexBackend;

    @Value("${dendrite-graph-factory.storage.index.hostname:#{null}}")
    private String indexHostname;

    @Value("${dendrite-graph-factory.storage.index.cluster-name:#{null}}")
    private String indexClusterName;

    @Value("${dendrite-graph-factory.storage.index.local-mode:#{null}}")
    private String indexLocalMode;

    @Value("${dendrite-graph-factory.storage.index.directory:#{null}}")
    private String indexDirectory;

    @Value("${dendrite-graph-factory.storage.index.client-only:#{null}}")
    private String indexClientOnly;

    private Map<String, Configuration> configs = new HashMap<>();
    private Map<String, DendriteGraph> graphs = new HashMap<>();

    public Configuration getConfiguration(String name) {
        Configuration config = configs.get(name);
        if (config == null) {
            config = new BaseConfiguration();
            configs.put(name, config);

            name = namePrefix + name;

            Configuration storage = config.subset("storage");
            storage.setProperty("backend", storageBackend);
            storage.setProperty("read-only", false);

            if (storageDirectory != null) {
                String dir = (new File(storageDirectory, name)).getPath();
                storage.setProperty("directory", dir);
            }

            storage.setProperty("hostname", storageHostname);
            storage.setProperty("port", storagePort);

            if (storageBackend.equals("hbase")) {
                storage.setProperty("tablename", name);
            }

            Configuration index = storage.subset("index").subset("search");
            index.setProperty("index-name", name);
            index.setProperty("backend", indexBackend);
            index.setProperty("hostname", indexHostname);
            index.setProperty("client-only", indexClientOnly);
            index.setProperty("local-mode", indexLocalMode);

            if (indexDirectory != null) {
                String dir = (new File(indexDirectory, name)).getPath();
                index.setProperty("directory", dir);
            }
        }

        return config;
    }

    public Set<String> getGraphNames() {
        return graphs.keySet();
    }

    public Collection<DendriteGraph> getGraphs() {
        return graphs.values();
    }

    public DendriteGraph getGraph(String name) {
        return graphs.get(name);
    }

    public DendriteGraph openGraph(String id) {
        DendriteGraph graph = graphs.get(id);
        if (graph == null) {
            TitanGraph titanGraph = openTitanGraph(id);
            RexsterApplicationGraph rexsterGraph = openRexsterGraph(id, titanGraph);
            graph = new DendriteGraph(id, titanGraph, rexsterGraph);

            graphs.put(id, graph);
        }

        return graph;
    }

    public DendriteGraph openSystemGraph(String id) {
        DendriteGraph graph = graphs.get(id);
        if (graph == null) {
            TitanGraph titanGraph = openTitanGraph(id);
            RexsterApplicationGraph rexsterGraph = openRexsterGraph(id, titanGraph);
            graph = new DendriteGraph(id, titanGraph, rexsterGraph, true);

            graphs.put(id, graph);
        }

        return graph;
    }

    public void stop() {
        for (DendriteGraph graph: graphs.values()) {
            graph.shutdown();
        }
    }

    private TitanGraph openTitanGraph(String name) {
        Configuration configuration = getConfiguration(name);

        logger.debug("opening titan graph " + name + " with backend " + storageBackend + "");

        return TitanFactory.open(configuration);
    }

    /// Create a rexster graph with gremlin enabled
    private RexsterApplicationGraph openRexsterGraph(String id,TitanGraph graph) {
        List<String> allowableNamespaces = new ArrayList<>();
        allowableNamespaces.add("tp:gremlin");

        List<HierarchicalConfiguration> extensionConfigurations = new ArrayList<>();

        return new RexsterApplicationGraph(id, graph, allowableNamespaces, extensionConfigurations);
    }
}
