package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.models.GraphMetadata;
import org.lab41.dendrite.models.ProjectMetadata;
import org.lab41.dendrite.services.MetadataService;
import org.lab41.dendrite.web.beans.GraphBean;
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
    public ResponseEntity<List<Map<String, Object>>> getGraphs() {

        List<Map<String, Object>> graphs = new ArrayList<>();
        for (GraphMetadata graphMetadata: metadataService.getGraphs()) {
            graphs.add(getGraphMap(graphMetadata));
        }

        return new ResponseEntity<>(graphs, HttpStatus.OK);
    }

    @RequestMapping(value = "/graphs/{graphId}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getGraph(@PathVariable String graphId) {

        GraphMetadata graphMetadata = metadataService.getGraph(graphId);

        if (graphMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("graph", getGraphMap(graphMetadata));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/graphs/{graphId}", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> deleteGraph(@PathVariable String graphId) throws Exception {

        GraphMetadata graphMetadata = metadataService.getGraph(graphId);

        if (graphMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        metadataService.deleteGraph(graphMetadata);
        metadataService.commit();

        Map<String, Object> response = new HashMap<>();
        response.put("msg", "deleted");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/graphs", method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> getGraphs(@PathVariable String projectId) {

        ProjectMetadata projectMetadata = metadataService.getProject(projectId);

        if (projectMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<Map<String, Object>> graphs = new ArrayList<>();
        for (GraphMetadata graphMetadata: projectMetadata.getGraphs()) {
            graphs.add(getGraphMap(graphMetadata));
        }

        return new ResponseEntity<>(graphs, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}/graphs", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createGraph(@PathVariable String projectId,
                                                           @Valid @RequestBody GraphBean item,
                                                           BindingResult result,
                                                           UriComponentsBuilder builder) {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("error", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        ProjectMetadata projectMetadata = metadataService.getProject(projectId);

        if (projectMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        GraphMetadata graphMetadata = metadataService.createGraph(projectMetadata);
        metadataService.commit();

        response.put("graph", getGraphMap(graphMetadata));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/graphs/{graphId}", method = RequestMethod.PUT)
    public ResponseEntity<Map<String, Object>> updateGraph(@PathVariable String graphId,
                                                           @Valid @RequestBody GraphBean item,
                                                           BindingResult result) {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("error", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        GraphMetadata graphMetadata = metadataService.getGraph(graphId);

        if (graphMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        graphMetadata.setName(item.getName());
        graphMetadata.setBackend(item.getBackend());
        graphMetadata.setDirectory(item.getDirectory());
        graphMetadata.setHostname(item.getHostname());
        graphMetadata.setPort(item.getPort());
        graphMetadata.setTablename(item.getTablename());
        metadataService.commit();

        response.put("graph", getGraphMap(graphMetadata));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @RequestMapping(value = "/projects/{projectId}/current-graph", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getCurrentGraph(@PathVariable String projectId) {

        ProjectMetadata projectMetadata = metadataService.getProject(projectId);

        if (projectMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Map<String, Object> response = new HashMap<>();

        GraphMetadata graphMetadata = projectMetadata.getCurrentGraph();
        response.put("graph", getGraphMap(graphMetadata));

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

        ProjectMetadata projectMetadata = metadataService.getProject(projectId);

        if (projectMetadata == null) {
            response.put("error", "project does not exist");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        GraphMetadata graphMetadata = metadataService.getGraph(item.getGraphId());

        if (graphMetadata == null) {
            response.put("error", "graph does not exist");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        if (projectMetadata != graphMetadata.getProject()) {
            response.put("error", "project does not own graph");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        projectMetadata.setCurrentGraph(graphMetadata);
        metadataService.commit();

        response.put("msg", "current graph changed");

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
