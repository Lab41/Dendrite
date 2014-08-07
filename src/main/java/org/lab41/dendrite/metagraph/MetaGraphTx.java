package org.lab41.dendrite.metagraph;

import com.google.common.base.Preconditions;
import org.lab41.dendrite.metagraph.models.*;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.FramedTransactionalGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

public class MetaGraphTx {
    Logger logger = LoggerFactory.getLogger(MetaGraphTx.class);

    private FramedTransactionalGraph<DendriteGraphTx> tx = null;

    public MetaGraphTx(DendriteGraphTx tx, FramedGraphFactory framedGraphFactory) {
        this.tx = framedGraphFactory.create(tx);
    }

    public void rollback() {
        tx.rollback();
    }

    public void commit() {
        tx.commit();
    }

    public UserMetadata createUser(String name) {
        Preconditions.checkArgument(!name.isEmpty());

        UserMetadata userMetadata = createVertex("user", UserMetadata.class);
        userMetadata.setName(name);

        return userMetadata;
    }

    public Iterable<UserMetadata> getUsers() {
        return getVertices("user", UserMetadata.class);
    }

    public UserMetadata getUser(String userId) {
        return getVertex(userId, "user", UserMetadata.class);
    }

    /**
     * Get or creates the user from an authentication principal.
     *
     * @param principal
     * @return
     */
    public UserMetadata getOrCreateUser(Principal principal) {
        String name = principal.getName();

        UserMetadata userMetadata = getUserByName(name);
        if (userMetadata == null) {
            userMetadata = createUser(name);
        }

        return userMetadata;
    }

    public UserMetadata getUserByName(String userName) {
        return tx.query()
                .has("type", "user")
                .has("name", userName)
                .vertices(UserMetadata.class)
                .iterator()
                .next();
    }

    public Iterable<ProjectMetadata> getProjects() {
        return getVertices("project", ProjectMetadata.class);
    }

    public ProjectMetadata getProject(String projectId) {
        return getVertex(projectId, "project", ProjectMetadata.class);
    }

    public ProjectMetadata createProject(String name, UserMetadata userMetadata) {
        return createProject(name, userMetadata, true);
    }

    public ProjectMetadata createProject(String name, UserMetadata userMetadata, boolean createBranch) {
        Preconditions.checkArgument(!name.isEmpty());

        ProjectMetadata projectMetadata = createVertex("project", ProjectMetadata.class);
        projectMetadata.setName(name);
        projectMetadata.addUser(userMetadata);

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

        tx.removeVertex(projectMetadata.asVertex());
    }

    public Iterable<? extends GraphMetadata> getGraphs() {
        return getVertices("graph", GraphMetadata.class);
    }

    public GraphMetadata getGraph(String graphId) {
        return getVertex(graphId, "graph", GraphMetadata.class);
    }

    public GraphMetadata createGraph(ProjectMetadata projectMetadata) {
        GraphMetadata graphMetadata = tx.addVertex(null, GraphMetadata.class);
        projectMetadata.addGraph(graphMetadata);

        return graphMetadata;
    }

    public GraphMetadata createGraph(GraphMetadata parentGraphMetadata) {
        ProjectMetadata projectMetadata = parentGraphMetadata.getProject();
        GraphMetadata graphMetadata = createGraph(projectMetadata);

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

        tx.removeVertex(graphMetadata.asVertex());
    }

    public Iterable<? extends BranchMetadata> getBranches() {
        return getVertices("branch", BranchMetadata.class);
    }

    public BranchMetadata getBranch(String branchId) {
        return getVertex(branchId, "branch", BranchMetadata.class);
    }

    /**
     * Create an branch in a project. The branch's graph will be created from the project's current graph.
     *
     * @param name the branch name
     * @param projectMetadata the project that owns the branch.
     * @return the branch.
     */
    public BranchMetadata createBranch(String name, ProjectMetadata projectMetadata) {
        GraphMetadata parentGraphMetadata = projectMetadata.getCurrentGraph();
        GraphMetadata graphMetadata;

        // Handle the case where we don't have a graph yet.
        if (parentGraphMetadata == null) {
            graphMetadata = createGraph(projectMetadata);
        } else {
            graphMetadata = createGraph(parentGraphMetadata);
        }

        return createBranch(name, graphMetadata);
    }

    /**
     * Create a branch with the given graph in a project.
     *
     * @param name the branch name
     * @param graphMetadata the branch graph.
     * @return the branch.
     */
    public BranchMetadata createBranch(String name, GraphMetadata graphMetadata) {
        BranchMetadata branchMetadata = tx.addVertex(null, BranchMetadata.class);
        branchMetadata.setName(name);
        branchMetadata.setGraph(graphMetadata);
        graphMetadata.getProject().addBranch(branchMetadata);

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

        tx.removeVertex(branchMetadata.asVertex());
    }

    public Iterable<? extends JobMetadata> getJobs() {
        return getVertices("job", JobMetadata.class);
    }

    public JobMetadata getJob(String jobId) {
        return getVertex(jobId, "job", JobMetadata.class);
    }

    public JobMetadata createJob(ProjectMetadata projectMetadata) {
        JobMetadata jobMetadata = tx.addVertex(null, JobMetadata.class);
        projectMetadata.addJob(jobMetadata);

        return jobMetadata;
    }

    public JobMetadata createJob(JobMetadata parentJobMetadata) {
        JobMetadata jobMetadata = tx.addVertex(null, JobMetadata.class);
        parentJobMetadata.addChildJob(jobMetadata);
        parentJobMetadata.getProject().addJob(jobMetadata);

        return jobMetadata;
    }

    public void deleteJob(JobMetadata jobMetadata) {
        for (JobMetadata childJobMetadata: jobMetadata.getChildJobs()) {
            deleteJob(childJobMetadata);
        }

        tx.removeVertex(jobMetadata.asVertex());
    }

    private <F> Iterable<F> getVertices(String type, final Class<F> kind) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(kind);

        return tx.getVertices("type", type, kind);
    }

    private <F extends Metadata> F getVertex(String id, String type, Class<F> kind) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(kind);

        F framedVertex = tx.getVertex(id, kind);

        if (framedVertex == null) {
            return null;
        }

        Preconditions.checkArgument(type.equals(framedVertex.asVertex().getProperty("type")));

        return framedVertex;
    }

    private <F extends Metadata> F createVertex(String type, Class<F> kind) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(kind);

        F framedVertex = tx.addVertex(null, kind);

        framedVertex.asVertex().setProperty("type", type);

        return framedVertex;
    }

    public static class NotFound extends Exception {
        private Class kind;
        private String id;

        public NotFound(Class kind) {
            super("could not find " + kind.getSimpleName());
            this.kind = kind;
            this.id = null;
        }

        public NotFound(Class kind, String id) {
            super("could not find " + kind.getSimpleName() + " '" + id + "'");
            this.kind = kind;
            this.id = id;
        }

        public Class getKind() {
            return kind;
        }

        public String getId() {
            return id;
        }
    }
}
