package org.lab41.dendrite.metagraph;

import com.thinkaurelius.titan.core.*;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.Parameter;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.DendriteGraphTransactionBuilder;
import org.lab41.dendrite.metagraph.DendriteGraphTx;

import java.util.Collection;
import java.util.Set;

public class DendriteGraphBatchWrapper implements TitanGraph {

    DendriteGraph graph;

    public DendriteGraphBatchWrapper(DendriteGraph graph) {
        this.graph = graph;
    }

    @Override
    public void commit() {
        graph.commit();
    }

    @Override
    public void rollback() {
        graph.rollback();
    }

    @Override
    @Deprecated
    public void stopTransaction(Conclusion conclusion) {
        graph.stopTransaction(conclusion);
    }

    public TitanTransaction getCurrentThreadTx() {
        return graph.getCurrentThreadTx();
    }

    @Override
    public String toString() {
        return graph.toString();
    }

    @Override
    public <T extends TitanType> Iterable<T> getTypes(Class<T> clazz) {
        return graph.getTypes(clazz);
    }

    @Override
    public <T extends Element> void dropKeyIndex(String key, Class<T> elementClass) {
        graph.dropKeyIndex(key, elementClass);
    }

    @Override
    public <T extends Element> void createKeyIndex(String key, Class<T> elementClass, Parameter... indexParameters) {
        graph.createKeyIndex(key, elementClass, indexParameters);
    }

    @Override
    public <T extends Element> Set<String> getIndexedKeys(Class<T> elementClass) {
        return graph.getIndexedKeys(elementClass);
    }

    @Override
    public Features getFeatures() {
        Features features = graph.getFeatures().copyFeatures();
        features.ignoresSuppliedIds = false;
        return features;
    }

    @Override
    public TitanVertex addVertex(Object id) {
        return graph.addVertex(id);
    }

    @Override
    public TitanVertex getVertex(Object id) {
        return graph.getVertex(id);
    }

    @Override
    public void removeVertex(Vertex vertex) {
        graph.removeVertex(vertex);
    }

    @Override
    public Iterable<Vertex> getVertices() {
        return graph.getVertices();
    }

    @Override
    public DendriteGraphTx newTransaction() {
        return graph.newTransaction();
    }

    @Override
    public DendriteGraphTransactionBuilder buildTransaction() {
        return graph.buildTransaction();
    }

    @Override
    public void shutdown() throws TitanException {
        graph.shutdown();
    }

    @Override
    public KeyMaker makeKey(String name) {
        return graph.makeKey(name);
    }

    @Override
    public LabelMaker makeLabel(String name) {
        return graph.makeLabel(name);
    }

    @Override
    public TitanGraphQuery query() {
        return graph.query();
    }

    @Override
    public TitanIndexQuery indexQuery(String indexName, String query) {
        return graph.indexQuery(indexName, query);
    }

    @Override
    public TitanMultiVertexQuery multiQuery(TitanVertex... vertices) {
        return graph.multiQuery(vertices);
    }

    @Override
    public TitanMultiVertexQuery multiQuery(Collection<TitanVertex> vertices) {
        return graph.multiQuery(vertices);
    }

    @Override
    public TitanType getType(String name) {
        return graph.getType(name);
    }

    @Override
    public boolean isOpen() {
        return graph.isOpen();
    }

    @Override
    public Iterable<Vertex> getVertices(String key, Object value) {
        return graph.getVertices(key, value);
    }

    @Override
    public TitanEdge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
        return graph.addEdge(id, outVertex, inVertex, label);
    }

    @Override
    public TitanEdge getEdge(Object id) {
        return graph.getEdge(id);
    }

    @Override
    public void removeEdge(Edge edge) {
        graph.removeEdge(edge);
    }

    @Override
    public Iterable<Edge> getEdges() {
        return graph.getEdges();
    }

    @Override
    public Iterable<Edge> getEdges(String key, Object value) {
        return graph.getEdges(key, value);
    }
}
