package org.lab41.dendrite.metagraph;

import com.google.common.base.Preconditions;
import org.lab41.dendrite.metagraph.models.*;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.FramedTransactionalGraph;

public class MetaGraphTx {

    private TitanGraph titanGraph;
    private FramedGraphFactory framedGraphFactory;
    private FramedTransactionalGraph<TitanTransaction> tx = null;

    public MetaGraphTx(TitanGraph titanGraph, FramedGraphFactory framedGraphFactory) {
        this.titanGraph = titanGraph;
        this.framedGraphFactory = framedGraphFactory;
    }

    private FramedTransactionalGraph<TitanTransaction> getAutoStartTx() {
        if (tx == null) {
            tx = framedGraphFactory.create(titanGraph.newTransaction());
        }

        return tx;
    }

    public void rollback() {
        if (tx != null) {
            tx.rollback();
            tx = null;
        }
    }

    public void commit() {
        if (tx != null) {
            tx.commit();
            tx = null;
        }
    }

    public Iterable<ProjectMetadata> getProjects() {
        return getVertices("project", ProjectMetadata.class);
    }

    public ProjectMetadata getProject(String projectId) {
        return getVertex(projectId, "project", ProjectMetadata.class);
    }

    public ProjectMetadata createProject(String name) {
        return createProject(name, true);
    }

    public ProjectMetadata createProject(String name, boolean createBranch) {
        Preconditions.checkArgument(!name.isEmpty());

        ProjectMetadata projectMetadata = createVertex("project", ProjectMetadata.class);
        projectMetadata.setName(name);

        if (createBranch) {
            BranchMetadata branchMetadata = createBranch("master", projectMetadata);
            projectMetadata.setCurrentBranch(branchMetadata);
        }

        return projectMetadata;
    }

    public void deleteProject(ProjectMetadata projectMetadata) throws Exception {
        projectMetadata.setCurrentBranch(null);

        for (BranchMetadata branchMetadata: projectMetadata.getBranches()) {
            deleteBranch(branchMetadata);
        }

        for (GraphMetadata graphMetadata: projectMetadata.getGraphs()) {
            deleteGraph(graphMetadata);
        }

        for (JobMetadata jobMetadata: projectMetadata.getJobs()) {
            deleteJob(jobMetadata);
        }

        getAutoStartTx().removeVertex(projectMetadata.asVertex());
    }

    public Iterable<? extends GraphMetadata> getGraphs() {
        return getVertices("graph", GraphMetadata.class);
    }

    public GraphMetadata getGraph(String graphId) {
        return getVertex(graphId, "graph", GraphMetadata.class);
    }

    public GraphMetadata createGraph(ProjectMetadata projectMetadata) {
        GraphMetadata graphMetadata = getAutoStartTx().addVertex(null, GraphMetadata.class);
        projectMetadata.addGraph(graphMetadata);

        return graphMetadata;
    }

    public GraphMetadata createGraph(GraphMetadata parentGraphMetadata) {
        GraphMetadata graphMetadata = getAutoStartTx().addVertex(null, GraphMetadata.class);
        parentGraphMetadata.addChildGraph(graphMetadata);

        return graphMetadata;
    }

    public void deleteGraph(GraphMetadata graphMetadata) throws Exception {
        // We cannot delete the current graph.
        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata != null) {
            GraphMetadata currentGraphMetadata = projectMetadata.getCurrentGraph();
            if (currentGraphMetadata != null && graphMetadata.getId().equals(currentGraphMetadata.getId())) {
                throw new Exception("cannot delete the current graph");
            }
        }

        getAutoStartTx().removeVertex(graphMetadata.asVertex());
    }

    public Iterable<? extends BranchMetadata> getBranches() {
        return getVertices("branch", BranchMetadata.class);
    }

    public BranchMetadata getBranch(String branchId) {
        return getAutoStartTx().getVertex(branchId, BranchMetadata.class);
    }

    /**
     * Create an empty branch in a project.
     *
     * @param name the branch name
     * @param projectMetadata the project that owns the branch.
     * @return the branch.
     */
    public BranchMetadata createBranch(String name, ProjectMetadata projectMetadata) {
        return createBranch(name, projectMetadata, createGraph(projectMetadata));
    }

    /**
     * Create a branch with the given graph in a project.
     *
     * @param name the branch name
     * @param projectMetadata the project that owns the branch.
     * @param graphMetadata the branch graph.
     * @return the branch.
     */
    public BranchMetadata createBranch(String name, ProjectMetadata projectMetadata, GraphMetadata graphMetadata) {
        BranchMetadata branchMetadata = getAutoStartTx().addVertex(null, BranchMetadata.class);
        branchMetadata.setName(name);
        branchMetadata.setGraph(graphMetadata);
        projectMetadata.addBranch(branchMetadata);

        return branchMetadata;
    }

    /**
     * Delete a branch.
     *
     * @param branchMetadata the branch.
     * @throws CannotDeleteCurrentBranchException
     */
    public void deleteBranch(BranchMetadata branchMetadata) throws CannotDeleteCurrentBranchException {
        // We cannot delete the current branch.
        ProjectMetadata projectMetadata = branchMetadata.getProject();
        if (projectMetadata != null) {
            BranchMetadata currentBranchMetadata = projectMetadata.getCurrentBranch();
            if (currentBranchMetadata != null && branchMetadata.getId().equals(currentBranchMetadata.getId())) {
                throw new CannotDeleteCurrentBranchException();
            }
        }

        getAutoStartTx().removeVertex(branchMetadata.asVertex());
    }

    public Iterable<? extends JobMetadata> getJobs() {
        return getVertices("job", JobMetadata.class);
    }

    public JobMetadata getJob(String jobId) {
        return getVertex(jobId, "job", JobMetadata.class);
    }

    public JobMetadata createJob(ProjectMetadata projectMetadata) {
        JobMetadata jobMetadata = getAutoStartTx().addVertex(null, JobMetadata.class);
        projectMetadata.addJob(jobMetadata);

        return jobMetadata;
    }

    public JobMetadata createJob(JobMetadata parentJobMetadata) {
        JobMetadata jobMetadata = getAutoStartTx().addVertex(null, JobMetadata.class);
        parentJobMetadata.addChildJob(jobMetadata);
        parentJobMetadata.getProject().addJob(jobMetadata);

        return jobMetadata;
    }

    public void deleteJob(JobMetadata jobMetadata) {
        for (JobMetadata childJobMetadata: jobMetadata.getChildJobs()) {
            deleteJob(childJobMetadata);
        }

        getAutoStartTx().removeVertex(jobMetadata.asVertex());
    }

    private <F> Iterable<F> getVertices(String type, final Class<F> kind) {
        return getAutoStartTx().getVertices("type", type, kind);
    }

    private <F extends Metadata> F getVertex(String id, String type, Class<F> kind) {
        F framedVertex = getAutoStartTx().getVertex(id, kind);

        Preconditions.checkArgument(type.equals(framedVertex.asVertex().getProperty("type")));

        return framedVertex;
    }

    private <F extends Metadata> F createVertex(String type, Class<F> kind) {
        F framedVertex = getAutoStartTx().addVertex(null, kind);

        framedVertex.asVertex().setProperty("type", type);

        return framedVertex;
    }
}
