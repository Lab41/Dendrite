package org.lab41.dendrite.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;

import java.util.Properties;

@TypeValue("graph")
public interface GraphMetadata extends Metadata {

    @Property("properties")
    public Properties getProperties();

    @Property("properties")
    public void setProperties(Properties properties);

    @JavaHandler
    public Configuration getConfiguration();

    @Adjacency(label = "project", direction = Direction.OUT)
    public ProjectMetadata getProject();

    @Adjacency(label = "project", direction = Direction.OUT)
    public void setProject(ProjectMetadata projectMetadata);

    public abstract class Impl implements JavaHandlerContext<Vertex>, GraphMetadata {

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
    }
}
