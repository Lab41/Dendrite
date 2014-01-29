package org.lab41.dendrite.metagraph;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.TransactionBuilder;

import java.util.concurrent.locks.Lock;

public class DendriteGraphTransactionBuilder implements TransactionBuilder {

    Lock lock;
    TransactionBuilder transactionBuilder;

    public DendriteGraphTransactionBuilder(Lock lock, TransactionBuilder transactionBuilder) {
        this.lock = lock;
        this.transactionBuilder = transactionBuilder;
    }

    @Override
    public DendriteGraphTransactionBuilder readOnly() {
        transactionBuilder.readOnly();
        return this;
    }

    @Override
    public DendriteGraphTransactionBuilder enableBatchLoading() {
        transactionBuilder.enableBatchLoading();
        return this;
    }

    @Override
    public DendriteGraphTransactionBuilder setCacheSize(int size) {
        transactionBuilder.setCacheSize(size);
        return this;
    }

    @Override
    public DendriteGraphTransactionBuilder checkInternalVertexExistence() {
        transactionBuilder.checkInternalVertexExistence();
        return this;
    }

    @Override
    public DendriteGraphTransactionBuilder setTimestamp(long timestamp) {
        transactionBuilder.setTimestamp(timestamp);
        return this;
    }

    @Override
    public DendriteGraphTransactionBuilder setMetricsPrefix(String prefix) {
        transactionBuilder.setMetricsPrefix(prefix);
        return this;
    }

    @Override
    public DendriteGraphTx start() {
        return new DendriteGraphTx(lock, transactionBuilder.start());
    }
}
