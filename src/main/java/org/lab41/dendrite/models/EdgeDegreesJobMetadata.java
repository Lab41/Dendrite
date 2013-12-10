package org.lab41.dendrite.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.Initializer;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("EdgeDegreesJobMetadata")
public interface EdgeDegreesJobMetadata extends JobMetadata, NamedMetadata {

    @Adjacency(label = "job", direction = Direction.OUT)
    public Iterable<HadoopJobMetadata> getHadoopJobs();

    @Adjacency(label = "job", direction = Direction.OUT)
    public void addHadoopJob(HadoopJobMetadata hadoopJob);

    public abstract class Impl implements JavaHandlerContext<Vertex>, EdgeDegreesJobMetadata {

        @Initializer
        public void init() {
            //setType("EdgeDegreesJobMetadata");
        }

    }
}
