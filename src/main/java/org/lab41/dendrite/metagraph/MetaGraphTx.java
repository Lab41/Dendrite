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
        Preconditions.checkArgument(!name.isEmpty());

        ProjectMetadata projectMetadata = createVertex("project", ProjectMetadata.class);
        projectMetadata.setName(name);

        // Create the initial graph.
        GraphMetadata graphMetadata = createGraph(projectMetadata);
        projectMetadata.setCurrentGraph(graphMetadata);

        return projectMetadata;
    }

    public void deleteProject(ProjectMetadata projectMetadata) throws Exception {

        projectMetadata.setCurrentGraph(null);

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
