package org.lab41.dendrite.metagraph;

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
        return getAutoStartTx().getVertex(projectId, ProjectMetadata.class);
    }

    public ProjectMetadata createProject() {
        ProjectMetadata projectMetadata = createVertex("project", ProjectMetadata.class);

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
        return getAutoStartTx().getVertex(graphId, GraphMetadata.class);
    }

    public GraphMetadata createGraph(ProjectMetadata projectMetadata) {
        GraphMetadata graphMetadata = getAutoStartTx().addVertex(null, GraphMetadata.class);
        projectMetadata.addGraphs(graphMetadata);
        graphMetadata.setProject(projectMetadata);

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
        return getAutoStartTx().getVertex(jobId, JobMetadata.class);
    }

    public JobMetadata createJob(ProjectMetadata projectMetadata) {
        JobMetadata jobMetadata = getAutoStartTx().addVertex(null, JobMetadata.class);
        projectMetadata.addJob(jobMetadata);

        return jobMetadata;
    }

    public JobMetadata createJob(JobMetadata parentJobMetadata) {
        JobMetadata jobMetadata = getAutoStartTx().addVertex(null, JobMetadata.class);
        jobMetadata.setParentJob(parentJobMetadata);
        parentJobMetadata.addChildJob(jobMetadata);

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

    /*
    private <F extends NamedMetadata> F getNamedVertex(String type, String name, final Class<F> kind) {
        / *
        Iterable<F> vertices = getAutoStartTx().query()
                .has("type", type)
                .has("name", name)
                .vertices(kind);
        * /

        Vertex vertex = titanGraph.newTransaction()
                .getVertex("typeAndName", type + ":" + name);

        if (vertex == null) {
            return null;
        } else {
            return getAutoStartTx().frame(vertex, kind);
        }
    }
    */

    private <F extends Metadata> F createVertex(String type, Class<F> kind) {
        F framedVertex = getAutoStartTx().addVertex(null, kind);

        framedVertex.asVertex().setProperty("type", type);

        return framedVertex;
    }

    private <F extends NamedMetadata> F createNamedVertex(String type, String name, Class<F> kind) {
        F framedVertex = getAutoStartTx().addVertex(null, kind);

        framedVertex.asVertex().setProperty("type", type);
        framedVertex.asVertex().setProperty("name", name);
        framedVertex.asVertex().setProperty("typeAndName", type + ":" + name);

        return framedVertex;
    }
}
