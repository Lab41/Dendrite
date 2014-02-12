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

import com.thinkaurelius.titan.core.Mapping;
import com.thinkaurelius.titan.core.Parameter;
import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.thinkaurelius.titan.core.attribute.FullFloat;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.tinkerpop.blueprints.util.io.gml.GMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;
import org.lab41.dendrite.util.io.faunusgraphson.FaunusGraphSONReader;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.Edge;
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

import org.json.JSONArray;
import org.json.JSONObject;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;

@Controller
public class GraphImportController {

    static Logger logger = LoggerFactory.getLogger(GraphImportController.class);

    static List<String> RESERVED_KEYS = Arrays.asList("id", "_id");

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

        logger.debug("receiving file: '" +  file.getOriginalFilename() + "'");
        logger.debug("file format: '" +  format + "'");
        logger.debug("search keys: '" + searchKeys + "'");

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "cannot find graph '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        try {
            // extract search indices from client JSON
            JSONObject jsonKeys = new JSONObject(searchKeys);
            JSONArray jsonVertices = jsonKeys.getJSONArray("vertices");
            JSONArray jsonEdges = jsonKeys.getJSONArray("edges");

            // build search indices for vertices
            for (int i = 0; i < jsonVertices.length(); ++i){
              JSONObject jsonVertex = jsonVertices.getJSONObject(i);
              String key = jsonVertex.getString("name");
              String type = jsonVertex.getString("type");

              // create the search index (if it doesn't already exist and isn't a reserved key)
              if (graph.getType(key) == null && !RESERVED_KEYS.contains(key)) {
                  Class cls;
                  List<Parameter> parameters = new ArrayList<>();

                  if (type.equals("string")) {
                      cls = String.class;
                      parameters.add(Parameter.of(Mapping.MAPPING_PREFIX, Mapping.STRING));
                  } else if (type.equals("text")) {
                      cls = String.class;
                      parameters.add(Parameter.of(Mapping.MAPPING_PREFIX, Mapping.TEXT));
                  } else if (type.equals("integer")) {
                      cls = Integer.class;
                  } else if (type.equals("float")) {
                      cls = FullFloat.class;
                  } else if (type.equals("double")) {
                      cls = FullDouble.class;
                  } else if (type.equals("geocoordinate")) {
                      cls = Geoshape.class;
                  } else {
                      graph.rollback();
                      response.put("status", "error");
                      response.put("msg", "unknown type '" + type + "'");
                      return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                  }

                  graph.makeKey(key)
                          .dataType(cls)
                          .indexed(Vertex.class)
                          .indexed(DendriteGraph.INDEX_NAME, Vertex.class, parameters.toArray(new Parameter[parameters.size()]))
                          .make();
              }
            }

            // build search indices for edges
            for (int i = 0; i < jsonEdges.length(); ++i){
              JSONObject jsonEdge = jsonEdges.getJSONObject(i);
              String key = jsonEdge.getString("name");
              String type = jsonEdge.getString("type");

              // create the search index (if it doesn't already exist and isn't a reserved key)
              if (graph.getType(key) == null && !RESERVED_KEYS.contains(key)) {
                  Class cls;
                  List<Parameter> parameters = new ArrayList<>();

                  if (type.equals("string")) {
                      cls = String.class;
                      parameters.add(Parameter.of(Mapping.MAPPING_PREFIX, Mapping.STRING));
                  } else if (type.equals("text")) {
                      cls = String.class;
                      parameters.add(Parameter.of(Mapping.MAPPING_PREFIX, Mapping.TEXT));
                  } else if (type.equals("integer")) {
                      cls = Integer.class;
                  } else if (type.equals("float")) {
                      cls = FullFloat.class;
                  } else if (type.equals("double")) {
                      cls = FullDouble.class;
                  } else if (type.equals("geocoordinate")) {
                      cls = Geoshape.class;
                  } else {
                      graph.rollback();
                      response.put("status", "error");
                      response.put("msg", "unknown type '" + type + "'");
                      return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                  }

                  graph.makeKey(key)
                          .dataType(cls)
                          .indexed(Edge.class)
                          .indexed(DendriteGraph.INDEX_NAME, Edge.class, parameters.toArray(new Parameter[parameters.size()]))
                          .make();
              }
            }

            // commit the indices
            graph.commit();

            InputStream inputStream = file.getInputStream();
            if (format.equalsIgnoreCase("GraphSON")) {
                GraphSONReader.inputGraph(graph, inputStream);
            } else if (format.equalsIgnoreCase("GraphML")) {
                GraphMLReader.inputGraph(graph, inputStream);
            } else if (format.equalsIgnoreCase("GML")) {
                GMLReader.inputGraph(graph, inputStream);
            } else if (format.equalsIgnoreCase("FaunusGraphSON")) {
                FaunusGraphSONReader.inputGraph(graph, inputStream);
            } else {
                graph.rollback();

                response.put("status", "error");
                response.put("msg", "unknown format '" + format + "'");
                inputStream.close();
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            inputStream.close();
        } catch (Throwable t) {
            t.printStackTrace();

            graph.rollback();

            response.put("status", "error");
            response.put("msg", "exception: " + t.toString());

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // We don't need to commit the transaction as the readers already do that for us.

        response.put("status", "ok");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
