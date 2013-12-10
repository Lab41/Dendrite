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
import org.lab41.dendrite.services.HistoryService;
import org.lab41.dendrite.services.MetadataService;
import org.lab41.dendrite.services.MetadataTx;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class GraphExportController {

    static Logger logger = LoggerFactory.getLogger(GraphExportController.class);

    @Autowired
    DendriteRexsterApplication application;

    @Autowired
    MetadataService metadataService;

    @Autowired
    HistoryService historyService;

    @RequestMapping(value = "/api/graphs/{graphId}/file-export", method = RequestMethod.POST)
    public ResponseEntity<byte[]> export(@PathVariable String graphId,
                                         @Valid GraphExportBean item,
                                         BindingResult result) {

        if (result.hasErrors()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        MetadataTx tx = metadataService.newTransaction();
        GraphMetadata graphMetadata = tx.getGraph(graphId);

        if (graphMetadata == null) {
            tx.rollback();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String graphName = graphMetadata.getName();
        tx.commit();

        logger.debug("exporting graph '" + graphName + "'");

        Graph graph = application.getGraph(graphName);
        if (graph == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String format = item.getFormat();
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

    @RequestMapping(value = "/api/graphs/{graphId}/file-save", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> save(@PathVariable String graphId,
                                                    @Valid GraphExportBean item,
                                                    BindingResult result) {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("status", "error");
            response.put("msg", result.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        MetadataTx tx = metadataService.newTransaction();
        GraphMetadata graphMetadata = tx.getGraph(graphId);

        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "cannot find graph metadata '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        String graphName = graphMetadata.getName();
        tx.commit();

        logger.debug("exporting graph '" + graphName + "'");

        Graph graph = application.getGraph(graphName);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "cannot find graph '" + graphName + "'");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // extract the storage location for the history
        String historyStorageLocation = historyService.getHistoryStorage();
        String format = item.getFormat();

        try {
            if (format.equalsIgnoreCase("GraphSON")) {
                GraphSONWriter.outputGraph(graph, historyStorageLocation+"/"+graphName+".json");
            } else if (format.equalsIgnoreCase("GraphML")) {
                GraphMLWriter.outputGraph(graph, historyStorageLocation+"/"+graphName+".xml");
            } else if (format.equalsIgnoreCase("GML")) {
                GMLWriter.outputGraph(graph, historyStorageLocation+"/"+graphName+".gml");
            } else {
                response.put("status", "error");
                response.put("msg", "unknown format '" + format + "'");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            response.put("status", "error");
            response.put("msg", "exception: " + e.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        response.put("status", "ok");

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
