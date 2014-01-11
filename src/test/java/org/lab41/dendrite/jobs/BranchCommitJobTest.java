package org.lab41.dendrite.jobs;

import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.TitanType;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import junit.framework.Assert;
import org.junit.Test;
import org.lab41.dendrite.metagraph.BaseMetaGraphTest;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;

public class BranchCommitJobTest extends BaseMetaGraphTest {

    @Test
    public void test() {
        MetaGraphTx tx = metaGraph.newTransaction();
        ProjectMetadata projectMetadata = tx.createProject("test");
        BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
        GraphMetadata srcGraphMetadata = branchMetadata.getGraph();
        JobMetadata jobMetadata = tx.createJob(projectMetadata);
        tx.commit();

        DendriteGraph srcGraph = metaGraph.getGraph(srcGraphMetadata.getId());

        TitanTransaction titanTx;

        // Create an index.
        titanTx = srcGraph.newTransaction();
        titanTx.makeKey("name").dataType(String.class).make();
        titanTx.makeLabel("friends").make();
        titanTx.commit();

        // Create a trivial graph.
        titanTx = srcGraph.newTransaction();
        Vertex srcJoeVertex = titanTx.addVertex(null);
        srcJoeVertex.setProperty("name", "Joe");
        srcJoeVertex.setProperty("age", 42);

        Vertex srcBobVertex = titanTx.addVertex(null);
        srcBobVertex.setProperty("name", "Bob");
        srcBobVertex.setProperty("age", 50);

        Edge srcEdge = titanTx.addEdge(null, srcJoeVertex, srcBobVertex, "friends");
        titanTx.commit();

        // Snapshot the graph.
        BranchCommitJob branchCommitJob = new BranchCommitJob(metaGraph, branchMetadata.getId(), jobMetadata.getId());

        Assert.assertEquals(branchCommitJob.getSrcGraph(), srcGraph);

        branchCommitJob.run();

        DendriteGraph dstGraph = branchCommitJob.getDstGraph();

        // Make sure the branch pointer was changed.
        tx = metaGraph.newTransaction();

        GraphMetadata dstGraphMetadata = tx.getGraph(dstGraph.getId());
        BranchMetadata updatedBranchMetadata = tx.getBranch(branchMetadata.getId());
        Assert.assertEquals(updatedBranchMetadata.getGraph(), dstGraphMetadata);

        tx.commit();

        // Make sure the indexes got copied.
        TitanType dstType = dstGraph.getType("name");
        Assert.assertNotNull(dstType);
        Assert.assertTrue(dstType instanceof TitanKey);

        TitanKey dstKey = (TitanKey) dstType;
        Assert.assertEquals(dstKey.getName(), "name");
        Assert.assertEquals(dstKey.getDataType(), String.class);

        // Make sure the vertices got copied.
        Vertex dstJoeVertex = dstGraph.getVertices("name", "Joe").iterator().next();
        Assert.assertNotNull(dstJoeVertex);
        Assert.assertEquals(dstJoeVertex.getProperty("name"), "Joe");
        Assert.assertEquals(dstJoeVertex.getProperty("age"), 42);

        Vertex dstBobVertex = dstGraph.getVertices("name", "Bob").iterator().next();
        Assert.assertNotNull(dstBobVertex);
        Assert.assertEquals(dstBobVertex.getProperty("name"), "Bob");
        Assert.assertEquals(dstBobVertex.getProperty("age"), 50);

        Edge dstEdge = dstJoeVertex.getEdges(Direction.BOTH).iterator().next();
        Assert.assertNotNull(dstEdge);
        Assert.assertEquals(dstEdge.getLabel(), "friends");
        Assert.assertEquals(dstEdge.getVertex(Direction.IN), dstJoeVertex);
        Assert.assertEquals(dstEdge.getVertex(Direction.OUT), dstBobVertex);
    }
}
