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

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.util.io.gml.GMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import org.lab41.dendrite.metagraph.DendriteGraph;
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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

@Controller
public class GraphImportController {

    static Logger logger = LoggerFactory.getLogger(GraphImportController.class);

    @Autowired
    MetaGraphService metaGraphService;

    // faunus->standard graphSON converter
    // modified from https://github.com/WhySearchTwice/Data-and-Rest-Services
    private static JSONObject loadAndTransformFaunusFormat(InputStream inputStream) throws Exception {
        JSONArray edges = new JSONArray();
        JSONArray vertices = new JSONArray();

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = br.readLine()) != null) {
            JSONObject json = new JSONObject(line);

            if (json.has("_inE")) {
                // create edges object
                JSONArray inE = json.getJSONArray("_inE");
                for (int i = 0; i < inE.length(); i++) {
                  JSONObject edge = inE.getJSONObject(i);
                  edge.put("_inV", json.get("_id"));
                  edges.put(edge);
                }

                // remove edges from intended vertex list
                json.remove("_inE");
            }

            if (json.has("_outE")) {

              // create edges object
              JSONArray outE = json.getJSONArray("_outE");
              for (int i = 0; i < outE.length(); i++) {
                JSONObject edge = outE.getJSONObject(i);
                edge.put("_outV", json.get("_id"));
                edges.put(edge);
              }

              // remove edges from intended vertex list
              json.remove("_outE");
            }

            // create vertex object
            vertices.put(json);
        }
        br.close();

        JSONObject graphJSON = new JSONObject();
        graphJSON.put("mode", "NORMAL");
        graphJSON.put("vertices", vertices);
        graphJSON.put("edges", edges);
        JSONObject graphWrapper = new JSONObject();
        graphWrapper.put("graph", graphJSON);
        return graphWrapper;
    }

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
        String searchkeys = item.getSearchkeys();
        CommonsMultipartFile file = item.getFile();

        logger.debug("receiving file:", file.getOriginalFilename());
        logger.debug("file format:", format);
        logger.debug("search keys: "+searchkeys);

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "cannot find graph '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        try {

            // create search indices
            TitanGraph titanGraph = graph.getTitanGraph();
            String elasticSearchIndex = "search";
            String[] reservedKeys = {"id", "_id"};
            if (titanGraph != null && searchkeys.indexOf(",") != -1) {

              // separate "k1,k2,k3" into ["k1", "k2", "k3"] and iterate
              String[] searchKeysArray = searchkeys.split(",");
              for(int i = 0; i<searchKeysArray.length; i++) {

                // create the search index (if it doesn't already exist and isn't a reserved key)
                if (titanGraph.getType(searchKeysArray[i]) == null && !Arrays.asList(reservedKeys).contains(searchKeysArray[i])) {
                  titanGraph.makeKey(searchKeysArray[i]).dataType(String.class).indexed(Vertex.class).indexed(elasticSearchIndex, Vertex.class).make();
                }

              }
            }

            InputStream inputStream = file.getInputStream();
            if (format.equalsIgnoreCase("GraphSON")) {
                GraphSONReader.inputGraph(graph, inputStream);
            } else if (format.equalsIgnoreCase("GraphML")) {
                GraphMLReader.inputGraph(graph, inputStream);
            } else if (format.equalsIgnoreCase("GML")) {
                GMLReader.inputGraph(graph, inputStream);
            } else if (format.equalsIgnoreCase("FaunusGraphSON")) {
                JSONObject json = loadAndTransformFaunusFormat(inputStream);
                InputStream in = new ByteArrayInputStream(json.toString().getBytes());
                GraphSONReader.inputGraph(graph, in);
                in.close();
            } else {
                response.put("status", "error");
                response.put("msg", "unknown format '" + format + "'");
                inputStream.close();
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            inputStream.close();
        } catch(Exception e) {
            response.put("status", "error");
            response.put("msg", "exception: " + e.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        response.put("status", "ok");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
