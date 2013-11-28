package org.lab41.dendrite.services;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedTransactionalGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.gremlingroovy.GremlinGroovyModule;
import com.tinkerpop.frames.modules.typedgraph.TypedGraphModuleBuilder;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.lab41.dendrite.models.GraphMetadata;
import org.lab41.dendrite.models.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MetadataService {

    Logger logger = LoggerFactory.getLogger(MetadataService.class);

    private TitanGraph titanGraph;
    private FramedTransactionalGraph<TitanGraph> framedGraph;

    @Autowired(required = true)
    public MetadataService(@Value("${metadata-graph.properties}") String pathToProperties, ResourceLoader resourceLoader) throws IOException, ConfigurationException {

        logger.debug("Path to Properties: " + pathToProperties);

        Resource resource = resourceLoader.getResource(pathToProperties);
        Configuration configuration = new PropertiesConfiguration(resource.getFile());

        FramedGraphFactory factory = new FramedGraphFactory(
                new GremlinGroovyModule()

                /*
                new TypedGraphModuleBuilder()
                    .withClass(GraphMetadata.class)
                    .withClass(Job.class)
                    .build()
                    */
        );
        titanGraph = TitanFactory.open(configuration);
        framedGraph = factory.create(titanGraph);

        initializeGraph();
    }

    private void initializeGraph() {
        if (titanGraph.getType("type") == null) {
            titanGraph.makeKey("type").dataType(String.class).indexed(Vertex.class).make();
        }

        if (titanGraph.getType("name") == null) {
            titanGraph.makeKey("name").dataType(String.class).indexed(Vertex.class).make();
        }

        if (titanGraph.getType("status") == null) {
            titanGraph.makeKey("status").dataType(String.class).indexed(Vertex.class).make();
        }

        /*
        titanGraph.makeLabel("job").manyToOne().make();
        titanGraph.makeLabel("dataset").oneToOne().make();
        */

        titanGraph.commit();
    }

    public void commit() {
        framedGraph.commit();
    }

    public GraphMetadata getGraphMetadata(String graphName) throws Exception {
        Iterator<Vertex> vertices = titanGraph.query()
                .has("type", "GraphMetadata")
                .has("name", graphName)
                .limit(1)
                .vertices()
                .iterator();

        GraphMetadata graphMetadata;

        if (vertices.hasNext()) {
            Vertex vertex = vertices.next();
            graphMetadata = framedGraph.frame(vertex, GraphMetadata.class);
        } else {
            graphMetadata = framedGraph.addVertex(null, GraphMetadata.class);
            graphMetadata.setType("GraphMetadata");
            graphMetadata.setName(graphName);

            logger.debug("creating GraphMetadata " + graphName);
        }

        framedGraph.commit();

        return graphMetadata;
    }

    public Job getJob(String graphName, String jobName) throws Exception {
        GraphMetadata graphMetadata = getGraphMetadata(graphName);

        Iterator<Job> jobs = graphMetadata.getJobsNamed(jobName).iterator();
        Job job;

        if (jobs.hasNext()) {
            job = jobs.next();
            logger.debug("job found: " + job.getName() + " " + job.getStatus());
        } else {
            job = graphMetadata.addJob();
            job.setType("Job");
            job.setName(jobName);
            job.setStatus("none");

            logger.debug("creating Job " + graphName + " " + jobName);
        }

        framedGraph.commit();

        return job;
    }

}
