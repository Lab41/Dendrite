package org.lab41.web.controller.viz;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.diskstorage.es.ElasticSearchIndex;
import com.thinkaurelius.titan.graphdb.database.StandardTitanGraph;
import com.tinkerpop.blueprints.Graph;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.lab41.faunus.AdjacencyFileOutputFormat;
import org.lab41.rexster.DendriteRexsterApplication;
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

    @RequestMapping(value = "/api/{graphName}/viz/elasticsearch/{indexName}", method = RequestMethod.POST)
    public ResponseEntity<String> elasticSearch(@RequestBody String body, @PathVariable String graphName, @PathVariable String indexName) throws Exception {
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

        // check rexster keys
        int flag = 0;
        final Iterator<String> rexsterConfigurationKeys = conf.getKeys();
        while (rexsterConfigurationKeys.hasNext()) {
            String key = rexsterConfigurationKeys.next();
            if (key.equals(indexHost)) {
                elasticSearchHost = conf.getString(key);
                flag = 1;
            }
            else if (key.equals(localMode)) {
                elasticSearchHost = "127.0.0.1";
                flag = 1;
            }
        }

        // if it didn't find either index hostname or local-mode for the specified index
        if (flag == 0) {
            json.put("status", "error");
            json.put("msg", "ElasticSearch index not found '" + indexName + "'");
            return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

        // build the elasticsearch url
        StringBuilder stringBuilderElasticSearchURL = new StringBuilder();
        stringBuilderElasticSearchURL.append("http://");
        stringBuilderElasticSearchURL.append(elasticSearchHost);
        stringBuilderElasticSearchURL.append(":9200/");
        stringBuilderElasticSearchURL.append(indexName);
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

}
