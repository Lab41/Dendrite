package org.lab41.dendrite.services;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration;
import com.tinkerpop.rexster.server.AbstractMapRexsterApplication;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class DendriteGraphFactory {

    @Value("${dendrite-graph-factory.storage.backend}")
    private String storageBackend;

    @Value("${dendrite-graph-factory.storage.directory:null}")
    private String storageDirectory;

    /*
    @Value("${dendrite-graph-factory.storage.hostname:null}")
    private String storageHostname;

    @Value("${dendrite-graph-factory.storage.port:null}}")
    private String storagePort;

    @Value("${dendrite-graph-factory.storage.index.backend:null}")
    private String indexBackend;

    @Value("${dendrite-graph-factory.storage.index.hostname:null}")
    private String indexHostname;

    @Value("${dendrite-graph-factory.storage.index.index-name:null}")
    private String indexIndexName;

    @Value("${dendrite-graph-factory.storage.index.cluster-name:null}")
    private String indexClusterName;

    @Value("${dendrite-graph-factory.storage.index.local-mode:null}")
    private String indexLocalMode;

    @Value("${dendrite-graph-factory.storage.index.directory:null}")
    private String indexDirectory;

    @Value("${dendrite-graph-factory.storage.index.client-only:null}")
    private String indexClientOnly;
    */

    private Map<String, TitanGraph> graphs = new HashMap<>();

    public Configuration getConfiguration(String name) {
        BaseConfiguration configuration = new BaseConfiguration();

        Configuration storage = configuration.subset(GraphDatabaseConfiguration.STORAGE_NAMESPACE);
        storage.setProperty(GraphDatabaseConfiguration.STORAGE_BACKEND_KEY, storageBackend);
        storage.setProperty(GraphDatabaseConfiguration.STORAGE_READONLY_KEY, false);

        /*
        storage.setProperty(GraphDatabaseConfiguration.HOSTNAME_KEY, storageHostname);
        storage.setProperty(GraphDatabaseConfiguration.PORT_KEY, storagePort);
        */

        if (storageDirectory != null) {
            String dir = (new File(storageDirectory, name)).getPath();
            storage.setProperty(GraphDatabaseConfiguration.STORAGE_DIRECTORY_KEY, dir);
        }

        /*
        Configuration index = storage.subset(GraphDatabaseConfiguration.INDEX_NAMESPACE).subset(name);
        index.setProperty(GraphDatabaseConfiguration.INDEX_BACKEND_KEY, indexBackend);
        index.setProperty(GraphDatabaseConfiguration.HOSTNAME_KEY, indexHostname);
        index.setProperty("client-only", indexClientOnly);

        if (indexDirectory != null) {
            String dir = (new File(indexDirectory, name)).getPath();
            index.setProperty(GraphDatabaseConfiguration.STORAGE_DIRECTORY_KEY, dir);
        }
        */

        return configuration;
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
