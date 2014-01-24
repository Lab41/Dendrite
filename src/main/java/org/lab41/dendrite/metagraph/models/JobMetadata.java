package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.Initializer;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

import java.util.Date;

@TypeValue("job")
public interface JobMetadata extends NamedMetadata {

    // frames doesn't seem to like enums...
    //public enum State { PENDING, RUNNING, DONE, ERROR };

    public static String PENDING = "PENDING";
    public static String RUNNING = "RUNNING";
    public static String DONE = "DONE";
    public static String ERROR = "ERROR";

    @Property("creationTime")
    public Date getCreationTime();

    @Property("creationTime")
    public void setCreationTime(Date date);

    @Property("doneTime")
    public Date getDoneTime();

    @Property("doneTime")
    public void setDoneTime(Date date);

    @Property("state")
    public String getState();

    @JavaHandler
    public void setState(String state);

    @Property("progress")
    public float getProgress();

    @Property("progress")
    public void setProgress(float progress);

    @Property("message")
    public String getMessage();

    @Property("message")
    public void setMessage(String message);

    @Property("mapreduceJobId")
    public String getMapreduceJobId();

    @Property("mapreduceJobId")
    public void setMapreduceJobId(String jobId);

    @Adjacency(label = "ownsJob", direction = Direction.IN)
    public ProjectMetadata getProject();

    @Adjacency(label = "childJob", direction = Direction.OUT)
    public Iterable<JobMetadata> getChildJobs();

    @Adjacency(label = "childJob", direction = Direction.OUT)
    public void addChildJob(JobMetadata job);

    @Adjacency(label = "childJob", direction = Direction.IN)
    public JobMetadata getParentJob();

    public abstract class Impl implements JavaHandlerContext<Vertex>, JobMetadata {

        @Initializer
        public void init() {
            setCreationTime(new Date());
            setState(PENDING );
            setProgress(0);
        }

        @Override
        @JavaHandler
        public void setState(String state) {
            asVertex().setProperty("state", state);

            if (state.equals(DONE) || state.equals(ERROR)) {
                setDoneTime(new Date());
            }
        }

    }
}
