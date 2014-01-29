package org.lab41.dendrite.web.controller;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.DendriteGraphTx;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.beans.CreateGraphBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/api")
public class GraphController {

    @Autowired
    MetaGraphService metaGraphService;

    @RequestMapping(value = "/graphs", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getGraphs() {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        List<Map<String, Object>> graphs = new ArrayList<>();
        for (GraphMetadata graphMetadata: tx.getGraphs()) {
            graphs.add(getGraphMap(graphMetadata));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("graphs", graphs);

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/graphs/{graphId}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getGraph(@PathVariable String graphId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        GraphMetadata graphMetadata = tx.getGraph(graphId);

        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find graph '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.put("graph", getGraphMap(graphMetadata));

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/graphs/{graphId}/random", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getRandom(@PathVariable String graphId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        GraphMetadata graphMetadata = tx.getGraph(graphId);

        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find graph '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        DendriteGraph graph = metaGraphService.getGraph(graphId);

        DendriteGraphTx dendriteGraphTx = graph.buildTransaction().readOnly().start();

        Map<Object, Object> verticesMap = new HashMap<>();
        Map<Object, Object> edgesMap = new HashMap<>();

        int vertexCount = 0;
        int edgeCount = 0;

        for (Vertex vertex: dendriteGraphTx.query().vertices()) {
            if (verticesMap.containsKey(vertex.getId())) {
                continue;
            }

            boolean hasEdges = false;

            for (Edge edge: vertex.getEdges(Direction.BOTH)) {
                if (edgesMap.containsKey(edge.getId())) {
                    continue;
                }

                hasEdges = true;
                edgeCount = addEdge(verticesMap, edgesMap, edge, 0, edgeCount);

                if (edgeCount > 100) { break; }
            }

            if (hasEdges) {
                vertexCount += 1;
            } else {
                /*
                if (vertexCount < 100) {
                    edgeCount = addVertex(verticesMap, edgesMap, vertex, 0, edgeCount);
                }
                */
            }

            if (edgeCount > 100) {
                break;
            }
        }

        response.put("vertices", new ArrayList<>(verticesMap.values()));
        response.put("edges", new ArrayList<>(edgesMap.values()));

        dendriteGraphTx.commit();

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private int addVertex(Map<Object, Object> verticesMap, Map<Object, Object> edgesMap, Vertex vertex, int depth, int edgeCount) {
        if (!verticesMap.containsKey(vertex.getId())) {
            Map<String, Object> vertexMap = new HashMap<>();
            verticesMap.put(vertex.getId(), vertexMap);

            vertexMap.put("_id", vertex.getId().toString());
            vertexMap.put("_type", "vertex");

            if (depth <= 5 && edgeCount <= 500) {
                for (Edge inEdge: vertex.getEdges(Direction.BOTH)) {
                    edgeCount = addEdge(verticesMap, edgesMap, inEdge, depth, edgeCount + 1);
                }
            }
        }

        return edgeCount;
    }

    private int addEdge(Map<Object, Object> verticesMap, Map<Object, Object> edgesMap, Edge edge, int depth, int edgeCount) {
        if (depth >= 5 || edgeCount > 500) {
            return edgeCount;
        }

        if (!edgesMap.containsKey(edge.getId())) {
            edgeCount += 1;
            Map<String, Object> edgeMap = new HashMap<>();
            edgesMap.put(edge.getId(), edgeMap);

            Vertex inV = edge.getVertex(Direction.IN);
            Vertex outV = edge.getVertex(Direction.OUT);

            edgeMap.put("_id", edge.getId().toString());
            edgeMap.put("_type", "edge");
            edgeMap.put("_inV", inV.getId().toString());
            edgeMap.put("_outV", outV.getId().toString());

            edgeCount = addVertex(verticesMap, edgesMap, inV, depth + 1, edgeCount + 1);
            edgeCount = addVertex(verticesMap, edgesMap, outV, depth + 1, edgeCount + 1);
        }

        return edgeCount;
    }

    @RequestMapping(value = "/graphs/{graphId}", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteGraph(@PathVariable String graphId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.newTransaction();
        GraphMetadata graphMetadata = tx.getGraph(graphId);

        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find graph '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/graphs", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getGraphs(@PathVariable String projectId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();
        ProjectMetadata projectMetadata = tx.getProject(projectId);

        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> graphs = new ArrayList<>();
        for (GraphMetadata graphMetadata: projectMetadata.getGraphs()) {
            graphs.add(getGraphMap(graphMetadata));
        }

        // Commit must come after all graph access.
        tx.commit();

        response.put("graphs", graphs);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/graphs", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createGraph(@PathVariable String projectId,
                                                           @Valid @RequestBody CreateGraphBean item,
                                                           BindingResult result,
                                                           UriComponentsBuilder builder) {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("status", "error");
            response.put("msg", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        MetaGraphTx tx = metaGraphService.newTransaction();
        ProjectMetadata projectMetadata = tx.getProject(projectId);

        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        GraphMetadata graphMetadata = tx.createGraph(projectMetadata);
        graphMetadata.setProperties(item.getProperties());

        response.put("graph", getGraphMap(graphMetadata));

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/current-graph", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getCurrentGraph(@PathVariable String projectId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();
        ProjectMetadata projectMetadata = tx.getProject(projectId);

        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        GraphMetadata graphMetadata = projectMetadata.getCurrentGraph();
        response.put("graph", getGraphMap(graphMetadata));

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Map<String, Object> getGraphMap(GraphMetadata graphMetadata) {
        Map<String, Object> graph = new HashMap<>();

        graph.put("_id", graphMetadata.getId());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date creationTime = graphMetadata.getCreationTime();
        if (creationTime != null) { graph.put("creationTime", df.format(creationTime)); }

        Properties properties = graphMetadata.getProperties();
        if (properties != null) {
            graph.put("properties", properties);
        }

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata != null) {
            graph.put("projectId", graphMetadata.getProject().getId());
        }

        GraphMetadata parentGraphMetadata = graphMetadata.getParentGraph();
        if (parentGraphMetadata != null) {
            graph.put("parentGraphId", parentGraphMetadata.getId());
        }

        return graph;
    }
}
