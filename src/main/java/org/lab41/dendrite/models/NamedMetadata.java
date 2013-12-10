package org.lab41.dendrite.models;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface NamedMetadata extends Metadata {

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(final String name);
}
