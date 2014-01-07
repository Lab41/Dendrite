package org.lab41.dendrite.graph;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
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

    private Map<String, DendriteGraph> graphs = new HashMap<>();

    public Set<String> getGraphNames() {
        return getGraphNames(false);
    }

    public Set<String> getGraphNames(boolean includeSystemGraphs) {
        if (includeSystemGraphs) {
            return graphs.keySet();
        } else {
            Set<String> values = new HashSet<>();

            for (Map.Entry<String, DendriteGraph> entry: graphs.entrySet()) {
                // Filter out the system graphs.
                if (!entry.getValue().isSystemGraph()) {
                    values.add(entry.getKey());
                }
            }
            return values;
        }
    }

    public Collection<DendriteGraph> getGraphs() {
        return getGraphs(false);
    }

    public Collection<DendriteGraph> getGraphs(boolean includeSystemGraphs) {
        if (includeSystemGraphs) {
            return graphs.values();
        } else {
            List<DendriteGraph> values = new ArrayList<>();

            for (Map.Entry<String, DendriteGraph> entry: graphs.entrySet()) {
                // Filter out the system graphs.
                if (!entry.getValue().isSystemGraph()) {
                    values.add(entry.getValue());
                }
            }

            return values;
        }
    }

    public DendriteGraph getGraph(String id) {
        return getGraph(id, false);
    }

    public DendriteGraph getGraph(String id, boolean includeSystemGraphs) {
        DendriteGraph graph = graphs.get(id);
        if (graph == null || (!includeSystemGraphs && graph.isSystemGraph())) {
            return null;
        }
        return graph;
    }

    public DendriteGraph openGraph(String id, Configuration config) {
        return openGraph(id, config, false);
    }

    public DendriteGraph openGraph(String id, Configuration config, boolean systemGraph) {
        DendriteGraph graph = graphs.get(id);
        if (graph == null) {
            if (config == null) {
                config = getConfiguration(id);
            }

            graph = new DendriteGraph(id, config, systemGraph);

            graphs.put(id, graph);
        }

        return graph;
    }

    public void stop() {
        for (DendriteGraph graph: graphs.values()) {
            graph.shutdown();
        }
    }

    private Configuration getConfiguration(String name) {
        Configuration config = new BaseConfiguration();

        // Add our prefix to the name so we can keep the databases organized.
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

        return config;
    }
}
