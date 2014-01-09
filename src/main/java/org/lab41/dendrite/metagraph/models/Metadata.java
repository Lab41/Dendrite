package org.lab41.dendrite.metagraph.models;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.frames.modules.typedgraph.TypeField;

@TypeField("type")
public interface Metadata extends VertexFrame {

    @JavaHandler
    public String getId();

    public abstract class Impl implements JavaHandlerContext<Vertex>, Metadata {

        @Override
        public String getId() {
            return asVertex().getId().toString();
        }

    }
}
