package org.lab41.dendrite.metagraph;

import com.thinkaurelius.titan.core.*;
import com.thinkaurelius.titan.graphdb.blueprints.TitanBlueprintsGraph;
import com.tinkerpop.blueprints.Features;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DendriteGraph extends TitanBlueprintsGraph {

    private Logger logger = LoggerFactory.getLogger(DendriteGraph.class);

    /**
     * The graph id.
     */
    private String id;

    /**
     * The graph properties.
     */
    private Properties properties;

    /**
     * Whether or not the graph is read-only.
     */
    private boolean readOnly;

    /**
     * The wrapped graph.
     */
    private TitanGraph titanGraph;

    /**
     * The wrapped graph's ElasticSearch node.
     */
    private Node elasticSearchNode;

    /**
     * The wrapped graph's ElasticSearch client.
     */
    private Client elasticSearchClient;

    /**
     * This read-write lock is used when we need to prevent table modifications. For example, when we are doing a graph
     * snapshot or when shutting down.
     */
    private ReadWriteLock tableLock = new ReentrantReadWriteLock();

    /**
     * Create an dendrite graph instance.
     *
     * @param id the graph id.
     * @param properties the graph properties.
     */
    public DendriteGraph(String id, Properties properties) {
        this.id = id;
        this.properties = properties;
        this.titanGraph = TitanFactory.open(getConfiguration());

        // Make sure the vertexId is indexed
        String backend = properties.getProperty("storage.index.search.backend", null);
        if (backend != null && backend.equals("elasticsearch")) {
            TitanTransaction tx = titanGraph.newTransaction();
            if (tx.getType(DendriteGraphTx.VERTEX_ID_KEY) == null) {
                tx.makeKey(DendriteGraphTx.VERTEX_ID_KEY)
                        .dataType(String.class)
                        .indexed("search", Vertex.class)
                        .make();
            }
            tx.commit();
        }
    }

    /**
     * Get the graph id.
     *
     * @return graph id.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the graph properties.
     *
     * @return the graph properties.
     */
    public Properties getProperties() {
        return new Properties(properties);
    }

    /**
     * Get the graph properties in a configuration format.
     *
     * @return the graph configuration.
     */
    public Configuration getConfiguration() {
        return new MapConfiguration(properties);
    }

    /**
     * Get the wrapped titan graph.
     *
     * @return the wrapped titan graph.
     */
    public TitanGraph getTitanGraph() {
        return titanGraph;
    }

    /**
     * Get the wrapped titan graph's ElasticSearch client.
     *
     * @return the ElasticSearch client.
     */
    public Client getElasticSearchClient() {
        if (elasticSearchClient != null) {
            return elasticSearchClient;
        }

        String backend = properties.getProperty("storage.index.search.backend", null);
        if (backend != null && backend.equals("elasticsearch")) {
            connectToElasticSearch();
        }

        return elasticSearchClient;
    }

    /**
     * Get the wrapped titan graph's ElasticSearch index name.
     *
     * @return the index name.
     */
    public String getIndexName() {
        return properties.getProperty("storage.index.search.index-name", null);
    }

    /**
     * Returns if the table is in read-only mode.
     *
     * @return if the table is read only.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Set if the table should be in read only mode or not.
     *
     * @param readOnly whether or not to set the table in read-only mode.
     */
    public void setReadOnly(boolean readOnly) {
        Lock lock = tableLock.writeLock();
        lock.lock();

        try {
            this.readOnly = readOnly;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isOpen() {
        return titanGraph.isOpen();
    }

    @Override
    public Features getFeatures() {
        return titanGraph.getFeatures();
    }

    /**
     * Open a new thread-independent transaction.
     *
     * @return a transaction.
     */
    @Override
    public DendriteGraphTx newTransaction() {
        DendriteGraphTransactionBuilder transactionBuilder = buildTransaction();

        if (readOnly) {
            transactionBuilder.readOnly();
        }

        return transactionBuilder.start();
    }

    @Override
    public TitanTransaction newThreadBoundTransaction() {
        return newTransaction();
    }

    @Override
    public DendriteGraphTransactionBuilder buildTransaction() {
        Lock lock = tableLock.readLock();
        lock.lock();

        try {
            TransactionBuilder transactionBuilder = titanGraph.buildTransaction();

            return new DendriteGraphTransactionBuilder(lock, transactionBuilder);
        } catch (Exception e) {
            lock.unlock();
            throw e;
        }
    }

    @Override
    public void shutdown() throws TitanException {
        Lock lock = tableLock.writeLock();
        lock.lock();

        try {
            if (elasticSearchClient != null) {
                elasticSearchClient.close();
            }

            if (elasticSearchNode != null) {
                elasticSearchNode.close();
            }

            titanGraph.shutdown();
        } finally {
            lock.unlock();
        }
    }

    synchronized private void connectToElasticSearch() {
        if (!properties.getProperty("storage.index.search.backend", null).equals("elasticsearch")) {
            return;
        }

        ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder();

        String clusterName = properties.getProperty("storage.index.search.cluster-name", null);
        if (clusterName != null) {
            builder.put("cluster.name", clusterName);
        }

        String sniff = properties.getProperty("storage.index.search.sniff", null);
        if (sniff != null) {
            builder.put("client.transport.sniff", sniff);
        }

        Settings settings = builder.build();

        if (properties.getProperty("storage.index.search.local-mode").equals("true")) {
            elasticSearchNode = NodeBuilder.nodeBuilder()
                    .data(false)
                    .local(true)
                    .node();

            elasticSearchClient = elasticSearchNode.client();
        } else {
            TransportClient client = new TransportClient(settings);
            elasticSearchClient = client;

            for (String hostname: properties.getProperty("storage.index.search.hostname", "localhost").split(",")) {
                String[] parts = hostname.split(":");
                int port = 9300;
                if (parts.length == 2) {
                    port = Integer.parseInt(parts[1]);
                }
                client.addTransportAddress(new InetSocketTransportAddress(parts[0], port));
            }
        }

        elasticSearchClient.admin().cluster().prepareHealth()
                .setWaitForYellowStatus()
                .execute().actionGet();
    }
}
