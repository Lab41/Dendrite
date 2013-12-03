package org.lab41.dendrite.web.controller.analysis;

import com.thinkaurelius.titan.core.TitanGraph;

import org.codehaus.jettison.json.JSONObject;
import org.lab41.dendrite.models.Job;
import org.lab41.dendrite.rexster.DendriteRexsterApplication;
import org.lab41.dendrite.services.MetadataService;
import org.lab41.dendrite.services.analysis.EdgeDegreesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class EdgeDegreesController {

    @Autowired
    DendriteRexsterApplication application;

    @Autowired
    MetadataService metadataService;

    @Autowired
    EdgeDegreesService edgeDegreesService;

    @RequestMapping(value = "/api/{graphName}/analysis/degrees", method = RequestMethod.GET)
    public ResponseEntity<String> degrees(@PathVariable String graphName) throws Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject json = new JSONObject();

        TitanGraph graph = (TitanGraph) application.getGraph(graphName);

        if (graph == null) {
            json.put("status", "error");
            json.put("msg", "unknown graph '" + graphName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (!(graph instanceof TitanGraph)) {
            json.put("status", "error");
            json.put("error", "graph is not a titan graph");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        Job job = metadataService.getJob(graphName, "degrees");
        String status = job.getStatus();

        if (status.equals("none")) {
            status = "queued";
            job.setStatus(status);
            metadataService.commit();

            edgeDegreesService.countDegrees(graphName, graph, job);

            json.put("status", status);

            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.OK);
        } else {
            json.put("status", "error");
            json.put("error", "job is already running with status '" + status + "'");

            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/api/{graphName}/analysis/degrees/status", method = RequestMethod.GET)
    public ResponseEntity<String> status(@PathVariable String graphName) throws Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject json = new JSONObject();

        TitanGraph graph = (TitanGraph) application.getGraph(graphName);

        if (graph == null) {
            json.put("status", "error");
            json.put("msg", "unknown graph '" + graphName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (!(graph instanceof TitanGraph)) {
            json.put("status", "error");
            json.put("error", "graph is not a titan graph");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        Job job = metadataService.getJob(graphName, "degrees");
        json.put("status", job.getStatus());

        return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/{graphName}/analysis/degrees/cancel", method = RequestMethod.GET)
    public ResponseEntity<String> cancel(@PathVariable String graphName) throws Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject json = new JSONObject();

        TitanGraph graph = (TitanGraph) application.getGraph(graphName);

        if (graph == null) {
            json.put("status", "error");
            json.put("msg", "unknown graph '" + graphName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (!(graph instanceof TitanGraph)) {
            json.put("status", "error");
            json.put("error", "graph is not a titan graph");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        Job job = metadataService.getJob(graphName, "degrees");
        job.setStatus("none");
        metadataService.commit();

        json.put("status", "none");

        return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.OK);
    }

}
