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

import com.tinkerpop.blueprints.util.io.gml.GMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;
import org.lab41.dendrite.util.io.faunusgraphson.FaunusGraphSONReader;

import com.tinkerpop.blueprints.Vertex;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.DendriteGraphTx;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.beans.GraphImportBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

@Controller
public class GraphImportController {

    static Logger logger = LoggerFactory.getLogger(GraphImportController.class);

    static List<String> RESERVED_KEYS = Arrays.asList("id", "_id");
    private final String searchIndexId = "vertexId";
    private final String elasticSearchIndex = "search";

    @Autowired
    MetaGraphService metaGraphService;

    @RequestMapping(value = "/api/graphs/{graphId}/file-import", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> importGraph(@PathVariable String graphId,
                                                           @Valid GraphImportBean item,
                                                           BindingResult result) {

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("status", "error");
            response.put("msg", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String format = item.getFormat();
        String searchKeys = item.getSearchkeys();
        CommonsMultipartFile file = item.getFile();

        logger.debug("receiving file:", file.getOriginalFilename());
        logger.debug("file format:", format);
        logger.debug("search keys: "+searchKeys);

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "cannot find graph '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        DendriteGraphTx tx = graph.newTransaction();

        try {
            // create search indices
            String elasticSearchIndex = "search";
            if (searchKeys.contains(",")) {
                // separate and iterate over "key1=type1,key2=type2"
                for (String keyType : searchKeys.split(",")) {
                    String[] tokens = keyVal.split("=");
                    String key = tokens[0];
                    String type = tokens[1];

                    //TODO: make dataType below based on type passed from client

                    // create the search index (if it doesn't already exist and isn't a reserved key)
                    if (tx.getType(key) == null && !RESERVED_KEYS.contains(key)) {
                        tx.makeKey(key)
                                .dataType(String.class)
                                .indexed(Vertex.class)
                                .indexed(elasticSearchIndex, Vertex.class)
                                .make();
                    }
                }
            }

            // elasticsearch uses a unique id separate from the titan id
            // in order to link elasticsearch results to titan results,
            // create a new index for the frontend vertexId
            tx.makeKey(searchIndexId)
                    .dataType(String.class)
                    .indexed(Vertex.class)
                    .indexed(elasticSearchIndex, Vertex.class)
                    .make();

            InputStream inputStream = file.getInputStream();
            if (format.equalsIgnoreCase("GraphSON")) {
                GraphSONReader.inputGraph(tx, inputStream);
            } else if (format.equalsIgnoreCase("GraphML")) {
                GraphMLReader.inputGraph(tx, inputStream);
            } else if (format.equalsIgnoreCase("GML")) {
                GMLReader.inputGraph(tx, inputStream);
            } else if (format.equalsIgnoreCase("FaunusGraphSON")) {
                FaunusGraphSONReader.inputGraph(tx, inputStream);
            } else {
                tx.rollback();

                response.put("status", "error");
                response.put("msg", "unknown format '" + format + "'");
                inputStream.close();
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            inputStream.close();
        } catch (IOException e) {
            tx.rollback();

            response.put("status", "error");
            response.put("msg", "exception: " + e.toString());

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // We don't need to commit the transaction as the readers already do that for us.

        response.put("status", "ok");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
