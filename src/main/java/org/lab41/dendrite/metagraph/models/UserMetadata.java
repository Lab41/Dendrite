package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

/**
 * Models a user. Keeps track of what projects they belong to.
 */
@TypeValue("user")
public interface UserMetadata  extends NamedMetadata {

    @JavaHandler
    public Id getId();

    /**
     * Returns all the projects created by this user
     *
     * @return
     */
    @Adjacency(label = "ownsProject", direction = Direction.IN)
    public Iterable<ProjectMetadata> getOwnedProjects();

    public static class Id {
        String id;

        public Id(String id) {
            this.id = id;
        }

        public String toString() {
            return this.id;
        }
    }

    public abstract class Impl implements JavaHandlerContext<Vertex>, UserMetadata {

        @Override
        public Id getId() {
            return new Id(asVertex().getId().toString());
        }
    }
}

