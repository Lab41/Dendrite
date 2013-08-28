package org.lab41.web.controller;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.util.io.gml.GMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.lab41.rexster.DendriteRexsterApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA. User: etryzelaar Date: 8/12/13 Time: 5:49 PM To change this template
 * use File | Settings | File Templates.
 */
@Controller
public class GraphImportController {

    @Autowired
    DendriteRexsterApplication application;

    @RequestMapping(value = "/api/{graphName}/file-import", method = RequestMethod.POST)
    public ResponseEntity<String> importGraph(@PathVariable String graphName, GraphImportBean importItem, BindingResult result)
        throws JSONException {
        HttpHeaders responseHeaders = new HttpHeaders();

        // FIXME: We can uncomment this once https://github.com/twilson63/ngUpload/pull/61 lands.
        //responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        responseHeaders.setContentType(MediaType.TEXT_HTML);

        JSONObject json = new JSONObject();

        if (result.hasErrors()) {
            json.put("status", "error");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (importItem.getFile() == null) {
            json.put("status", "error");
            json.put("msg", "missing 'file' field");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }
        System.err.println("filename: '" + importItem.getFile().getOriginalFilename() + "'");

        if (importItem.getFormat() == null) {
            json.put("status", "error");
            json.put("msg", "missing 'format' field");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }
        System.err.println("format: '" + importItem.getFormat());

        Graph graph = application.getGraph(graphName);
        if (graph == null) {
            json.put("status", "error");
            json.put("msg", "unknown graph '" + graphName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }
        System.err.println("graph:" + graph);

        String format = importItem.getFormat();
        try {
            if (format.equalsIgnoreCase("GraphSON")) {
                GraphSONReader.inputGraph(graph, importItem.getFile().getInputStream());
            } else if (format.equalsIgnoreCase("GraphML")) {
                GraphMLReader.inputGraph(graph, importItem.getFile().getInputStream());
            } else if (format.equalsIgnoreCase("GML")) {
                GMLReader.inputGraph(graph, importItem.getFile().getInputStream());
            } else {
                json.put("status", "error");
                json.put("msg", "unknown format '" + format + "'");
                return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
            }
        } catch(IOException err) {
            json.put("status", "error");
            json.put("msg", "exception: " + err.toString());
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        json.put("status", "ok");
        return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.OK);
    }

}
