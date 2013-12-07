package org.lab41.dendrite.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.Initializer;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("job")
public interface JobMetadata extends NamedMetadata {

    enum State { WAITING, RUNNING, DONE, ERROR };

    @Property("state")
    public State getState();

    @Property("state")
    public void setState(State state);

    @Property("progress")
    public float getProgress();

    @Property("progress")
    public void setProgress(float progress);

    /*
    @JavaHandler
    public float getTotalProgress();
    */

    @Adjacency(label = "dependsOn", direction = Direction.OUT)
    public Iterable<JobMetadata> getChildJobs();

    @Adjacency(label = "dependsOn", direction = Direction.OUT)
    public void addChildJob(JobMetadata job);

    /*
    @Adjacency(label = "ownedByJob", direction = Direction.OUT)
    public Iterable<JobMetadata> getParentJobs();

    @Adjacency(label = "ownedByJob", direction = Direction.OUT)
    public void addParentJob(JobMetadata job);
    */

    /*
    @Adjacency(label = "childJob", direction = Direction.OUT)
    public Iterable<? extends JobMetadata> getChildJobs();

    @Adjacency(label = "childJob", direction = Direction.OUT)
    public void addChildJob(JobMetadata jobMetadata);
    */

    public abstract class Impl implements JavaHandlerContext<Vertex>, JobMetadata {

        @Initializer
        public void init() {
            setState(State.WAITING);
            setProgress(0);
        }

        /*
        @Override
        public float getTotalProgress() {
            return getProgress();
        }
        */
    }
}
