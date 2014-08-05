package org.lab41.dendrite.metagraph;

import com.tinkerpop.blueprints.Vertex;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.metagraph.models.UserMetadata;

public class DendriteGraphTest extends BaseMetaGraphTest {

    DendriteGraph graph;

    @Before
    public void setUp() {
        super.setUp();

        MetaGraphTx tx = metaGraph.newTransaction();
        UserMetadata userMetadata = tx.createUser("test");
        ProjectMetadata projectMetadata = tx.createProject("test", userMetadata);
        GraphMetadata graphMetadata = projectMetadata.getCurrentGraph();
        tx.commit();

        graph = metaGraph.getGraph(graphMetadata.getId());
    }

    @After
    public void tearDown() {
        graph.shutdown();
        graph = null;
        super.tearDown();
    }

    @Test
    public void testGraphShouldCreateGraph() {
        DendriteGraphTx tx = graph.newTransaction();
        Vertex v1 = tx.addVertex(null);
        Vertex v2 = tx.addVertex(null);
        tx.addEdge(null, v1, v2, "edge");
        tx.commit();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGraphShouldThrowWhenWritingInReadOnlyMode() {
        DendriteGraphTx tx = graph.newTransaction();
        Vertex v1 = tx.addVertex(null);
        Vertex v2 = tx.addVertex(null);
        tx.addEdge(null, v1, v2, "edge");
        tx.commit();

        graph.setReadOnly(true);
        tx = graph.newTransaction();
        try {
            tx.addVertex(null);
        } finally {
            tx.rollback();
        }
    }

    @Test
    public void testGraphShouldWorkWhenReadingInReadOnlyMode() {
        DendriteGraphTx tx = graph.newTransaction();
        Vertex v1 = tx.addVertex(null);
        Vertex v2 = tx.addVertex(null);
        tx.addEdge(null, v1, v2, "edge");
        tx.commit();

        graph.setReadOnly(true);
        tx = graph.newTransaction();
        Vertex v3 = tx.getVertex(v1.getId());
        Assert.assertEquals(v1, v3);
        tx.commit();
    }
}
