package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.frames.Property;

public interface NamedMetadata extends Metadata {

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(final String name);
}
