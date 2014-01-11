package org.lab41.dendrite.jobs;

import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.core.TitanLabel;
import com.thinkaurelius.titan.core.TitanProperty;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.graphdb.transaction.StandardTitanTx;
import com.thinkaurelius.titan.graphdb.types.TypeAttribute;
import com.thinkaurelius.titan.graphdb.types.system.SystemKey;
import com.thinkaurelius.titan.graphdb.types.vertices.TitanKeyVertex;
import com.thinkaurelius.titan.graphdb.types.vertices.TitanLabelVertex;
import com.thinkaurelius.titan.graphdb.types.vertices.TitanTypeVertex;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BranchCommitJob extends AbstractJob implements Runnable {

    Logger logger = LoggerFactory.getLogger(BranchCommitJob.class);

    String branchId;
    DendriteGraph srcGraph;
    DendriteGraph dstGraph;

    public BranchCommitJob(MetaGraph metaGraph, String branchId, String jobId) {
        super(metaGraph, jobId);

        MetaGraphTx tx = metaGraph.newTransaction();

        BranchMetadata branchMetadata = tx.getBranch(branchId);
        GraphMetadata srcGraphMetadata = branchMetadata.getGraph();
        GraphMetadata dstGraphMetadata = tx.createGraph(srcGraphMetadata);
        tx.commit();

        this.branchId = branchId;
        this.srcGraph = metaGraph.getGraph(srcGraphMetadata.getId());
        this.dstGraph = metaGraph.getGraph(dstGraphMetadata.getId());
    }

    public DendriteGraph getSrcGraph() {
        return srcGraph;
    }

    public DendriteGraph getDstGraph() {
        return dstGraph;
    }

    @Override
    public void run() {
        logger.debug("Starting commit on "
                + srcGraph.getId()
                + " to "
                + dstGraph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "commit-graph");
        setJobState(jobId, JobMetadata.RUNNING);

        createIndices(srcGraph, dstGraph);

        TitanTransaction srcTx = srcGraph.newTransaction();
        TitanTransaction dstTx = dstGraph.newTransaction();

        snapshotVertices(srcTx, dstTx);
        snapshotEdges(srcTx, dstTx);

        dstTx.commit();
        srcTx.commit();

        setJobState(jobId, JobMetadata.DONE);

        // Update the branch to point the new graph.
        MetaGraphTx tx = metaGraph.newTransaction();
        BranchMetadata branchMetadata = tx.getBranch(branchId);
        branchMetadata.setGraph(tx.getGraph(dstGraph.getId()));
        tx.commit();

        logger.debug("snapshotGraph: finished job: " + jobId);
    }

    private void createIndices(DendriteGraph srcGraph, DendriteGraph dstGraph) {
        // This is very much a hack, but unfortunately Titan does not yet expose a proper way to copy indices from
        // one graph to another.

        TitanTransaction srcTx = srcGraph.newTransaction();
        StandardTitanTx dstTx = (StandardTitanTx) dstGraph.newTransaction();

        for(TitanKey titanKey: srcTx.getTypes(TitanKey.class)) {
            if (titanKey instanceof TitanKeyVertex) {
                TitanKeyVertex keyVertex = (TitanKeyVertex) titanKey;
                TypeAttribute.Map definition = getDefinition(keyVertex);
                dstTx.makePropertyKey(keyVertex.getName(), definition);
            }
        }

        for(TitanLabel titanLabel: srcTx.getTypes(TitanLabel.class)) {
            TitanLabelVertex keyVertex = (TitanLabelVertex) titanLabel;
            TypeAttribute.Map definition = getDefinition(keyVertex);
            dstTx.makeEdgeLabel(keyVertex.getName(), definition);
        }

        dstTx.commit();
        srcTx.commit();
    }

    private TypeAttribute.Map getDefinition(TitanTypeVertex vertex) {
        TypeAttribute.Map definition = new TypeAttribute.Map();

        for (TitanProperty p: vertex.query().includeHidden().type(SystemKey.TypeDefinition).properties()) {
            definition.add(p.getValue(TypeAttribute.class));
        }

        return definition;
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
