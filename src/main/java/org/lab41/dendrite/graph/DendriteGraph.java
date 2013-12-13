package org.lab41.dendrite.graph;

import com.thinkaurelius.titan.core.*;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import org.apache.commons.configuration.Configuration;

import java.util.Collection;
import java.util.Set;

public class DendriteGraph implements TitanGraph {

    private String id;

    private Configuration configuration;

    private TitanGraph titanGraph;

    private RexsterApplicationGraph rexsterGraph;

    private boolean systemGraph = false;

    public DendriteGraph(String id,
                         Configuration configuration,
                         TitanGraph titanGraph,
                         RexsterApplicationGraph rexsterGraph) {
        this(id, configuration, titanGraph, rexsterGraph, false);
    }

    public DendriteGraph(String id,
                         Configuration configuration,
                         TitanGraph titanGraph,
                         RexsterApplicationGraph rexsterGraph,
                         boolean systemGraph) {
        this.id = id;
        this.configuration = configuration;
        this.titanGraph = titanGraph;
        this.rexsterGraph = rexsterGraph;
        this.systemGraph = systemGraph;
    }

    public String getId() {
        return id;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public boolean isSystemGraph() {
        return systemGraph;
    }

    public TitanGraph getTitanGraph() {
        return titanGraph;
    }

    public RexsterApplicationGraph getRexsterGraph() {
        return rexsterGraph;
    }

    @Override
    public Features getFeatures() {
        return titanGraph.getFeatures();
    }

    @Override
    public Vertex addVertex(Object id) {
        return titanGraph.addVertex(id);
    }

    @Override
    public Vertex getVertex(Object id) {
        return titanGraph.getVertex(id);
    }

    @Override
    public void removeVertex(Vertex vertex) {
        titanGraph.removeVertex(vertex);

    }

    @Override
    public Iterable<Vertex> getVertices() {
        return titanGraph.getVertices();
    }

    @Override
    public Iterable<Vertex> getVertices(String key, Object value) {
        return titanGraph.getVertices(key, value);
    }

    @Override
    public Edge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
        return titanGraph.addEdge(id, outVertex, inVertex, label);
    }

    @Override
    public Edge getEdge(Object id) {
        return titanGraph.getEdge(id);
    }

    @Override
    public void removeEdge(Edge edge) {
        titanGraph.removeEdge(edge);

    }

    @Override
    public Iterable<Edge> getEdges() {
        return titanGraph.getEdges();
    }

    @Override
    public Iterable<Edge> getEdges(String key, Object value) {
        return titanGraph.getEdges();
    }

    @Override
    public <T extends Element> void dropKeyIndex(String key, Class<T> elementClass) {
        titanGraph.dropKeyIndex(key, elementClass);
    }

    @Override
    public <T extends Element> void createKeyIndex(String key, Class<T> elementClass, Parameter... indexParameters) {
        titanGraph.createKeyIndex(key, elementClass, indexParameters);
    }

    @Override
    public <T extends Element> Set<String> getIndexedKeys(Class<T> elementClass) {
        return titanGraph.getIndexedKeys(elementClass);
    }

    @Override
    public TitanTransaction newTransaction() {
        return titanGraph.newTransaction();
    }

    @Override
    public TransactionBuilder buildTransaction() {
        return titanGraph.buildTransaction();
    }

    @Override
    public void shutdown() throws TitanException{
        titanGraph.shutdown();
    }

    @Override
    public KeyMaker makeKey(String name) {
        return titanGraph.makeKey(name);
    }

    @Override
    public LabelMaker makeLabel(String name) {
        return titanGraph.makeLabel(name);
    }

    @Override
    public <T extends TitanType> Iterable<T> getTypes(Class<T> clazz) {
        return titanGraph.getTypes(clazz);
    }

    @Override
    public TitanGraphQuery query() {
        return titanGraph.query();
    }

    @Override
    public TitanIndexQuery indexQuery(String indexName, String query) {
        return titanGraph.indexQuery(indexName, query);
    }

    @Override
    public TitanMultiVertexQuery multiQuery(TitanVertex... vertices) {
        return titanGraph.multiQuery(vertices);
    }

    @Override
    public TitanMultiVertexQuery multiQuery(Collection<TitanVertex> vertices) {
        return titanGraph.multiQuery(vertices);
    }

    @Override
    public TitanType getType(String name) {
        return titanGraph.getType(name);
    }

    @Override
    public boolean isOpen() {
        return titanGraph.isOpen();
    }

    @Override
    public void stopTransaction(Conclusion conclusion) {
        titanGraph.stopTransaction(conclusion);
    }

    @Override
    public void commit() {
        titanGraph.commit();
    }

    @Override
    public void rollback() {
        titanGraph.rollback();
    }
}
