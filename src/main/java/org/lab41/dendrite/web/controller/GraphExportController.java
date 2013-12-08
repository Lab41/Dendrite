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

import org.lab41.dendrite.models.GraphMetadata;
import org.lab41.dendrite.rexster.DendriteRexsterApplication;
import org.lab41.dendrite.services.MetadataService;
import org.lab41.dendrite.web.beans.GraphExportBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Controller
public class GraphExportController {

    static Logger logger = LoggerFactory.getLogger(GraphExportController.class);

    @Autowired
    DendriteRexsterApplication application;

    @Autowired
    MetadataService metadataService;

    @RequestMapping(value = "/api/graphs/{graphId}/file-export", method = RequestMethod.POST)
    public ResponseEntity<byte[]> export(@PathVariable String graphId,
                                         @Valid @RequestBody GraphExportBean item,
                                         BindingResult result) {

        if (result.hasErrors()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        String format = item.getFormat();

        GraphMetadata graphMetadata = metadataService.getGraph(graphId);
        if (graphMetadata == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String graphName = graphMetadata.getName();
        logger.debug("exporting graph '" + graphName + "'");

        Graph graph = application.getGraph(graphName);
        if (graph == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        HttpHeaders headers = new HttpHeaders();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            if (format.equalsIgnoreCase("GraphSON")) {
                headers.setContentType(new MediaType("application", "vnd.rexster+json"));
                headers.set("Content-Disposition", "attachment; filename=\"graph.json\"");

                GraphSONWriter.outputGraph(graph, byteArrayOutputStream);
            } else if (format.equalsIgnoreCase("GraphML")) {
                headers.setContentType(new MediaType("application", "vnd.rexster+xml"));
                headers.set("Content-Disposition", "attachment; filename=\"graph.xml\"");

                GraphMLWriter.outputGraph(graph, byteArrayOutputStream);
            } else if (format.equalsIgnoreCase("GML")) {
                headers.setContentType(new MediaType("application", "vnd.rexster+gml"));
                headers.set("Content-Disposition", "attachment; filename=\"graph.gml\"");

                GMLWriter.outputGraph(graph, byteArrayOutputStream);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
    }

}
