package org.lab41.dendrite.metagraph;

import com.thinkaurelius.titan.core.*;
import com.thinkaurelius.titan.graphdb.blueprints.TitanBlueprintsTransaction;
import com.thinkaurelius.titan.graphdb.transaction.StandardTitanTx;
import com.thinkaurelius.titan.graphdb.types.TypeAttribute;
import com.tinkerpop.blueprints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.locks.Lock;

public class DendriteGraphTx extends TitanBlueprintsTransaction {

    private Logger logger = LoggerFactory.getLogger(DendriteGraphTx.class);

    private Lock tableLock;
    private TitanTransaction tx;
    private final String searchIndexId = "vertexId";

    public DendriteGraphTx(Lock tableLock, TitanTransaction tx) {
        this.tableLock = tableLock;
        this.tx = tx;
    }

    @Override
    public TitanVertex addVertex() {
        TitanVertex vertex = tx.addVertex();
        vertex.setProperty(searchIndexId, vertex.getId());
        return vertex;
    }

    @Override
    public TitanVertex addVertex(Long id) {
        TitanVertex vertex = tx.addVertex(id);
        vertex.setProperty(searchIndexId, vertex.getId());
        return vertex;
    }

    @Override
    public TitanEdge addEdge(TitanVertex outVertex, TitanVertex inVertex, TitanLabel label) {
        return tx.addEdge(outVertex, inVertex, label);
    }

    @Override
    public TitanProperty addProperty(TitanVertex vertex, TitanKey key, Object attribute) {
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
    public TitanVertex addVertex(Object id) {
        TitanVertex vertex = (TitanVertex) tx.addVertex(id);
        vertex.setProperty(searchIndexId, vertex.getId());
        return vertex;
    }

    @Override
    public TitanVertex getVertex(Object id) {
        return (TitanVertex) tx.getVertex(id);
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
    public TitanEdge getEdge(Object id) {
        return (TitanEdge) tx.getEdge(id);
    }

    @Override
    public Iterable<Edge> getEdges() {
        return tx.getEdges();
    }
}
