package org.lab41.dendrite.graph;

import com.thinkaurelius.titan.core.*;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.Parameter;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

public class DendriteGraph implements TitanGraph {

    private Logger logger = LoggerFactory.getLogger(DendriteGraph.class);

    private String id;

    private Configuration configuration;

    private TitanGraph titanGraph;

    private boolean systemGraph = false;

    public DendriteGraph(String id, Configuration configuration) {
        this(id, configuration, false);
    }

    public DendriteGraph(String id, Configuration configuration, boolean systemGraph) {
        this.id = id;
        this.configuration = configuration;
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
        if (titanGraph == null) {
            logger.debug("opening titan graph '" + id + "'");

            titanGraph = TitanFactory.open(configuration);

            logger.debug("finished opening titan graph '" + id + "'");
        }
        return titanGraph;
    }

    @Override
    public Features getFeatures() {
        return getTitanGraph().getFeatures();
    }

    @Override
    public Vertex addVertex(Object id) {
        return getTitanGraph().addVertex(id);
    }

    @Override
    public Vertex getVertex(Object id) {
        return getTitanGraph().getVertex(id);
    }

    @Override
    public void removeVertex(Vertex vertex) {
        getTitanGraph().removeVertex(vertex);

    }

    @Override
    public Iterable<Vertex> getVertices() {
        return getTitanGraph().getVertices();
    }

    @Override
    public Iterable<Vertex> getVertices(String key, Object value) {
        return getTitanGraph().getVertices(key, value);
    }

    @Override
    public Edge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
        return getTitanGraph().addEdge(id, outVertex, inVertex, label);
    }

    @Override
    public Edge getEdge(Object id) {
        return getTitanGraph().getEdge(id);
    }

    @Override
    public void removeEdge(Edge edge) {
        getTitanGraph().removeEdge(edge);

    }

    @Override
    public Iterable<Edge> getEdges() {
        return getTitanGraph().getEdges();
    }

    @Override
    public Iterable<Edge> getEdges(String key, Object value) {
        return getTitanGraph().getEdges();
    }

    @Override
    public <T extends Element> void dropKeyIndex(String key, Class<T> elementClass) {
        getTitanGraph().dropKeyIndex(key, elementClass);
    }

    @Override
    public <T extends Element> void createKeyIndex(String key, Class<T> elementClass, Parameter... indexParameters) {
        getTitanGraph().createKeyIndex(key, elementClass, indexParameters);
    }

    @Override
    public <T extends Element> Set<String> getIndexedKeys(Class<T> elementClass) {
        return getTitanGraph().getIndexedKeys(elementClass);
    }

    @Override
    public TitanTransaction newTransaction() {
        return getTitanGraph().newTransaction();
    }

    @Override
    public TransactionBuilder buildTransaction() {
        return getTitanGraph().buildTransaction();
    }

    @Override
    public void shutdown() throws TitanException{
        getTitanGraph().shutdown();
    }

    @Override
    public KeyMaker makeKey(String name) {
        return getTitanGraph().makeKey(name);
    }

    @Override
    public LabelMaker makeLabel(String name) {
        return getTitanGraph().makeLabel(name);
    }

    @Override
    public <T extends TitanType> Iterable<T> getTypes(Class<T> clazz) {
        return getTitanGraph().getTypes(clazz);
    }

    @Override
    public TitanGraphQuery query() {
        return getTitanGraph().query();
    }

    @Override
    public TitanIndexQuery indexQuery(String indexName, String query) {
        return getTitanGraph().indexQuery(indexName, query);
    }

    @Override
    public TitanMultiVertexQuery multiQuery(TitanVertex... vertices) {
        return getTitanGraph().multiQuery(vertices);
    }

    @Override
    public TitanMultiVertexQuery multiQuery(Collection<TitanVertex> vertices) {
        return getTitanGraph().multiQuery(vertices);
    }

    @Override
    public TitanType getType(String name) {
        return getTitanGraph().getType(name);
    }

    @Override
    public boolean isOpen() {
        return getTitanGraph().isOpen();
    }

    @Override
    public void stopTransaction(Conclusion conclusion) {
        getTitanGraph().stopTransaction(conclusion);
    }

    @Override
    public void commit() {
        getTitanGraph().commit();
    }

    @Override
    public void rollback() {
        getTitanGraph().rollback();
    }
}
