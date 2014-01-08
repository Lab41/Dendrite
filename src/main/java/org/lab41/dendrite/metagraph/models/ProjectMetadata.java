package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("project")
public interface ProjectMetadata extends NamedMetadata {

    @Adjacency(label = "currentGraph", direction = Direction.OUT)
    public GraphMetadata getCurrentGraph();

    @Adjacency(label = "currentGraph", direction = Direction.OUT)
    public void setCurrentGraph(GraphMetadata graph);

    @Adjacency(label = "ownsGraph", direction = Direction.OUT)
    public Iterable<GraphMetadata> getGraphs();

    @Adjacency(label = "ownsGraph", direction = Direction.OUT)
    public void addGraph(GraphMetadata graph);

    @Adjacency(label = "ownsJob", direction = Direction.OUT)
    public Iterable<JobMetadata> getJobs();

    @Adjacency(label = "ownsJob", direction = Direction.OUT)
    void addJob(JobMetadata jobMetadata);
}
