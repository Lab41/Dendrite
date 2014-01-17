package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.Initializer;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;

import java.util.*;

@TypeValue("graph")
public interface GraphMetadata extends Metadata {

    @Property("creationTime")
    public Date getCreationTime();

    @Property("creationTime")
    public void setCreationTime(Date date);

    @Property("properties")
    public Properties getProperties();

    @Property("properties")
    public void setProperties(Properties properties);

    @JavaHandler
    public Configuration getConfiguration();

    /// Return the project that owns this graph.
    @Adjacency(label = "ownsGraph", direction = Direction.IN)
    public ProjectMetadata getProject();

    /// Return all the branches that directly contain this graph.
    @Adjacency(label = "ownsBranch", direction = Direction.IN)
    public Iterable<BranchMetadata> getDirectBranches();

    /// Return all the branches that contain this graph.
    @JavaHandler
    public Set<BranchMetadata> getBranches();

    /// Return all the immediate graphs that were derived from this graph.
    @Adjacency(label = "childGraph", direction = Direction.OUT)
    public Iterable<GraphMetadata> getChildGraphs();

    /// Add a new child graph.
    @Adjacency(label = "childGraph", direction = Direction.OUT)
    public void addChildGraph(GraphMetadata graph);

    /// Return the graph that this graph was derived from.
    @Adjacency(label = "childGraph", direction = Direction.IN)
    public GraphMetadata getParentGraph();

    public abstract class Impl implements JavaHandlerContext<Vertex>, GraphMetadata {

        @Initializer
        public void init() {
            setCreationTime(new Date());
        }

        @Override
        @JavaHandler
        public Configuration getConfiguration() {
            Properties properties = getProperties();
            if (properties == null) {
                return null;
            } else {
                return new MapConfiguration(properties);
            }
        }

        @Override
        @JavaHandler
        public Set<BranchMetadata> getBranches() {
            Set<BranchMetadata> branches = new HashSet<>();

            for (BranchMetadata branch: getDirectBranches()) {
                branches.add(branch);
            }

            for (GraphMetadata child: getChildGraphs()) {
                branches.addAll(child.getBranches());
            }

            return branches;
        }
    }
}
