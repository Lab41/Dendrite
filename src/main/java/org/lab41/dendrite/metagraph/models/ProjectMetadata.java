package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinParam;
import com.tinkerpop.frames.modules.javahandler.Initializer;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

import java.util.Date;
import java.util.Iterator;

@TypeValue("project")
public interface ProjectMetadata extends NamedMetadata {

    @JavaHandler
    public Id getId();

    @Property("creationTime")
    public Date getCreationTime();

    @Property("creationTime")
    public void setCreationTime(Date date);

    @Adjacency(label = "currentBranch", direction = Direction.OUT)
    public BranchMetadata getCurrentBranch();

    @Adjacency(label = "currentBranch", direction = Direction.OUT)
    public void setCurrentBranch(BranchMetadata branchMetadata);

    @JavaHandler
    public GraphMetadata getCurrentGraph();

    @Adjacency(label = "userOwnsProject", direction = Direction.OUT)
    public void addUser(UserMetadata user);

    @Adjacency(label = "userOwnsProject", direction = Direction.OUT)
    public Iterable<UserMetadata> getUsers();

    @Adjacency(label = "ownsBranch", direction = Direction.OUT)
    public Iterable<BranchMetadata> getBranches();

    @JavaHandler
    public BranchMetadata getBranchByName(String branchName);

    @Adjacency(label = "ownsBranch", direction = Direction.OUT)
    public void addBranch(BranchMetadata branchMetadata);

    @Adjacency(label = "ownsGraph", direction = Direction.OUT)
    public Iterable<GraphMetadata> getGraphs();

    @Adjacency(label = "ownsGraph", direction = Direction.OUT)
    public void addGraph(GraphMetadata graph);

    @Adjacency(label = "ownsJob", direction = Direction.OUT)
    public Iterable<JobMetadata> getJobs();

    @Adjacency(label = "ownsJob", direction = Direction.OUT)
    void addJob(JobMetadata jobMetadata);

    public static class Id {
        String id;

        public Id(String id) {
            this.id = id;
        }

        public String toString() {
            return this.id;
        }
    }

    public abstract class Impl implements JavaHandlerContext<Vertex>, ProjectMetadata {

        @Initializer
        public void init() {
            setCreationTime(new Date());
        }

        @Override
        @JavaHandler
        public GraphMetadata getCurrentGraph() {
            BranchMetadata branchMetadata = getCurrentBranch();

            if (branchMetadata == null) {
                return null;
            }

            return branchMetadata.getGraph();
        }

        @Override
        @JavaHandler
        public BranchMetadata getBranchByName(String branchName) {
            Iterator<? extends Element> elements = gremlin().out("ownsBranch").has("name", branchName).iterator();

            if (elements.hasNext()) {
                Element element = elements.next();
                assert element instanceof Vertex;

                return frame((Vertex) element, BranchMetadata.class);
            } else {
                return null;
            }
        }
    }
}
