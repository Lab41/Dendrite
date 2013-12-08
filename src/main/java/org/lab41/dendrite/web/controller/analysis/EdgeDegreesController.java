package org.lab41.dendrite.web.controller.analysis;

import com.thinkaurelius.titan.core.TitanGraph;

import org.codehaus.jettison.json.JSONObject;
import org.lab41.dendrite.models.EdgeDegreesJobMetadata;
import org.lab41.dendrite.models.GraphMetadata;
import org.lab41.dendrite.models.JobMetadata;
import org.lab41.dendrite.models.ProjectMetadata;
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

import java.util.HashMap;
import java.util.Map;

@Controller
public class EdgeDegreesController {

    @Autowired
    MetadataService metadataService;

    @Autowired
    EdgeDegreesService edgeDegreesService;

    @RequestMapping(value = "/api/graphs/{graphId}/analysis/degrees", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> startJob(@PathVariable String graphId) throws Exception {

        Map<String, Object> response = new HashMap<>();

        GraphMetadata graphMetadata = metadataService.getGraph(graphId);
        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            metadataService.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        JobMetadata jobMetadata = metadataService.createJob(projectMetadata);

        edgeDegreesService.countDegrees(graphMetadata, jobMetadata);

        response.put("status", "ok");
        response.put("msg", "job submittied");
        response.put("href", "");

        metadataService.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
