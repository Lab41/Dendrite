package org.lab41.dendrite.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import com.tinkerpop.frames.annotations.gremlin.GremlinParam;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
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
    public Iterable<GraphMetadata> addGraphs(GraphMetadata graph);

    @Adjacency(label = "ownsJob", direction = Direction.OUT)
    public Iterable<JobMetadata> getJobs();

    /*
    @GremlinGroovy("it.out('jobMetadata').has('name', name)")
    Iterable<? extends JobMetadata> getJobsNamed(@GremlinParam("name") String name);
    */

    @Adjacency(label = "ownsJob", direction = Direction.OUT)
    void addJob(JobMetadata jobMetadata);

    /*
    @JavaHandler
    public GraphMetadata getGraph(String graphId);
    */

    /*
    public abstract class Impl implements JavaHandlerContext<Vertex>, ProjectMetadata {

        @Override
        public GraphMetadata getGraph(String graphId) {
            Iterable<Vertex> vertices = this.it().query().labels("ownsGraph").has("_id", graphId).vertices();

            if (vertices)


            Iterable<GraphMetadata> graphs = g().frameVertices(;

            g().frame()
        }
    }
    */
}
