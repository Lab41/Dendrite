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

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ElasticSearchController extends AbstractController {

    static Logger logger = LoggerFactory.getLogger(ElasticSearchController.class);

    @Autowired
    MetaGraphService metaGraphService;

    /**
     * Search a branch.
     *
     * @param branchId
     * @param body
     * @return
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    @PreAuthorize("hasPermission(#branchId, 'branch', 'admin')")
    @RequestMapping(value = "/api/branches/{branchId}/search", method = RequestMethod.POST)
    public ResponseEntity<String> branchSearch(@PathVariable String branchId,
                                               @RequestBody String body) throws JSONException, UnsupportedEncodingException {

        MetaGraphTx tx = metaGraphService.newTransaction();

        try {
            BranchMetadata branchMetadata = tx.getBranch(branchId);
            if (branchMetadata == null) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.APPLICATION_JSON);

                JSONObject json = new JSONObject();
                json.put("status", "error");
                json.put("msg", "unknown branch '" + branchId + "'");

                return new ResponseEntity<>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
            }

            GraphMetadata graphMetadata = branchMetadata.getGraph();
            if (graphMetadata == null) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.APPLICATION_JSON);

                JSONObject json = new JSONObject();
                json.put("status", "error");
                json.put("msg", "branch '" + branchId + "' does not point at a graph!");

                return new ResponseEntity<>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
            }

            return graphSearch(graphMetadata.getId().toString(), body);
        } finally {
            tx.rollback();
        }
    }

    /**
     * Search a graph.
     *
     * @param graphId
     * @param body
     * @return
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    @PreAuthorize("hasPermission(#graphId, 'graph', 'admin')")
    @RequestMapping(value = "/api/graphs/{graphId}/search", method = RequestMethod.POST)
    public ResponseEntity<String> graphSearch(@PathVariable String graphId,
                                              @RequestBody String body) throws JSONException, UnsupportedEncodingException {

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        DendriteGraph graph = metaGraphService.getDendriteGraph(graphId);
        if (graph == null) {
            JSONObject json = new JSONObject();
            json.put("status", "error");
            json.put("msg", "unknown graph '" + graphId + "'");

            return new ResponseEntity<>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        Client client = graph.getElasticSearchClient();
        if (client == null) {
            JSONObject json = new JSONObject();
            json.put("status", "error");
            json.put("msg", "graph does not have graph elasticsearch index");

            return new ResponseEntity<>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        SearchResponse response = client.prepareSearch(graph.getIndexName())
                .setSource(body)
                .execute()
                .actionGet();

        return new ResponseEntity<>(response.toString(), responseHeaders, HttpStatus.OK);
    }

    /**
     * Return the index mapping for a branch.
     *
     * @param branchId
     * @return
     * @throws JSONException
     * @throws IOException
     */
    @PreAuthorize("hasPermission(#branchId, 'branch', 'admin')")
    @RequestMapping(value = "/api/branches/{branchId}/search/mapping", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> branchMapping(@PathVariable String branchId) throws JSONException, IOException {

        MetaGraphTx tx = metaGraphService.newTransaction();

        try {
            BranchMetadata branchMetadata = tx.getBranch(branchId);
            if (branchMetadata == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("msg", "unknown branch '" + branchId + "'");

                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            GraphMetadata graphMetadata = branchMetadata.getGraph();
            if (graphMetadata == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("msg", "branch '" + branchId + "' does not point at a graph!");

                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            return graphMapping(graphMetadata.getId());
        } finally {
            tx.rollback();
        }
    }

    /**
     * Get the index mapping for the graph.
     *
     * @param graphId
     * @return
     * @throws JSONException
     * @throws IOException
     */
    @PreAuthorize("hasPermission(#graphId, 'graph', 'admin')")
    @RequestMapping(value = "/api/graphs/{graphId}/search/mapping", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> graphMapping(@PathVariable String graphId) throws JSONException, IOException {
        return graphMapping(new GraphMetadata.Id(graphId));
    }


    private ResponseEntity<Map<String, Object>> graphMapping(GraphMetadata.Id graphId) throws JSONException, IOException {

        Map<String, Object> response = new HashMap<>();

        DendriteGraph graph = metaGraphService.getDendriteGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "unknown graph '" + graphId + "'");

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Client client = graph.getElasticSearchClient();
        if (client == null) {
            response.put("status", "error");
            response.put("msg", "graph does not have graph elasticsearch index");

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        MetaData metaData = client.admin().cluster().prepareState()
                .setFilterIndices(graph.getIndexName())
                .execute()
                .actionGet()
                .getState()
                .getMetaData();

        for (IndexMetaData indexMetaData: metaData) {
            for (MappingMetaData mappingMetaData: indexMetaData.mappings().values()) {
                response.put(mappingMetaData.type(), mappingMetaData.sourceAsMap());
            }
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
