/**
 * Copyright 2013 In-Q-Tel/Lab41
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab41.dendrite.web.controller;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.util.io.gml.GMLWriter;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONWriter;

import org.codehaus.jettison.json.JSONObject;
import org.lab41.dendrite.rexster.DendriteRexsterApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.lab41.services.HistoryService;

import java.io.IOError;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

@Controller
public class GraphExportController {

    @Autowired
    DendriteRexsterApplication application;

    @Autowired
    HistoryService historyService;

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

    @RequestMapping(value = "/api/{graphName}/file-save", method = RequestMethod.POST)
    public void save(@PathVariable String graphName, GraphExportBean exportItem, HttpServletResponse response, BindingResult result) {

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

        // extract the storage location for the history
        String historyStorageLocation = historyService.getHistoryStorage();

        String format = exportItem.getFormat();
        try {

            if (format.equalsIgnoreCase("GraphSON")) {
                GraphSONWriter.outputGraph(graph, historyStorageLocation+"/"+graphName+".json");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else if (format.equalsIgnoreCase("GraphML")) {
                GraphMLWriter.outputGraph(graph, historyStorageLocation+"/"+graphName+".xml");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else if (format.equalsIgnoreCase("GML")) {
                GMLWriter.outputGraph(graph, historyStorageLocation+"/"+graphName+".gml");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

    }
}
