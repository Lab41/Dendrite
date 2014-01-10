package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("branch")
public interface BranchMetadata extends NamedMetadata {

    @Adjacency(label = "ownsBranch", direction = Direction.IN)
    public ProjectMetadata getProject();

    @Adjacency(label = "branchTarget", direction = Direction.OUT)
    public GraphMetadata getGraph();

    @Adjacency(label = "branchTarget", direction = Direction.OUT)
    public void setGraph(GraphMetadata graphMetadata);
}
