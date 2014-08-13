package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;
import com.tinkerpop.gremlin.java.GremlinPipeline;

import java.util.Iterator;

/**
 * Models a user. Keeps track of what projects they belong to.
 */
@TypeValue("user")
public interface UserMetadata extends NamedMetadata {

    @JavaHandler
    public Id getId();

    /**
     * Returns all the projects created by this user
     *
     * @return
     */
    @Adjacency(label = "ownsProject", direction = Direction.OUT)
    public Iterable<ProjectMetadata> getProjects();

    @JavaHandler
    public Iterable<GraphMetadata> getGraphs();

    @JavaHandler
    public GraphMetadata getGraph(GraphMetadata.Id id);

    @JavaHandler
    public Iterable<BranchMetadata> getBranches();

    @JavaHandler
    public BranchMetadata getBranch(BranchMetadata.Id id);

    public static class Id {
        String id;

        public Id(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return this.id;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Id && id.equals(((Id) other).id);
        }
    }

    public abstract class Impl implements JavaHandlerContext<Vertex>, UserMetadata {

        @Override
        public Id getId() {
            return new Id(asVertex().getId().toString());
        }

        @Override
        @JavaHandler
        public Iterable<BranchMetadata> getBranches() {
            GremlinPipeline<Vertex, Vertex> it = gremlin()
                    .out("ownsProject")
                    .out("ownsBranch");

            return frameVertices(it, BranchMetadata.class);
        }

        @Override
        @JavaHandler
        public BranchMetadata getBranch(BranchMetadata.Id branchId) {

            for (BranchMetadata branchMetadata: getBranches()) {
                if (branchMetadata.getId().equals(branchId)) {
                    return branchMetadata;
                }
            }

            return null;
        }

        @Override
        @JavaHandler
        public Iterable<GraphMetadata> getGraphs() {

            GremlinPipeline<Vertex, Vertex> it = gremlin()
                    .out("ownsProject")
                    .out("ownsGraph");

            return frameVertices(it, GraphMetadata.class);
        }

        @Override
        @JavaHandler
        public GraphMetadata getGraph(GraphMetadata.Id graphId) {

            for (GraphMetadata graphMetadata: getGraphs()) {
                if (graphMetadata.getId().equals(graphId)) {
                    return graphMetadata;
                }
            }

            return null;
        }
    }
}

