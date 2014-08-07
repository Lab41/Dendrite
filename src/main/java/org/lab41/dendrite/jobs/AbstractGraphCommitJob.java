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
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGraphCommitJob extends AbstractJob implements Runnable {

    private Logger logger = LoggerFactory.getLogger(AbstractGraphCommitJob.class);

    ProjectMetadata.Id projectId;
    BranchMetadata.Id branchId;
    GraphMetadata.Id srcGraphId;
    GraphMetadata.Id dstGraphId;

    protected AbstractGraphCommitJob(MetaGraph metaGraph,
                                     JobMetadata.Id jobId,
                                     ProjectMetadata.Id projectId,
                                     BranchMetadata.Id branchId,
                                     GraphMetadata.Id srcGraphId,
                                     GraphMetadata.Id dstGraphId) {
        super(metaGraph, jobId);

        this.projectId = projectId;
        this.branchId = branchId;
        this.srcGraphId = srcGraphId;
        this.dstGraphId = dstGraphId;
    }

    protected AbstractGraphCommitJob(MetaGraph metaGraph,
                                     JobMetadata.Id jobId,
                                     ProjectMetadata.Id projectId,
                                     BranchMetadata.Id branchId) {
        super(metaGraph, jobId);

        MetaGraphTx tx = metaGraph.newTransaction();

        BranchMetadata branchMetadata = tx.getBranch(branchId);
        GraphMetadata srcGraphMetadata = branchMetadata.getGraph();
        GraphMetadata dstGraphMetadata = tx.createGraph(srcGraphMetadata);

        this.projectId = projectId;
        this.branchId = branchId;
        this.srcGraphId = srcGraphMetadata.getId();
        this.dstGraphId = dstGraphMetadata.getId();

        tx.commit();
    }

    public ProjectMetadata.Id getProjectId() {
        return projectId;
    }

    public BranchMetadata.Id getBranchId() {
        return branchId;
    }

    public GraphMetadata.Id getSrcGraphId() {
        return srcGraphId;
    }

    public GraphMetadata.Id getDstGraphId() {
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

        setJobState(jobId, JobMetadata.RUNNING);

        DendriteGraph srcGraph = metaGraph.getGraph(srcGraphId);
        DendriteGraph dstGraph = metaGraph.getGraph(dstGraphId);

        try {
            copyIndices(srcGraph, dstGraph);
            copyGraph(srcGraph, dstGraph);

            // Update the branch to point the new graph.
            MetaGraphTx tx = metaGraph.newTransaction();
            try {
                BranchMetadata branchMetadata = tx.getBranch(branchId);
                branchMetadata.setGraph(tx.getGraph(dstGraph.getId()));
                tx.commit();
            } catch (Throwable t) {
                tx.rollback();
                throw t;
            }

            setJobState(jobId, JobMetadata.DONE);
        } catch (Throwable t) {
            setJobState(jobId, JobMetadata.ERROR, t.getMessage());
            logger.error("error running job: " + t.getMessage());
            return;
        }

        logger.debug("finished job: " + jobId);
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
                if (dstTx.getType(keyVertex.getName()) == null) {
                    dstTx.makePropertyKey(keyVertex.getName(), definition);
                }
            }
        }

        for(TitanLabel titanLabel: srcTx.getTypes(TitanLabel.class)) {
            TitanLabelVertex keyVertex = (TitanLabelVertex) titanLabel;
            TypeAttribute.Map definition = getDefinition(keyVertex);
            if (dstTx.getType(keyVertex.getName()) == null) {
                dstTx.makeEdgeLabel(keyVertex.getName(), definition);
            }
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
