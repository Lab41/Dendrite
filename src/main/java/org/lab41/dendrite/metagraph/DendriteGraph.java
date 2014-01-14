package org.lab41.dendrite.metagraph;

import com.thinkaurelius.titan.core.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DendriteGraph {

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

    /**
     * Open a new thread-independent transaction.
     *
     * @return a transaction.
     */
    public DendriteGraphTx newTransaction() {
        Lock lock = tableLock.readLock();

        lock.lock();

        try {
            TransactionBuilder transactionBuilder = titanGraph.buildTransaction();

            if (readOnly) {
                transactionBuilder.readOnly();
            }

            return new DendriteGraphTx(lock, transactionBuilder.start());
        } catch (Exception e) {
            lock.unlock();
            throw e;
        }
    }

    public void shutdown() throws TitanException {
        Lock lock = tableLock.writeLock();
        lock.lock();

        try {
            titanGraph.shutdown();
        } finally {
            lock.unlock();
        }
    }
}
