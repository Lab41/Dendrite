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

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.services.MetaGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.StringBuilder;

@Controller
public class ElasticSearchController {

    @Autowired
    MetaGraphService metaGraphService;

    @RequestMapping(value = "/api/graphs/{graphId}/search", method = RequestMethod.POST)
    public ResponseEntity<String> elasticSearch(@PathVariable String graphId,
                                                @RequestBody String body) throws JSONException, UnsupportedEncodingException {

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject json = new JSONObject();

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            json.put("status", "error");
            json.put("msg", "unknown graph '" + graphId + "'");
            return new ResponseEntity<>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        Configuration config = graph.getConfiguration();

        String elasticSearchHost = config.getString("storage.index.search.hostname", null);
        String elasticSearchIndexName = config.getString("storage.index.search.index-name", null);

        if (config.getBoolean("storage.index.search.local-mode", false)) {
            elasticSearchHost = "localhost";
        }

        // if it didn't find either index hostname or local-mode for the specified index
        if (elasticSearchHost == null || elasticSearchIndexName == null) {
            json.put("status", "error");
            json.put("msg", "could not find ElasticSearch index for '" + graphId + "'");
            return new ResponseEntity<>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // build the elasticsearch url
        StringBuilder stringBuilderElasticSearchURL = new StringBuilder();
        stringBuilderElasticSearchURL.append("http://");
        stringBuilderElasticSearchURL.append(elasticSearchHost);
        stringBuilderElasticSearchURL.append(":9200/");
        stringBuilderElasticSearchURL.append(elasticSearchIndexName);
        stringBuilderElasticSearchURL.append("/_search");
        String elasticSearchURL = stringBuilderElasticSearchURL.toString();

        // decode url-encoded json response back into json
        String decodedString = java.net.URLDecoder.decode(body, "UTF-8");
        JSONObject jsonQuery = new JSONObject(decodedString);
        JSONObject jsonResult = null;

        // pass the query directly to elasticsearch and return that result
        HttpClient httpClient = new DefaultHttpClient();

        try {
            HttpPost request = new HttpPost(elasticSearchURL);
            StringEntity params = new StringEntity(jsonQuery.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);

            HttpEntity entity  = response.getEntity();
            InputStream is = entity.getContent();

            StringBuilder stringBuilderContent = new StringBuilder();
            int i;
            char c;
            // reads till the end of the stream
            while((i=is.read())!=-1)
            {
                // converts integer to character
                c=(char)i;
                stringBuilderContent.append(c);
            }
            String content = stringBuilderContent.toString();
            jsonResult = new JSONObject(content);
        }catch (Exception ex) {
            json.put("status", "error");
            json.put("msg", "error retrieving query from ElasticSearch");
            return new ResponseEntity<>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        return new ResponseEntity<>(jsonResult.toString(), responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/graphs/{graphId}/search/mapping", method = RequestMethod.GET)
    public ResponseEntity<String> elasticSearchMapping(@PathVariable String graphId) throws JSONException {

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject json = new JSONObject();

        DendriteGraph graph = metaGraphService.getGraph(graphId);
        if (graph == null) {
            json.put("status", "error");
            json.put("msg", "unknown graph '" + graphId + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        Configuration config = graph.getConfiguration();

        String elasticSearchHost = config.getString("storage.index.search.hostname", null);
        String elasticSearchIndexName = config.getString("storage.index.search.index-name", null);

        if (config.getBoolean("storage.index.search.local-mode", false)) {
            elasticSearchHost = "localhost";
        }

        // if it didn't find either index hostname or local-mode for the specified index
        if (elasticSearchHost == null || elasticSearchIndexName == null) {
            json.put("status", "error");
            json.put("msg", "could not find ElasticSearch index for '" + graphId + "'");
            return new ResponseEntity<>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // build the elasticsearch url
        StringBuilder stringBuilderElasticSearchURL = new StringBuilder();
        stringBuilderElasticSearchURL.append("http://");
        stringBuilderElasticSearchURL.append(elasticSearchHost);
        stringBuilderElasticSearchURL.append(":9200/" + elasticSearchIndexName + "/_mapping");
        String elasticSearchURL = stringBuilderElasticSearchURL.toString();

        // object for json response back into json
        JSONObject jsonResult = null;

        // pass the query directly to elasticsearch and return that result
        HttpClient httpClient = new DefaultHttpClient();

        try {
            HttpGet request = new HttpGet(elasticSearchURL);
            HttpResponse response = httpClient.execute(request);

            HttpEntity entity  = response.getEntity();
            InputStream is = entity.getContent();

            StringBuilder stringBuilderContent = new StringBuilder();
            int i;
            char c;
            // reads till the end of the stream
            while((i=is.read())!=-1)
            {
                // converts integer to character
                c=(char)i;
                stringBuilderContent.append(c);
            }
            String content = stringBuilderContent.toString();
            jsonResult = new JSONObject(content);
        }catch (Exception ex) {
            json.put("status", "error");
            json.put("msg", "error retrieving query from ElasticSearch");
            return new ResponseEntity<>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        jsonResult = jsonResult.getJSONObject(elasticSearchIndexName);

        // return
        return new ResponseEntity<>(jsonResult.toString(), responseHeaders, HttpStatus.OK);
    }
}
