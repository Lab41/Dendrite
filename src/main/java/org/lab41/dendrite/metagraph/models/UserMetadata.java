package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

/**
 * Models a user. Keeps track of what projects they belong to.
 */
@TypeValue("user")
public interface UserMetadata  extends NamedMetadata {

    /**
     * Returns all the projects created by this user
     *
     * @return
     */
    @Adjacency(label = "ownsProject", direction = Direction.IN)
    public Iterable<ProjectMetadata> getOwnedProjects();

}

