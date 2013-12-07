package org.lab41.dendrite.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("graph")
public interface GraphMetadata extends NamedMetadata {

    @Property("backend")
    public String getBackend();

    @Property("backend")
    public void setBackend(String backent);

    @Property("directory")
    public String getDirectory();

    @Property("directory")
    public void setDirectory(String directory);

    @Property("hostname")
    public String getHostname();

    @Property("hostname")
    public void setHostname(String hostname);

    @Property("port")
    public Integer getPort();

    @Property("port")
    public void setPort(Integer port);

    @Property("tablename")
    public String getTablename();

    @Property("tablename")
    public void setTablename(String tablename);

    @Adjacency(label = "project", direction = Direction.OUT)
    public ProjectMetadata getProject();

    @Adjacency(label = "project", direction = Direction.OUT)
    public void setProject(ProjectMetadata projectMetadata);

    /*
    public abstract class Impl implements JavaHandlerContext<Vertex>, GraphMetadata {

        @Initializer
        public void init() {
        }
    }
    */
}
