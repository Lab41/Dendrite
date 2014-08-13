package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.Initializer;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

import java.util.Date;

@TypeValue("branch")
public interface BranchMetadata extends NamedMetadata {

    @JavaHandler
    public Id getId();

    @Property("creationTime")
    public Date getCreationTime();

    @Property("creationTime")
    public void setCreationTime(Date date);

    @Property("modificationTime")
    public Date getModificationTime();

    @Property("modificationTime")
    public void setModificationTime(Date date);

    @Adjacency(label = "ownsBranch", direction = Direction.IN)
    public ProjectMetadata getProject();

    @Adjacency(label = "branchTarget", direction = Direction.OUT)
    public GraphMetadata getGraph();

    @JavaHandler
    public void setGraph(GraphMetadata graphMetadata);

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

    public abstract class Impl implements JavaHandlerContext<Vertex>, BranchMetadata {

        @Initializer
        public void init() {
            Date date = new Date();
            setCreationTime(date);
            setModificationTime(date);
        }

        @Override
        public Id getId() {
            return new Id(asVertex().getId().toString());
        }

        @Override
        @JavaHandler
        public void setGraph(GraphMetadata graphMetadata) {
            Vertex vertex = asVertex();
            for (final Edge edge: vertex.getEdges(Direction.OUT, "branchTarget")) {
                g().removeEdge(edge);

            }

            if (graphMetadata != null) {
                g().addEdge(null, vertex, graphMetadata.asVertex(), "branchTarget");
            }

            setModificationTime(new Date());
        }
    }
}
