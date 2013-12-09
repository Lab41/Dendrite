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

@Controller
public class GraphSaveController {

    @Autowired
    DendriteRexsterApplication application;

    @RequestMapping(value = "/api/{graphName}/file-save", method = RequestMethod.POST)
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
            String filename = "/home/dendrite/kylef/dendrite-db/graph";

            if (format.equalsIgnoreCase("GraphSON")) {
                GraphSONWriter.outputGraph(graph, filename+".json");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else if (format.equalsIgnoreCase("GraphML")) {
                GraphMLWriter.outputGraph(graph, filename+".xml");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else if (format.equalsIgnoreCase("GML")) {
                GMLWriter.outputGraph(graph, filename+".gml");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
