package org.lab41.dendrite.web.controller;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.DendriteGraphTx;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.NotFound;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.metagraph.models.UserMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.requests.CreateGraphRequest;
import org.lab41.dendrite.web.responses.GetGraphResponse;
import org.lab41.dendrite.web.responses.GetGraphsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/api")
public class GraphController extends AbstractController {

    @Autowired
    MetaGraphService metaGraphService;

    // Note this doesn't use @PreAuthorize on purpose because it'll only show the user's graphs.
    @RequestMapping(value = "/graphs", method = RequestMethod.GET)
    @ResponseBody
    public GetGraphsResponse getGraphs(Principal principal) {

        // This needs to be a read/write transaction as we might make a user.
        MetaGraphTx tx = metaGraphService.buildTransaction().start();
        List<GetGraphResponse> graphs = new ArrayList<>();

        try {
            UserMetadata userMetadata = tx.getOrCreateUser(principal);

            for (ProjectMetadata projectMetadata : userMetadata.getProjects()) {
                for (GraphMetadata graphMetadata : projectMetadata.getGraphs()) {
                    graphs.add(new GetGraphResponse(graphMetadata));
                }
            }
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        // Commit must come after all graph access.
        tx.commit();

        return new GetGraphsResponse(graphs);
    }

    @PreAuthorize("hasPermission(#graphId, 'graph', 'admin')")
    @RequestMapping(value = "/graphs/{graphId}", method = RequestMethod.GET)
    @ResponseBody
    public GetGraphResponse getGraph(@PathVariable String graphId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            GraphMetadata graphMetadata = tx.getGraph(graphId);
            if (graphMetadata == null) {
                throw new NotFound(GraphMetadata.class, graphId);
            }

            // FIXME: Temporary hack to force loading the graph until the UI can handle it occurring asynchronously.
            metaGraphService.getDendriteGraph(graphMetadata.getId());

            return new GetGraphResponse(graphMetadata);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#graphId, 'graph', 'admin')")
    @RequestMapping(value = "/graphs/{graphId}/random", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getRandom(@PathVariable String graphId) throws NotFound {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            GraphMetadata graphMetadata = tx.getGraph(graphId);
            if (graphMetadata == null) {
                throw new NotFound(GraphMetadata.class, graphId);
            }

            DendriteGraph graph = metaGraphService.getDendriteGraph(graphId);

            DendriteGraphTx dendriteGraphTx = graph.buildTransaction().readOnly().start();

            try {
                Map<Object, Object> verticesMap = new HashMap<>();
                Map<Object, Object> edgesMap = new HashMap<>();

                for (Vertex vertex : dendriteGraphTx.query().limit(300).vertices()) {
                    addVertex(verticesMap, vertex);
                }

                for (Edge edge : dendriteGraphTx.query().limit(300).edges()) {
                    addEdge(verticesMap, edgesMap, edge);
                }

                response.put("vertices", new ArrayList<>(verticesMap.values()));
                response.put("edges", new ArrayList<>(edgesMap.values()));
            } finally {
                dendriteGraphTx.commit();
            }

            return new ResponseEntity<>(response, HttpStatus.OK);
        } finally {
            tx.commit();
        }
    }

    private void addVertex(Map<Object, Object> verticesMap, Vertex vertex) {
        if (!verticesMap.containsKey(vertex.getId())) {
            Map<String, Object> vertexMap = new HashMap<>();
            verticesMap.put(vertex.getId(), vertexMap);

            vertexMap.put("_id", vertex.getId().toString());
            vertexMap.put("_type", "vertex");

            // Include the name so we can have a nice tooltip.
            Object value = vertex.getProperty("name");
            if (value != null) {
                vertexMap.put("name", value);
            }
        }
    }

    private void addEdge(Map<Object, Object> verticesMap, Map<Object, Object> edgesMap, Edge edge) {
        if (!edgesMap.containsKey(edge.getId())) {
            Map<String, Object> edgeMap = new HashMap<>();
            edgesMap.put(edge.getId(), edgeMap);

            Vertex inV = edge.getVertex(Direction.IN);
            Vertex outV = edge.getVertex(Direction.OUT);

            edgeMap.put("_id", edge.getId().toString());
            edgeMap.put("_type", "edge");
            edgeMap.put("_inV", inV.getId().toString());
            edgeMap.put("_outV", outV.getId().toString());

            addVertex(verticesMap, inV);
            addVertex(verticesMap, outV);
        }
    }

    @PreAuthorize("hasPermission(#graphId, 'graph', 'admin')")
    @RequestMapping(value = "/graphs/{graphId}", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteGraph(@PathVariable String graphId) throws NotFound {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();

        try {
            GraphMetadata graphMetadata = tx.getGraph(graphId);
            if (graphMetadata == null) {
                throw new NotFound(GraphMetadata.class, graphId);
            }

            try {
                tx.deleteGraph(graphMetadata);
            } catch (Exception e) {
                response.put("status", "error");
                response.put("msg", e.toString());
                tx.rollback();
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            response.put("msg", "deleted");

            return new ResponseEntity<>(response, HttpStatus.OK);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/graphs", method = RequestMethod.GET)
    @ResponseBody
    public GetGraphsResponse getGraphs(@PathVariable String projectId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            List<GetGraphResponse> graphs = new ArrayList<>();
            for (GraphMetadata graphMetadata : projectMetadata.getGraphs()) {
                graphs.add(new GetGraphResponse(graphMetadata));
            }

            return new GetGraphsResponse(graphs);
        } finally {
            tx.commit();
        }
    }

    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/graphs", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<GetGraphResponse> createGraph(@PathVariable String projectId,
                                                        @Valid @RequestBody CreateGraphRequest item,
                                                        BindingResult result,
                                                        UriComponentsBuilder builder) throws BindingException, NotFound {

        if (result.hasErrors()) {
            throw new BindingException(result);
        }

        MetaGraphTx tx = metaGraphService.newTransaction();
        GetGraphResponse getGraphResponse;
        HttpHeaders headers = new HttpHeaders();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            GraphMetadata graphMetadata = tx.createGraph(projectMetadata);
            graphMetadata.setProperties(item.getProperties());

            headers.setLocation(builder.path("/graphs/{graphId}").buildAndExpand(projectMetadata.getId()).toUri());

            getGraphResponse = new GetGraphResponse(graphMetadata);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        return new ResponseEntity<>(getGraphResponse, headers, HttpStatus.CREATED);
    }

    @PreAuthorize("hasPermission(#projectId, 'project', 'admin')")
    @RequestMapping(value = "/projects/{projectId}/current-graph", method = RequestMethod.GET)
    @ResponseBody
    public GetGraphResponse getCurrentGraph(@PathVariable String projectId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            ProjectMetadata projectMetadata = tx.getProject(projectId);
            if (projectMetadata == null) {
                throw new NotFound(ProjectMetadata.class, projectId);
            }

            GraphMetadata graphMetadata = projectMetadata.getCurrentGraph();

            // FIXME: Temporary hack to force loading the graph until the UI can handle it occurring asynchronously.
            metaGraphService.getDendriteGraph(graphMetadata.getId());

            return new GetGraphResponse(graphMetadata);
        } finally {
            tx.commit();
        }
    }
}
