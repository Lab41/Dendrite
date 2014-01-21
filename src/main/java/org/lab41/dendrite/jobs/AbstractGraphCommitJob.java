package org.lab41.dendrite.jobs;

import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.core.TitanLabel;
import com.thinkaurelius.titan.core.TitanProperty;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.graphdb.types.TypeAttribute;
import com.thinkaurelius.titan.graphdb.types.system.SystemKey;
import com.thinkaurelius.titan.graphdb.types.vertices.TitanKeyVertex;
import com.thinkaurelius.titan.graphdb.types.vertices.TitanLabelVertex;
import com.thinkaurelius.titan.graphdb.types.vertices.TitanTypeVertex;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.DendriteGraphTx;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGraphCommitJob extends AbstractJob implements Runnable {

    private Logger logger = LoggerFactory.getLogger(AbstractGraphCommitJob.class);

    String branchId;
    String srcGraphId;
    String dstGraphId;

    public AbstractGraphCommitJob(MetaGraph metaGraph, String jobId, String branchId) {
        super(metaGraph, jobId);

        MetaGraphTx tx = metaGraph.newTransaction();

        BranchMetadata branchMetadata = tx.getBranch(branchId);
        GraphMetadata srcGraphMetadata = branchMetadata.getGraph();
        GraphMetadata dstGraphMetadata = tx.createGraph(srcGraphMetadata);

        this.branchId = branchId;
        this.srcGraphId = srcGraphMetadata.getId();
        this.dstGraphId = dstGraphMetadata.getId();

        tx.commit();
    }

    public String getBranchId() {
        return branchId;
    }

    public String getSrcGraphId() {
        return srcGraphId;
    }

    public String getDstGraphId() {
        return dstGraphId;
    }

    @Override
    public void run() {
        logger.debug("Starting commit on "
                + srcGraphId
                + " to "
                + dstGraphId
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        DendriteGraph srcGraph = metaGraph.getGraph(srcGraphId);
        DendriteGraph dstGraph = metaGraph.getGraph(dstGraphId);

        setJobState(jobId, JobMetadata.RUNNING);

        copyIndices(srcGraph, dstGraph);
        copyGraph(srcGraph, dstGraph);

        setJobState(jobId, JobMetadata.DONE);

        // Update the branch to point the new graph.
        MetaGraphTx tx = metaGraph.newTransaction();
        BranchMetadata branchMetadata = tx.getBranch(branchId);
        branchMetadata.setGraph(tx.getGraph(dstGraph.getId()));
        tx.commit();

        logger.debug("snapshotGraph: finished job: " + jobId);
    }

    protected void copyIndices(DendriteGraph srcGraph, DendriteGraph dstGraph) {
        // This is very much a hack, but unfortunately Titan does not yet expose a proper way to copy indices from
        // one graph to another.

        TitanTransaction srcTx = srcGraph.newTransaction();
        DendriteGraphTx dstTx = dstGraph.newTransaction();

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

    protected abstract void copyGraph(DendriteGraph srcGraph, DendriteGraph dstGraph);

    protected TypeAttribute.Map getDefinition(TitanTypeVertex vertex) {
        TypeAttribute.Map definition = new TypeAttribute.Map();

        for (TitanProperty p: vertex.query().includeHidden().type(SystemKey.TypeDefinition).properties()) {
            definition.add(p.getValue(TypeAttribute.class));
        }

        return definition;
    }
}
