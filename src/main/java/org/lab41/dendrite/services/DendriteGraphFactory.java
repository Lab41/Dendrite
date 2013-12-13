package org.lab41.dendrite.services;

import com.thinkaurelius.titan.core.TitanGraph;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    private Map<String, TitanGraph> graphs = new HashMap<>();

    public Configuration getConfiguration(String name) {
        BaseConfiguration config = new BaseConfiguration();

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

    public Set<String> getGraphNames() {
        return graphs.keySet();
    }

    public Collection<TitanGraph> getGraphs() {
        return graphs.values();
    }

    public TitanGraph getGraph(String name) {
        return graphs.get(name);
    }

    public TitanGraph openGraph(String name) {
        TitanGraph graph = graphs.get(name);
        if (graph == null) {
            Configuration configuration = getConfiguration(name);

            logger.debug("opening graph " + name + " [" + storageBackend + "]");

            graph = com.thinkaurelius.titan.core.TitanFactory.open(configuration);
            graphs.put(name, graph);
        }

        return graph;
    }

    public void stop() {
        for (TitanGraph graph: graphs.values()) {
            graph.shutdown();
        }
    }
}
