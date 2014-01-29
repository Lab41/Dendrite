package org.lab41.dendrite.metagraph;

import com.thinkaurelius.titan.core.TransactionBuilder;
import com.tinkerpop.frames.FramedGraphFactory;

import java.util.concurrent.locks.Lock;

public class MetaGraphTransactionBuilder {

    DendriteGraphTransactionBuilder transactionBuilder;
    FramedGraphFactory framedGraphFactory;

    public MetaGraphTransactionBuilder(DendriteGraphTransactionBuilder transactionBuilder,
                                       FramedGraphFactory framedGraphFactory) {
        this.transactionBuilder = transactionBuilder;
        this.framedGraphFactory = framedGraphFactory;
    }

    public MetaGraphTransactionBuilder readOnly() {
        transactionBuilder.readOnly();
        return this;
    }

    public MetaGraphTransactionBuilder enableBatchLoading() {
        transactionBuilder.enableBatchLoading();
        return this;
    }

    public MetaGraphTransactionBuilder setCacheSize(int size) {
        transactionBuilder.setCacheSize(size);
        return this;
    }

    public MetaGraphTransactionBuilder checkInternalVertexExistence() {
        transactionBuilder.checkInternalVertexExistence();
        return this;
    }

    public MetaGraphTransactionBuilder setTimestamp(long timestamp) {
        transactionBuilder.setTimestamp(timestamp);
        return this;
    }

    public MetaGraphTransactionBuilder setMetricsPrefix(String prefix) {
        transactionBuilder.setMetricsPrefix(prefix);
        return this;
    }

    public MetaGraphTx start() {
        return new MetaGraphTx(transactionBuilder.start(), framedGraphFactory);
    }
}
