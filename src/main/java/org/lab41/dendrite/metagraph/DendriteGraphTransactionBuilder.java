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
    public TransactionBuilder readOnly() {
        return transactionBuilder.readOnly();
    }

    @Override
    public TransactionBuilder enableBatchLoading() {
        return transactionBuilder.enableBatchLoading();
    }

    @Override
    public TransactionBuilder setCacheSize(int size) {
        return transactionBuilder.setCacheSize(size);
    }

    @Override
    public TransactionBuilder checkInternalVertexExistence() {
        return transactionBuilder.checkInternalVertexExistence();
    }

    @Override
    public TransactionBuilder setTimestamp(long timestamp) {
        return transactionBuilder.setTimestamp(timestamp);
    }

    @Override
    public TransactionBuilder setMetricsPrefix(String prefix) {
        return transactionBuilder.setMetricsPrefix(prefix);
    }

    @Override
    public DendriteGraphTx start() {
        return new DendriteGraphTx(lock, transactionBuilder.start());
    }
}
