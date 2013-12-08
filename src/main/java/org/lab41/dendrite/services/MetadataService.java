package org.lab41.dendrite.services;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
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
    private FramedTransactionalGraph<TitanGraph> framedGraph;

    @Autowired(required = true)
    public MetadataService(@Value("${metadata-graph.properties}") String pathToProperties, ResourceLoader resourceLoader) throws IOException, ConfigurationException {

        logger.debug("Path to Properties: " + pathToProperties);

        Resource resource = resourceLoader.getResource(pathToProperties);
        Configuration configuration = new PropertiesConfiguration(resource.getFile());

        FramedGraphFactory factory = new FramedGraphFactory(
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
        titanGraph = TitanFactory.open(configuration);
        framedGraph = factory.create(titanGraph);

        initializeGraph();
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
                    .dataType(JobMetadata.State.class)
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

        if (titanGraph.getType("dependsOn") == null) {
            titanGraph.makeLabel("dependsOn").oneToMany().make();
        }

        /*
        if (titanGraph.getType("dependedOnBy") == null) {
            titanGraph.makeLabel("dependedOnBy").manyToOne().make();
        }
        */

        // HadoopJobMetadata keys
        if (titanGraph.getType("jobid") == null) {
            titanGraph.makeKey("jobid")
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .make();
        }

        titanGraph.commit();
    }

    public void rollback() {
        framedGraph.rollback();
    }

    public void commit() {
        framedGraph.commit();
    }

    public Iterable<ProjectMetadata> getProjects() {
        return getVertices("project", ProjectMetadata.class);
    }

    public ProjectMetadata getProject(String projectId) {
        return framedGraph.getVertex(projectId, ProjectMetadata.class);
    }

    public ProjectMetadata createProject() throws Exception {
        ProjectMetadata projectMetadata = createVertex("project", ProjectMetadata.class);

        // Create the initial graph.
        GraphMetadata graphMetadata = createGraph(projectMetadata);
        projectMetadata.setCurrentGraph(graphMetadata);

        return projectMetadata;
    }

    public void deleteProject(ProjectMetadata projectMetadata) throws Exception {

        projectMetadata.setCurrentGraph(null);

        for (GraphMetadata graphMetadata: projectMetadata.getGraphs()) {
            deleteGraph(graphMetadata);
        }

        for (JobMetadata jobMetadata: projectMetadata.getJobs()) {
            deleteJob(jobMetadata);
        }

        framedGraph.removeVertex(projectMetadata.asVertex());
    }

    public Iterable<? extends GraphMetadata> getGraphs() {
        return getVertices("graph", GraphMetadata.class);
    }

    public GraphMetadata getGraph(String graphId) {
        return framedGraph.getVertex(graphId, GraphMetadata.class);
    }

    public GraphMetadata createGraph(ProjectMetadata projectMetadata) {
        GraphMetadata graphMetadata = framedGraph.addVertex(null, GraphMetadata.class);
        projectMetadata.addGraphs(graphMetadata);
        graphMetadata.setProject(projectMetadata);

        return graphMetadata;
    }

    public void deleteGraph(GraphMetadata graphMetadata) throws Exception {
        // We cannot delete the current graph.
        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata != null) {
            GraphMetadata currentGraphMetadata = projectMetadata.getCurrentGraph();
            if (currentGraphMetadata != null && graphMetadata.getId().equals(currentGraphMetadata.getId())) {
                throw new Exception("cannot delete the current graph");
            }
        }

        framedGraph.removeVertex(graphMetadata.asVertex());
    }

    public Iterable<? extends JobMetadata> getJobs() {
        return getVertices("job", JobMetadata.class);
    }

    public JobMetadata getJob(String jobId) {
        return framedGraph.getVertex(jobId, JobMetadata.class);
    }

    public JobMetadata createJob(ProjectMetadata projectMetadata) {
        JobMetadata jobMetadata = framedGraph.addVertex(null, JobMetadata.class);
        projectMetadata.addJob(jobMetadata);

        return jobMetadata;
    }

    public JobMetadata createJob(JobMetadata parentJobMetadata) {
        JobMetadata jobMetadata = framedGraph.addVertex(null, JobMetadata.class);
        //jobMetadata.setParentJob(parentJobMetadata);
        parentJobMetadata.addChildJob(jobMetadata);

        return jobMetadata;
    }

    public void deleteJob(JobMetadata jobMetadata) {
        for (JobMetadata childJobMetadata: jobMetadata.getChildJobs()) {
            deleteJob(childJobMetadata);
        }

        framedGraph.removeVertex(jobMetadata.asVertex());
    }

    private <F> Iterable<F> getVertices(String type, final Class<F> kind) {
        return framedGraph.getVertices("type", type, kind);
    }

    private <F extends NamedMetadata> F getNamedVertex(String type, String name, final Class<F> kind) {
        /*
        Iterable<F> vertices = framedGraph.query()
                .has("type", type)
                .has("name", name)
                .vertices(kind);
        */

        Vertex vertex = titanGraph.newTransaction()
                .getVertex("typeAndName", type + ":" + name);

        if (vertex == null) {
            return null;
        } else {
            return framedGraph.frame(vertex, kind);
        }
    }

    private <F extends Metadata> F createVertex(String type, Class<F> kind) {
        F framedVertex = framedGraph.addVertex(null, kind);

        framedVertex.asVertex().setProperty("type", type);

        return framedVertex;
    }

    private <F extends NamedMetadata> F createNamedVertex(String type, String name, Class<F> kind) {
        F framedVertex = framedGraph.addVertex(null, kind);

        framedVertex.asVertex().setProperty("type", type);
        framedVertex.asVertex().setProperty("name", name);
        framedVertex.asVertex().setProperty("typeAndName", type + ":" + name);

        return framedVertex;
    }
}
