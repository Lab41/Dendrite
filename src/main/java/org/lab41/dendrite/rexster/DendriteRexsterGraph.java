package org.lab41.dendrite.rexster;

import com.tinkerpop.blueprints.*;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.DendriteGraphTx;

public class DendriteRexsterGraph implements TransactionalGraph {

    private DendriteGraph graph;
    private ThreadLocal<DendriteGraphTx> txs = new ThreadLocal<DendriteGraphTx>() {
        protected DendriteGraphTx initialValue() {
            return null;
        }
    };

    public DendriteRexsterGraph(DendriteGraph graph) {
        this.graph = graph;
    }

    private DendriteGraphTx getAutoStartTx() {
        DendriteGraphTx tx = txs.get();
        if (tx == null) {
            tx = graph.newTransaction();
            txs.set(tx);
        }

        return tx;
    }

    @Override
    public void stopTransaction(Conclusion conclusion) {
        getAutoStartTx().stopTransaction(conclusion);
    }

    @Override
    public Features getFeatures() {
        return getAutoStartTx().getFeatures();
    }

    @Override
    public Vertex addVertex(Object id) {
        return getAutoStartTx().addVertex(id);
    }

    @Override
    public Vertex getVertex(Object id) {
        return getAutoStartTx().getVertex(id);
    }

    @Override
    public void removeVertex(Vertex vertex) {
        getAutoStartTx().removeVertex(vertex);
    }

    @Override
    public Iterable<Vertex> getVertices() {
        return getAutoStartTx().getVertices();
    }

    @Override
    public Iterable<Vertex> getVertices(String key, Object value) {
        return getAutoStartTx().getVertices(key, value);
    }

    @Override
    public Edge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
        return getAutoStartTx().addEdge(id, outVertex, inVertex, label);
    }

    @Override
    public Edge getEdge(Object id) {
        return getAutoStartTx().getEdge(id);
    }

    @Override
    public void removeEdge(Edge edge) {
        getAutoStartTx().removeEdge(edge);
    }

    @Override
    public Iterable<Edge> getEdges() {
        return getAutoStartTx().getEdges();
    }

    @Override
    public Iterable<Edge> getEdges(String key, Object value) {
        return getAutoStartTx().getEdges(key, value);
    }

    @Override
    public GraphQuery query() {
        return getAutoStartTx().query();
    }

    @Override
    public void shutdown() {
        getAutoStartTx().shutdown();
    }

    @Override
    public void commit() {
        getAutoStartTx().commit();
    }

    @Override
    public void rollback() {
        getAutoStartTx().rollback();
    }
}
