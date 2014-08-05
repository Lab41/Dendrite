package org.lab41.dendrite.metagraph;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.metagraph.models.UserMetadata;

public class MetaGraphTest extends BaseMetaGraphTest {

    @Test
    public void testGettingSystemGraph() {
        DendriteGraph systemGraph = metaGraph.getSystemGraph();

        // Make sure the system graph isn't returned by default.
        Assert.assertNull(metaGraph.getGraph(systemGraph.getId()));

        // Make sure it is returned when we ask.
        Assert.assertEquals(metaGraph.getGraph(systemGraph.getId(), true), systemGraph);
    }

    @Test
    public void testGettingNormalGraph() {
        // Create a graph.
        MetaGraphTx tx = metaGraph.newTransaction();

        UserMetadata userMetadata = tx.createUser("test");
        Assert.assertNotNull(userMetadata);

        ProjectMetadata projectMetadata = tx.createProject("test", userMetadata);
        Assert.assertNotNull(projectMetadata);

        GraphMetadata graphMetadata = projectMetadata.getCurrentGraph();
        Assert.assertNotNull(graphMetadata);

        tx.commit();

        DendriteGraph graph = metaGraph.getGraph(graphMetadata.getId());
        Assert.assertNotNull(graph);
    }

    @Test
    public void testGetGraphs() {
        // Initially there is 1 system graph and no regular graphs.
        Assert.assertEquals(metaGraph.getGraphs().size(), 0);
        Assert.assertEquals(metaGraph.getGraphs(true).size(), 1);

        // Create a graph.
        MetaGraphTx tx = metaGraph.newTransaction();

        UserMetadata userMetadata = tx.createUser("test");
        tx.createProject("test", userMetadata);
        tx.commit();

        // Now there should now be two graphs.
        Assert.assertEquals(metaGraph.getGraphs().size(), 1);
        Assert.assertEquals(metaGraph.getGraphs(true).size(), 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGettingInvalidVertexType() {
        MetaGraphTx tx = metaGraph.newTransaction();

        UserMetadata userMetadata = tx.createUser("test");
        ProjectMetadata projectMetadata = tx.createProject("test", userMetadata);
        tx.commit();

        tx = metaGraph.newTransaction();
        tx.getJob(projectMetadata.getId());
        tx.rollback();
    }
}
