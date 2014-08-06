package org.lab41.dendrite.web.controller.analysis;

import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.services.analysis.jung.*;
import org.lab41.dendrite.web.requests.PageRankRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Controller
public class JungController {

    @Autowired
    MetaGraphService metaGraphService;

    @Autowired
    BarycenterDistanceService barycenterDistanceService;

    @Autowired
    BetweennessCentralityService betweennessCentralityService;

    @Autowired
    ClosenessCentralityService closenessCentralityService;

    @Autowired
    EigenvectorCentralityService eigenvectorCentralityService;

    @Autowired
    PageRankService pageRankService;

    @PreAuthorize("hasPermission(#graphId, 'graph','admin')")
    @RequestMapping(value = "/api/graphs/{graphId}/analysis/jung/barycenter-distance", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> jungBarycenterDistance(@PathVariable String graphId) throws Exception {

        Map<String, Object> response = new HashMap<>();

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        MetaGraphTx tx = metaGraphService.newTransaction();

        GraphMetadata graphMetadata = tx.getGraph(graphId);
        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing project metadata for graph '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        barycenterDistanceService.run(graph, jobMetadata.getId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PreAuthorize("hasPermission(#graphId, 'graphId','admin')")
    @RequestMapping(value = "/api/graphs/{graphId}/analysis/jung/betweenness-centrality", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> jungBetweennessCentrality(@PathVariable String graphId) throws Exception {

        Map<String, Object> response = new HashMap<>();

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        MetaGraphTx tx = metaGraphService.newTransaction();

        GraphMetadata graphMetadata = tx.getGraph(graphId);
        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing project metadata for graph '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        betweennessCentralityService.run(graph, jobMetadata.getId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasPermission(#graphId, 'graph','admin')")
    @RequestMapping(value = "/api/graphs/{graphId}/analysis/jung/closeness-centrality", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> jungClosenessCentrality(@PathVariable String graphId) throws Exception {

        Map<String, Object> response = new HashMap<>();

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        MetaGraphTx tx = metaGraphService.newTransaction();

        GraphMetadata graphMetadata = tx.getGraph(graphId);
        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing project metadata for graph '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        closenessCentralityService.run(graph, jobMetadata.getId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasPermission(#graphId, 'graph','admin')")
    @RequestMapping(value = "/api/graphs/{graphId}/analysis/jung/eigenvector-centrality", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> jungEigenvectorCentrality(@PathVariable String graphId) throws Exception {

        Map<String, Object> response = new HashMap<>();

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        MetaGraphTx tx = metaGraphService.newTransaction();

        GraphMetadata graphMetadata = tx.getGraph(graphId);
        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing project metadata for graph '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        eigenvectorCentralityService.run(graph, jobMetadata.getId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasPermission(#graphId, 'graph','admin')")
    @RequestMapping(value = "/api/graphs/{graphId}/analysis/jung/pagerank", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> jungPageRank(@PathVariable String graphId,
                                                            @Valid @RequestBody PageRankRequest item,
                                                            BindingResult result) throws Exception {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("status", "error");
            response.put("msg", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        MetaGraphTx tx = metaGraphService.newTransaction();

        GraphMetadata graphMetadata = tx.getGraph(graphId);
        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing project metadata for graph '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        pageRankService.run(graph, jobMetadata.getId(), item.getAlpha());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
