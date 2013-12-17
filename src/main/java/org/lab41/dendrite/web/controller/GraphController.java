package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.models.GraphMetadata;
import org.lab41.dendrite.models.ProjectMetadata;
import org.lab41.dendrite.services.MetadataService;
import org.lab41.dendrite.services.MetadataTx;
import org.lab41.dendrite.web.beans.UpdateCurrentGraphBean;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class GraphController {

    @Autowired
    MetadataService metadataService;

    @RequestMapping(value = "/graphs", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getGraphs() {

        MetadataTx tx = metadataService.newTransaction();

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
        MetadataTx tx = metadataService.newTransaction();

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

    @RequestMapping(value = "/graphs/{graphId}", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteGraph(@PathVariable String graphId) {

        Map<String, Object> response = new HashMap<>();
        MetadataTx tx = metadataService.newTransaction();
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
        MetadataTx tx = metadataService.newTransaction();
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
                                                           UriComponentsBuilder builder) {

        Map<String, Object> response = new HashMap<>();
        MetadataTx tx = metadataService.newTransaction();
        ProjectMetadata projectMetadata = tx.getProject(projectId);

        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        GraphMetadata graphMetadata = tx.createGraph(projectMetadata);

        response.put("graph", getGraphMap(graphMetadata));

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/current-graph", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getCurrentGraph(@PathVariable String projectId) {

        Map<String, Object> response = new HashMap<>();
        MetadataTx tx = metadataService.newTransaction();
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

    @RequestMapping(value = "/projects/{projectId}/current-graph", method = RequestMethod.PUT)
    public ResponseEntity<Map<String, Object>> getCurrentGraph(@PathVariable String projectId,
                                                               @Valid @RequestBody UpdateCurrentGraphBean item,
                                                               BindingResult result) {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("error", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        MetadataTx tx = metadataService.newTransaction();

        ProjectMetadata projectMetadata = tx.getProject(projectId);

        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find project '" + projectId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        GraphMetadata graphMetadata = tx.getGraph(item.getGraphId());

        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find graph '" + item.getGraphId() + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        if (projectMetadata != graphMetadata.getProject()) {
            response.put("status", "error");
            response.put("msg", "project does not own graph");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        projectMetadata.setCurrentGraph(graphMetadata);

        response.put("msg", "current graph changed");

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Map<String, Object> getGraphMap(GraphMetadata graphMetadata) {
        Map<String, Object> graph = new HashMap<>();

        graph.put("_id", graphMetadata.getId());
        graph.put("name", graphMetadata.getName());
        graph.put("backend", graphMetadata.getBackend());
        graph.put("directory", graphMetadata.getDirectory());
        graph.put("hostname", graphMetadata.getHostname());
        graph.put("port", graphMetadata.getPort());
        graph.put("tablename", graphMetadata.getTablename());

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata != null) {
            graph.put("projectId", graphMetadata.getProject().getId());
        }

        return graph;
    }
}
