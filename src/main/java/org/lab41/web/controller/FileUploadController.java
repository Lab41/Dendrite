package org.lab41.web.controller;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;

import org.lab41.rexster.DendriteRexsterApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA. User: etryzelaar Date: 8/12/13 Time: 5:49 PM To change this template
 * use File | Settings | File Templates.
 */
@Controller
public class FileUploadController {

    @Autowired
    DendriteRexsterApplication application;

    @RequestMapping(value = "/api/{graphName}/fileupload", method = RequestMethod.POST)
    public ResponseEntity<String> upload(@PathVariable String graphName, FileUploadBean uploadItem, BindingResult result)
        throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        if (result.hasErrors()) {
            String json = "{\"status\": \"error\"}";
            return new ResponseEntity<String>(json, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (uploadItem.getFile() == null) {
            String json = "{\"status\": \"error\", \"message\": \"missing file\"}";
            return new ResponseEntity<String>(json, responseHeaders, HttpStatus.BAD_REQUEST);
        }
        System.err.println("filename: '" + uploadItem.getFile().getOriginalFilename() + "'");

        if (uploadItem.getFormat() == null) {
            String json = "{\"status\": \"error\", \"message\": \"missing format\"}";
            return new ResponseEntity<String>(json, responseHeaders, HttpStatus.BAD_REQUEST);
        }
        System.err.println("format: '" + uploadItem.getFormat());

        if (!uploadItem.getFormat().equalsIgnoreCase("GraphSON")) {
            String json = "{\"status\": \"error\", \"message\": \"unknown format\"}";
            return new ResponseEntity<String>(json, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        Graph graph = application.getGraph(graphName);
        if (graph == null) {
            String json = "{\"status\": \"error\", \"message\": \"unknown graph\"}";
            return new ResponseEntity<String>(json, responseHeaders, HttpStatus.BAD_REQUEST);
        }
        System.err.println("graph:" + graph);

        GraphSONReader.inputGraph(graph, uploadItem.getFile().getInputStream());

        String json = "{\"status\": \"ok\"}";
        return new ResponseEntity<String>(json, responseHeaders, HttpStatus.OK);
    }

}
