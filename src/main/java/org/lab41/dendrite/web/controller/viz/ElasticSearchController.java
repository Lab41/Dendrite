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

package org.lab41.dendrite.web.controller.viz;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.diskstorage.es.ElasticSearchIndex;
import com.thinkaurelius.titan.graphdb.database.StandardTitanGraph;
import com.tinkerpop.blueprints.Graph;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.lab41.dendrite.rexster.DendriteRexsterApplication;
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
import java.lang.StringBuilder;
import java.util.Iterator;

@Controller
public class ElasticSearchController {

    @Autowired
    DendriteRexsterApplication application;

    @RequestMapping(value = "/api/{graphName}/viz/{indexName}/{esName}", method = RequestMethod.POST)
    public ResponseEntity<String> elasticSearch(@RequestBody String body,
                                                @PathVariable String graphName,
                                                @PathVariable String indexName,
                                                @PathVariable String esName) throws Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject json = new JSONObject();

        Graph graph = application.getGraph(graphName);
        HierarchicalConfiguration conf = application.getStorageConfig(graphName);

        String elasticSearchHost = null;
        String elasticSearchName = null;

        if (graph == null) {
            json.put("status", "error");
            json.put("msg", "unknown graph '" + graphName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (!(graph instanceof TitanGraph)) {
            json.put("status", "error");
            json.put("error", "graph is not a titan graph");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (indexName == null) {
            json.put("status", "error");
            json.put("msg", "unknown index '" + indexName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (esName == null) {
            json.put("status", "error");
            json.put("msg", "unknown elasticsearch index '" + esName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // build the index hostname key
        StringBuilder stringBuilderIndexHost = new StringBuilder();
        stringBuilderIndexHost.append("storage..index..");
        stringBuilderIndexHost.append(indexName);
        stringBuilderIndexHost.append("..hostname");
        String indexHost = stringBuilderIndexHost.toString();

        // build the index local-mode key
        StringBuilder stringBuilderLocalMode = new StringBuilder();
        stringBuilderLocalMode.append("storage..index..");
        stringBuilderLocalMode.append(indexName);
        stringBuilderLocalMode.append("..local-mode");
        String localMode = stringBuilderLocalMode.toString();

        // build the index-name key
        StringBuilder stringBuilderIndexName = new StringBuilder();
        stringBuilderIndexName.append("storage..index..");
        stringBuilderIndexName.append(indexName);
        stringBuilderIndexName.append("..index-name");
        String esIndexName = stringBuilderIndexName.toString();

        // check rexster keys
        boolean flag = false;
        Iterator<String> rexsterConfigurationKeys = conf.getKeys();
        while (rexsterConfigurationKeys.hasNext()) {
            String key = rexsterConfigurationKeys.next();
            if (key.equals(indexHost)) {
                elasticSearchHost = conf.getString(key);
                flag = true;
            }
            else if (key.equals(localMode)) {
                elasticSearchHost = "127.0.0.1";
                flag = true;
            }
        }

        // if it didn't find either index hostname or local-mode for the specified index
        if (!flag) {
            json.put("status", "error");
            json.put("msg", "ElasticSearch index not found '" + indexName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // get elasticsearch index name
        flag = false;
        rexsterConfigurationKeys = conf.getKeys();
        while (rexsterConfigurationKeys.hasNext()) {
            String key = rexsterConfigurationKeys.next();
            if (key.equals(esIndexName)) {
                elasticSearchName = conf.getString(key);
                flag = true;
            }
        }
        if (!flag) {
            elasticSearchName = "titan";
        }

        // build the elasticsearch url
        StringBuilder stringBuilderElasticSearchURL = new StringBuilder();
        stringBuilderElasticSearchURL.append("http://");
        stringBuilderElasticSearchURL.append(elasticSearchHost);
        stringBuilderElasticSearchURL.append(":9200/");
        stringBuilderElasticSearchURL.append(elasticSearchName);
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
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        return new ResponseEntity<String>(jsonResult.toString(), responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/{graphName}/viz/{indexName}/{esName}/facets", method = RequestMethod.GET)
    public ResponseEntity<String> elasticSearchFacets(@RequestBody String body,
                                                      @PathVariable String graphName,
                                                      @PathVariable String indexName,
                                                      @PathVariable String esName) throws Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject json = new JSONObject();

        Graph graph = application.getGraph(graphName);
        HierarchicalConfiguration conf = application.getStorageConfig(graphName);

        String elasticSearchHost = null;

        if (graph == null) {
            json.put("status", "error");
            json.put("msg", "unknown graph '" + graphName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (!(graph instanceof TitanGraph)) {
            json.put("status", "error");
            json.put("error", "graph is not a titan graph");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (indexName == null) {
            json.put("status", "error");
            json.put("msg", "unknown index '" + indexName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (esName == null) {
            json.put("status", "error");
            json.put("msg", "unknown elasticsearch index '" + esName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // build the index hostname key
        StringBuilder stringBuilderIndexHost = new StringBuilder();
        stringBuilderIndexHost.append("storage..index..");
        stringBuilderIndexHost.append(indexName);
        stringBuilderIndexHost.append("..hostname");
        String indexHost = stringBuilderIndexHost.toString();

        // build the index local-mode key
        StringBuilder stringBuilderLocalMode = new StringBuilder();
        stringBuilderLocalMode.append("storage..index..");
        stringBuilderLocalMode.append(indexName);
        stringBuilderLocalMode.append("..local-mode");
        String localMode = stringBuilderLocalMode.toString();

        // build the index-name key
        StringBuilder stringBuilderIndexName = new StringBuilder();
        stringBuilderIndexName.append("storage..index..");
        stringBuilderIndexName.append(indexName);
        stringBuilderIndexName.append("..index-name");
        String esIndexName = stringBuilderIndexName.toString();

        // check rexster keys
        boolean flag = false;
        final Iterator<String> rexsterConfigurationKeys = conf.getKeys();
        while (rexsterConfigurationKeys.hasNext()) {
            String key = rexsterConfigurationKeys.next();
            if (key.equals(indexHost)) {
                elasticSearchHost = conf.getString(key);
                flag = true;
            }
            else if (key.equals(localMode)) {
                elasticSearchHost = "127.0.0.1";
                flag = true;
            }
        }

        // if it didn't find either index hostname or local-mode for the specified index
        if (!flag) {
            json.put("status", "error");
            json.put("msg", "ElasticSearch index not found '" + indexName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // get elasticsearch index name
        flag = false;
        while (rexsterConfigurationKeys.hasNext()) {
            String key = rexsterConfigurationKeys.next();
            if (key.equals(indexName)) {
                esIndexName = conf.getString(key);
                flag = true;
            }
        }
        if (!flag) {
            esIndexName = "titan";
        }

        // build the elasticsearch url
        StringBuilder stringBuilderElasticSearchURL = new StringBuilder();
        stringBuilderElasticSearchURL.append("http://");
        stringBuilderElasticSearchURL.append(elasticSearchHost);
        stringBuilderElasticSearchURL.append(":9200/"+esIndexName+"/_mapping");
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
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        // return
        return new ResponseEntity<String>(jsonResult.toString(), responseHeaders, HttpStatus.OK);
    }


}
