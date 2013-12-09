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
import com.tinkerpop.blueprints.util.io.gml.GMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;

import org.lab41.dendrite.models.GraphMetadata;
import org.lab41.dendrite.rexster.DendriteRexsterApplication;
import org.lab41.dendrite.services.MetadataService;
import org.lab41.dendrite.services.MetadataTx;
import org.lab41.dendrite.web.beans.GraphImportBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class GraphImportController {

    static Logger logger = LoggerFactory.getLogger(GraphImportController.class);

    @Autowired
    DendriteRexsterApplication application;

    @Autowired
    MetadataService metadataService;

    @RequestMapping(value = "/api/graphs/{graphId}/file-import", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> importGraph(@PathVariable String graphId,
                                                           @Valid GraphImportBean item,
                                                           BindingResult result) {

        MetadataTx tx = metadataService.newTransaction();

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("status", "error");
            response.put("msg", result.toString());
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String format = item.getFormat();
        CommonsMultipartFile file = item.getFile();

        logger.debug("receiving file:", file.getOriginalFilename());
        logger.debug("file format:", format);

        GraphMetadata graphMetadata = tx.getGraph(graphId);
        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "cannot find graph metadata '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        String graphName = graphMetadata.getName();

        Graph graph = application.getGraph(graphName);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "cannot find graph '" + graphName + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        try {
            if (format.equalsIgnoreCase("GraphSON")) {
                GraphSONReader.inputGraph(graph, file.getInputStream());
            } else if (format.equalsIgnoreCase("GraphML")) {
                GraphMLReader.inputGraph(graph, file.getInputStream());
            } else if (format.equalsIgnoreCase("GML")) {
                GMLReader.inputGraph(graph, file.getInputStream());
            } else {
                response.put("status", "error");
                response.put("msg", "unknown format '" + format + "'");
                tx.rollback();
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        } catch(IOException err) {
            response.put("status", "error");
            response.put("msg", "exception: " + err.toString());
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        response.put("status", "ok");

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
