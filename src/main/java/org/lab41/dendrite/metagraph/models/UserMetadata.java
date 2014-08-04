package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

/**
 * Models a user. Keeps track of what projects they belong to.
 */
public interface UserMetadata  extends NamedMetadata {

    @Property("LDAPString")
    public String getLDAPString();

    @Property("LDAPString")
    public void setLDAPString();

    /**
     * Returns all the projects created by this user
     *
     * @return
     */
    @Adjacency(label = "createdBy", direction = Direction.IN)
    public Iterable<ProjectMetadata> getCreatedProjects();


    /**
     * Returns all the projects that have been shared with
     * this user by other users.
     *
     * @return
     */
    @Adjacency(label = "sharedWith", direction = Direction.IN)
    public Iterable<ProjectMetadata> getSharedProjects();

}

