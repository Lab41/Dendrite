package org.lab41.dendrite.jobs;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BranchCommitJob extends AbstractGraphCommitJob {

    Logger logger = LoggerFactory.getLogger(BranchCommitJob.class);

    public BranchCommitJob(MetaGraph metaGraph,
                           JobMetadata.Id jobId,
                           ProjectMetadata.Id projectId,
                           BranchMetadata.Id branchId,
                           GraphMetadata.Id srcGraphId,
                           GraphMetadata.Id dstGraphId) {
        super(metaGraph, jobId, projectId, branchId, srcGraphId, dstGraphId);
    }

    @Override
    public void copyGraph(DendriteGraph srcGraph, DendriteGraph dstGraph) {
        TitanTransaction srcTx = srcGraph.newTransaction();
        TitanTransaction dstTx = dstGraph.newTransaction();

        snapshotVertices(srcTx, dstTx);
        snapshotEdges(srcTx, dstTx);

        dstTx.commit();
        srcTx.commit();
    }

    private void snapshotVertices(TitanTransaction srcTx, TitanTransaction dstTx) {
        for (Vertex srcVertex: srcTx.getVertices()) {
            Vertex dstVertex = dstTx.addVertex(srcVertex.getId());

            for (String key: srcVertex.getPropertyKeys()) {
                dstVertex.setProperty(key, srcVertex.getProperty(key));
            }
        }
    }

    private void snapshotEdges(TitanTransaction srcTx, TitanTransaction dstTx) {
        for (Edge srcEdge: srcTx.getEdges()) {
            Vertex srcInVertex = srcEdge.getVertex(Direction.IN);
            Vertex srcOutVertex = srcEdge.getVertex(Direction.OUT);

            Vertex dstInVertex = dstTx.getVertex(srcInVertex.getId());
            Vertex dstOutVertex = dstTx.getVertex(srcOutVertex.getId());

            dstTx.addEdge(srcEdge.getId(), dstInVertex, dstOutVertex, srcEdge.getLabel());
        }
    }
}
