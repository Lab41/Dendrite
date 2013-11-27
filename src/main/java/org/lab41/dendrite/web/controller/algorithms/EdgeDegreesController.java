package org.lab41.dendrite.web.controller.algorithms;

import com.thinkaurelius.titan.core.TitanGraph;

import org.codehaus.jettison.json.JSONObject;
import org.lab41.dendrite.rexster.DendriteRexsterApplication;
import org.lab41.dendrite.services.algorithms.EdgeDegreesService;
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
    EdgeDegreesService edgeDegreesService;

    @RequestMapping(value = "/api/{graphName}/algorithms/degrees", method = RequestMethod.GET)
    public ResponseEntity<String> pageRank(@PathVariable String graphName) throws Exception {
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

        edgeDegreesService.countDegrees(graphName, graph);

        return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.OK);
    }

}
