package org.lab41.dendrite.jobs;

import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.TitanType;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import junit.framework.Assert;
import org.junit.Test;
import org.lab41.dendrite.metagraph.*;
import org.lab41.dendrite.metagraph.models.*;

public class BranchCommitJobTest extends BaseMetaGraphTest {

    @Test
    public void test() {
        // Create the project.
        MetaGraphTx metaGraphTx = metaGraph.newTransaction();

        UserMetadata userMetadata = metaGraphTx.createUser("test");
        ProjectMetadata projectMetadata = metaGraphTx.createProject("test", userMetadata);
        BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
        GraphMetadata srcGraphMetadata = branchMetadata.getGraph();
        GraphMetadata dstGraphMetadata = metaGraphTx.createGraph(srcGraphMetadata);
        JobMetadata jobMetadata = metaGraphTx.createJob(projectMetadata);

        metaGraphTx.commit();

        // Create the source graph.
        DendriteGraph srcGraph = metaGraph.getGraph(srcGraphMetadata.getId());

        DendriteGraphTx srcTx;

        // Create an index.
        srcTx = srcGraph.newTransaction();
        srcTx.makeKey("name").dataType(String.class).make();
        srcTx.makeLabel("friends").make();
        srcTx.commit();

        // Create a trivial graph.
        srcTx = srcGraph.newTransaction();
        Vertex srcJoeVertex = srcTx.addVertex(null);
        srcJoeVertex.setProperty("name", "Joe");
        srcJoeVertex.setProperty("age", 42);

        Vertex srcBobVertex = srcTx.addVertex(null);
        srcBobVertex.setProperty("name", "Bob");
        srcBobVertex.setProperty("age", 50);

        srcTx.addEdge(null, srcJoeVertex, srcBobVertex, "friends");
        srcTx.commit();

        // Snapshot the graph.
        BranchCommitJob branchCommitJob = new BranchCommitJob(
                metaGraph,
                jobMetadata.getId(),
                projectMetadata.getId(),
                branchMetadata.getId(),
                srcGraphMetadata.getId(),
                dstGraphMetadata.getId());

        GraphMetadata.Id srcGraphId = branchCommitJob.getSrcGraphId();
        GraphMetadata.Id dstGraphId = branchCommitJob.getDstGraphId();

        Assert.assertEquals(srcGraphId, srcGraph.getId());

        branchCommitJob.run();

        DendriteGraph dstGraph = metaGraph.getGraph(dstGraphId);
        Assert.assertNotNull(dstGraph);

        // Make sure the branch pointer was changed.
        metaGraphTx = metaGraph.newTransaction();

        BranchMetadata updatedBranchMetadata = metaGraphTx.getBranch(branchMetadata.getId());
        Assert.assertEquals(updatedBranchMetadata.getGraph(), dstGraphMetadata);
        metaGraphTx.commit();

        // Make sure the indexes got copied.
        DendriteGraphTx dstTx = dstGraph.newTransaction();

        TitanType dstType = dstTx.getType("name");
        Assert.assertNotNull(dstType);
        Assert.assertTrue(dstType instanceof TitanKey);

        TitanKey dstKey = (TitanKey) dstType;
        Assert.assertEquals(dstKey.getName(), "name");
        Assert.assertEquals(dstKey.getDataType(), String.class);

        // Make sure the vertices got copied.
        Vertex dstJoeVertex = dstTx.getVertices("name", "Joe").iterator().next();
        Assert.assertNotNull(dstJoeVertex);
        Assert.assertEquals(dstJoeVertex.getProperty("name"), "Joe");
        Assert.assertEquals(dstJoeVertex.getProperty("age"), 42);

        Vertex dstBobVertex = dstTx.getVertices("name", "Bob").iterator().next();
        Assert.assertNotNull(dstBobVertex);
        Assert.assertEquals(dstBobVertex.getProperty("name"), "Bob");
        Assert.assertEquals(dstBobVertex.getProperty("age"), 50);

        Edge dstEdge = dstJoeVertex.getEdges(Direction.BOTH).iterator().next();
        Assert.assertNotNull(dstEdge);
        Assert.assertEquals(dstEdge.getLabel(), "friends");
        Assert.assertEquals(dstEdge.getVertex(Direction.IN), dstJoeVertex);
        Assert.assertEquals(dstEdge.getVertex(Direction.OUT), dstBobVertex);

       dstTx.rollback();
    }
}
