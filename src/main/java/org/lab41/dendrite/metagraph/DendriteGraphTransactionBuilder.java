package org.lab41.dendrite.metagraph;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.TransactionBuilder;

public class DendriteGraphTransactionBuilder implements TransactionBuilder {
    public DendriteGraphTransactionBuilder() {
        throw new Error("unimplemented");
    }

    @Override
    public TransactionBuilder readOnly() {
        return null;
    }

    @Override
    public TransactionBuilder enableBatchLoading() {
        return null;
    }

    @Override
    public TransactionBuilder setCacheSize(int size) {
        return null;
    }

    @Override
    public TransactionBuilder checkInternalVertexExistence() {
        return null;
    }

    @Override
    public TransactionBuilder setTimestamp(long timestamp) {
        return null;
    }

    @Override
    public TransactionBuilder setMetricsPrefix(String prefix) {
        return null;
    }

    @Override
    public TitanTransaction start() {
        return null;
    }
}
