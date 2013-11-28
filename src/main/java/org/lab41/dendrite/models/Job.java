package org.lab41.dendrite.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeField;

public interface Job {
    @Property("type")
    public String getType();

    @Property("type")
    public void setType(String type);

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(String name);

    @Property("status")
    public String getStatus();

    @Property("status")
    public void setStatus(String status);

    @Adjacency(label = "graph-source", direction = Direction.OUT)
    public GraphMetadata getGraphMetadata();

    @Adjacency(label = "graph-source", direction = Direction.OUT)
    public void setGraphMetadata(GraphMetadata graphMetadata);
}
