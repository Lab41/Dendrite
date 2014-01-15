package org.lab41.dendrite.metagraph;

import com.thinkaurelius.titan.core.*;
import com.thinkaurelius.titan.graphdb.transaction.StandardTitanTx;
import com.thinkaurelius.titan.graphdb.types.TypeAttribute;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.Lock;

public class DendriteGraphTx implements TitanTransaction {

    private Logger logger = LoggerFactory.getLogger(DendriteGraphTx.class);

    private Lock tableLock;
    private TitanGraph titanGraph;
    private TitanTransaction tx;

    public DendriteGraphTx(Lock tableLock, TitanGraph titanGraph, TitanTransaction tx) {
        this.tableLock = tableLock;
        this.titanGraph = titanGraph;
        this.tx = tx;
    }

    @Override
    public TitanVertex addVertex() {
        return tx.addVertex();
    }

    @Override
    public TitanVertex addVertex(Long id) {
        return tx.addVertex(id);
    }

    @Override
    public TitanEdge addEdge(TitanVertex outVertex, TitanVertex inVertex, TitanLabel label) {
        return tx.addEdge(outVertex, inVertex, label);
    }

    @Override
    public TitanEdge addEdge(TitanVertex outVertex, TitanVertex inVertex, String label) {
        return tx.addEdge(outVertex, inVertex, label);
    }

    @Override
    public TitanProperty addProperty(TitanVertex vertex, TitanKey key, Object attribute) {
        return tx.addProperty(vertex, key, attribute);
    }

    @Override
    public TitanProperty addProperty(TitanVertex vertex, String key, Object attribute) {
        return tx.addProperty(vertex, key, attribute);
    }

    @Override
    public TitanVertex getVertex(long id) {
        return tx.getVertex(id);
    }

    @Override
    public boolean containsVertex(long vertexid) {
        return tx.containsVertex(vertexid);
    }

    @Override
    public TitanGraphQuery query() {
        return tx.query();
    }

    @Override
    public TitanIndexQuery indexQuery(String indexName, String query) {
        return tx.indexQuery(indexName, query);
    }

    @Override
    public TitanMultiVertexQuery multiQuery(TitanVertex... vertices) {
        return tx.multiQuery(vertices);
    }

    @Override
    public TitanMultiVertexQuery multiQuery(Collection<TitanVertex> vertices) {
        return tx.multiQuery(vertices);
    }

    @Override
    public TitanVertex getVertex(TitanKey key, Object attribute) {
        return tx.getVertex(key, attribute);
    }

    @Override
    public TitanVertex getVertex(String key, Object attribute) {
        return tx.getVertex(key, attribute);
    }

    @Override
    public Iterable<TitanVertex> getVertices(TitanKey key, Object attribute) {
        return tx.getVertices(key, attribute);
    }

    @Override
    public Iterable<TitanEdge> getEdges(TitanKey key, Object attribute) {
        return tx.getEdges(key, attribute);
    }

    @Override
    public boolean containsType(String name) {
        return tx.containsType(name);
    }

    @Override
    public TitanType getType(String name) {
        return tx.getType(name);
    }

    @Override
    public TitanKey getPropertyKey(String name) {
        return tx.getPropertyKey(name);
    }

    @Override
    public TitanLabel getEdgeLabel(String name) {
        return tx.getEdgeLabel(name);
    }

    @Override
    public <T extends TitanType> Iterable<T> getTypes(Class<T> clazz) {
        return tx.getTypes(clazz);
    }

    @Override
    public KeyMaker makeKey(String name) {
        return tx.makeKey(name);
    }

    @Override
    public LabelMaker makeLabel(String name) {
        return tx.makeLabel(name);
    }

    public void makePropertyKey(String key, TypeAttribute.Map definition) {
        ((StandardTitanTx) tx).makePropertyKey(key, definition);
    }

    public void makeEdgeLabel(String key, TypeAttribute.Map definition) {
        ((StandardTitanTx) tx).makeEdgeLabel(key, definition);
    }

    @Override
    public void commit() {
        tx.commit();
        tableLock.unlock();
    }

    @Override
    public void rollback() {
        tx.rollback();
        tableLock.unlock();
    }

    @Override
    public boolean isOpen() {
        return tx.isOpen();
    }

    @Override
    public boolean isClosed() {
        return tx.isClosed();
    }

    @Override
    public boolean hasModifications() {
        return tx.hasModifications();
    }

    @Override
    @Deprecated
    public void stopTransaction(Conclusion conclusion) {
        tx.stopTransaction(conclusion);
        tableLock.unlock();
    }

    @Override
    public void shutdown() {
        tx.shutdown();
        tableLock.unlock();
    }

    @Override
    public Features getFeatures() {
        return tx.getFeatures();
    }

    @Override
    public Vertex addVertex(Object id) {
        return tx.addVertex(id);
    }

    @Override
    public Vertex getVertex(Object id) {
        return tx.getVertex(id);
    }

    @Override
    public void removeVertex(Vertex vertex) {
        tx.removeVertex(vertex);
    }

    @Override
    public Iterable<Vertex> getVertices() {
        return tx.getVertices();
    }

    @Override
    public Iterable<Vertex> getVertices(String key, Object value) {
        return tx.getVertices(key, value);
    }

    @Override
    public Edge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
        return tx.addEdge(id, outVertex, inVertex, label);
    }

    @Override
    public Edge getEdge(Object id) {
        return tx.getEdge(id);
    }

    @Override
    public void removeEdge(Edge edge) {
        tx.removeEdge(edge);
    }

    @Override
    public Iterable<Edge> getEdges() {
        return tx.getEdges();
    }

    @Override
    public Iterable<Edge> getEdges(String key, Object value) {
        return tx.getEdges(key, value);
    }

    @Override
    public <T extends Element> void dropKeyIndex(String key, Class<T> elementClass) {
        tx.dropKeyIndex(key, elementClass);
    }

    @Override
    public <T extends Element> void createKeyIndex(String key, Class<T> elementClass, Parameter... indexParameters) {
        tx.createKeyIndex(key, elementClass, indexParameters);
    }

    @Override
    public <T extends Element> Set<String> getIndexedKeys(Class<T> elementClass) {
        return tx.getIndexedKeys(elementClass);
    }
}
