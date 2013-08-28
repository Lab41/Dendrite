package org.lab41.web.controller;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.util.io.gml.GMLWriter;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONWriter;

import org.codehaus.jettison.json.JSONObject;
import org.lab41.rexster.DendriteRexsterApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOError;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA. User: etryzelaar Date: 8/28/13 Time: 9:38 AM To change this template
 * use File | Settings | File Templates.
 */

@Controller
public class GraphExportController {

    @Autowired
    DendriteRexsterApplication application;

    @RequestMapping(value = "/api/{graphName}/file-export", method = RequestMethod.POST)
    public void export(@PathVariable String graphName, GraphExportBean exportItem, HttpServletResponse response, BindingResult result) {

        JSONObject json = new JSONObject();

        if (result.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (exportItem.getFormat() == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Graph graph = application.getGraph(graphName);
        if (graph == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String format = exportItem.getFormat();
        try {
            if (format.equalsIgnoreCase("GraphSON")) {
                response.setContentType("application/vnd.rexster+json");
                response.setHeader("Content-Disposition", "attachment; filename=\"graph.json\"");
                GraphSONWriter.outputGraph(graph, response.getOutputStream());
            } else if (format.equalsIgnoreCase("GraphML")) {
                response.setContentType("application/vnd.rexster+xml");
                response.setHeader("Content-Disposition", "attachment; filename=\"graph.xml\"");
                GraphMLWriter.outputGraph(graph, response.getOutputStream());
            } else if (format.equalsIgnoreCase("GML")) {
                response.setContentType("application/vnd.rexster+gml");
                response.setHeader("Content-Disposition", "attachment; filename=\"graph.gml\"");
                GMLWriter.outputGraph(graph, response.getOutputStream());
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

}
