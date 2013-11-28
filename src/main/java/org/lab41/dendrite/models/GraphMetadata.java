package org.lab41.dendrite.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import com.tinkerpop.frames.annotations.gremlin.GremlinParam;
import com.tinkerpop.frames.modules.typedgraph.TypeField;
import com.tinkerpop.gremlin.groovy.Gremlin;

public interface GraphMetadata {
    @Property("type")
    public String getType();

    @Property("type")
    public void setType(String type);

    @Property("name")
    String getName();

    @Property("name")
    void setName(String name);

    @Adjacency(label = "job", direction = Direction.OUT)
    Iterable<Job> getJobs();

    @GremlinGroovy("it.out('job').has('name', name)")
    Iterable<Job> getJobsNamed(@GremlinParam("name") String name);

    @Adjacency(label = "job", direction = Direction.OUT)
    Job addJob();
}
