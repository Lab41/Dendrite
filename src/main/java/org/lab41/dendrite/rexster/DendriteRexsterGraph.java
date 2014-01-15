package org.lab41.dendrite.rexster;

import com.thinkaurelius.titan.core.*;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.Parameter;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.DendriteGraphTransactionBuilder;
import org.lab41.dendrite.metagraph.DendriteGraphTx;

import java.util.Collection;
import java.util.Set;

public class DendriteRexsterGraph implements TitanGraph {

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
        switch (conclusion) {
            case SUCCESS:
                commit();
                break;
            case FAILURE:
                rollback();
                break;
            default:
                throw new IllegalArgumentException("Unrecognized conclusion: "+ conclusion);
        }
    }

    @Override
    public Features getFeatures() {
        return graph.getTitanGraph().getFeatures();
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
    public TitanGraphQuery query() {
        return getAutoStartTx().query();
    }

    @Override
    public TitanIndexQuery indexQuery(String indexName, String query) {
        return getAutoStartTx().indexQuery(indexName, query);
    }

    @Override
    public TitanMultiVertexQuery multiQuery(TitanVertex... vertices) {
        return getAutoStartTx().multiQuery(vertices);
    }

    @Override
    public TitanMultiVertexQuery multiQuery(Collection<TitanVertex> vertices) {
        return getAutoStartTx().multiQuery(vertices);
    }

    @Override
    public TitanType getType(String name) {
        return getAutoStartTx().getType(name);
    }

    @Override
    public boolean isOpen() {
        return getAutoStartTx().isOpen();
    }

    @Override
    public TitanTransaction newTransaction() {
        return graph.newTransaction();
    }

    @Override
    public DendriteGraphTransactionBuilder buildTransaction() {
        return graph.buildTransaction();
    }

    @Override
    public void shutdown() {
        getAutoStartTx().shutdown();
    }

    @Override
    public KeyMaker makeKey(String name) {
        return getAutoStartTx().makeKey(name);
    }

    @Override
    public LabelMaker makeLabel(String name) {
        return getAutoStartTx().makeLabel(name);
    }

    @Override
    public <T extends TitanType> Iterable<T> getTypes(Class<T> clazz) {
        return getAutoStartTx().getTypes(clazz);
    }

    @Override
    public void commit() {
        DendriteGraphTx tx = txs.get();
        if (tx != null && tx.isOpen()) {
            getAutoStartTx().commit();
            txs.remove();
        }
    }

    @Override
    public void rollback() {
        DendriteGraphTx tx = txs.get();
        if (tx != null && tx.isOpen()) {
            getAutoStartTx().rollback();
            txs.remove();
        }
    }

    @Override
    public <T extends Element> void dropKeyIndex(String key, Class<T> elementClass) {
        getAutoStartTx().dropKeyIndex(key, elementClass);
    }

    @Override
    public <T extends Element> void createKeyIndex(String key, Class<T> elementClass, Parameter... indexParameters) {
        getAutoStartTx().createKeyIndex(key, elementClass, indexParameters);
    }

    @Override
    public <T extends Element> Set<String> getIndexedKeys(Class<T> elementClass) {
        return getAutoStartTx().getIndexedKeys(elementClass);
    }
}
