package org.lab41.dendrite.services;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedTransactionalGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.modules.gremlingroovy.GremlinGroovyModule;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import com.tinkerpop.frames.modules.typedgraph.TypedGraphModuleBuilder;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.lab41.dendrite.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Iterator;

@Service
public class MetadataService {

    static Logger logger = LoggerFactory.getLogger(MetadataService.class);

    private TitanGraph titanGraph;
    private FramedGraphFactory framedGraphFactory;

    @Autowired(required = true)
    public MetadataService(@Value("${metadata-graph.properties}") String pathToProperties, ResourceLoader resourceLoader) throws IOException, ConfigurationException {

        logger.debug("Path to Properties: " + pathToProperties);

        Resource resource = resourceLoader.getResource(pathToProperties);
        Configuration configuration = new PropertiesConfiguration(resource.getFile());
        titanGraph = TitanFactory.open(configuration);

        initializeGraph();

        framedGraphFactory = new FramedGraphFactory(
                new GremlinGroovyModule(),
                new JavaHandlerModule(),
                new TypedGraphModuleBuilder()
                        //.withClass(NamedMetadata.class)
                        .withClass(ProjectMetadata.class)
                        .withClass(GraphMetadata.class)
                        .withClass(JobMetadata.class)
                        //.withClass(HadoopJobMetadata.class)
                        .build()
        );
    }

    private void initializeGraph() {
        // Metadata keys
        if (titanGraph.getType("type") == null) {
            titanGraph.makeKey("type")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .indexed(Edge.class)
                    .make();
        }

        // NamedMetadata keys
        if (titanGraph.getType("name") == null) {
            titanGraph.makeKey("name")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .indexed(Edge.class)
                    .make();
        }

        if (titanGraph.getType("typeAndName") == null) {
            titanGraph.makeKey("typeAndName")
                    .dataType(String.class)
                    .unique()
                    .indexed(Vertex.class)
                    .make();
        }

        // ProjectMetadata keys
        if (titanGraph.getType("graphHead") == null) {
            titanGraph.makeLabel("graphHead").oneToOne().make();
        }

        if (titanGraph.getType("ownsJob") == null) {
            titanGraph.makeLabel("ownsJob").oneToMany().make();
        }

        if (titanGraph.getType("ownsJob") == null) {
            titanGraph.makeLabel("ownsJob").oneToMany().make();
        }

        // GraphMetadata keys
        if (titanGraph.getType("backend") == null) {
            titanGraph.makeKey("backend")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (titanGraph.getType("directory") == null) {
            titanGraph.makeKey("directory")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (titanGraph.getType("hostname") == null) {
            titanGraph.makeKey("hostname")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (titanGraph.getType("port") == null) {
            titanGraph.makeKey("port")
                    .dataType(Integer.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (titanGraph.getType("tablename") == null) {
            titanGraph.makeKey("tablename")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        // JobMetadata keys
        if (titanGraph.getType("state") == null) {
            titanGraph.makeKey("state")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (titanGraph.getType("progress") == null) {
            titanGraph.makeKey("progress")
                    .dataType(Float.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (titanGraph.getType("mapreduceJobId") == null) {
            titanGraph.makeKey("mapreduceJobId")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (titanGraph.getType("childJob") == null) {
            titanGraph.makeLabel("childJob").oneToMany().make();
        }

        if (titanGraph.getType("parentJob") == null) {
            titanGraph.makeLabel("parentJob").manyToOne().make();
        }

        titanGraph.commit();
    }

    public MetadataTx newTransaction() {
        return new MetadataTx(titanGraph, framedGraphFactory);
    }
}
