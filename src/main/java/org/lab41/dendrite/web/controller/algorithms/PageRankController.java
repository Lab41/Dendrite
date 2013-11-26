package org.lab41.dendrite.web.controller.algorithms;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseInputFormat;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Graph;

import org.codehaus.jettison.json.JSONObject;
import org.lab41.dendrite.faunus.AdjacencyFileOutputFormat;
import org.lab41.dendrite.rexster.DendriteRexsterApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class PageRankController {

    @Autowired
    DendriteRexsterApplication application;

    @RequestMapping(value = "/api/{graphName}/algorithms/pagerank", method = RequestMethod.GET)
    public ResponseEntity<String> pageRank(@PathVariable String graphName) throws Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject json = new JSONObject();

        Graph graph = application.getGraph(graphName);

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

        FaunusGraph faunusGraph = new FaunusGraph();

        faunusGraph.setGraphInputFormat(TitanHBaseInputFormat.class);
        faunusGraph.getConf().set("faunus.graph.input.titan.storage.backend", "hbase");
        faunusGraph.getConf().set(TitanHBaseInputFormat.FAUNUS_GRAPH_INPUT_TITAN_STORAGE_HOSTNAME, "localhost");
        faunusGraph.getConf().set(TitanHBaseInputFormat.FAUNUS_GRAPH_INPUT_TITAN_STORAGE_PORT, "2181");
        faunusGraph.getConf().set(TitanHBaseInputFormat.FAUNUS_GRAPH_INPUT_TITAN_STORAGE_TABLENAME, graphName);
        faunusGraph.getConf().set("mapred.jar", "../faunus/target/faunus-0.4.0-Lab41-job.jar");

        faunusGraph.setGraphOutputFormat(AdjacencyFileOutputFormat.class);
        faunusGraph.getConf().set(FaunusGraph.FAUNUS_SIDEEFFECT_OUTPUT_FORMAT, "org.apache.hadoop.mapreduce.lib.output.TextOutputFormat");
        faunusGraph.setOutputLocation("output");
        faunusGraph.setOutputLocationOverwrite(true);

        FaunusPipeline faunusPipeline = (new FaunusPipeline(faunusGraph)).V().property("name").groupCount();
        faunusPipeline.submit();

        return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.OK);
    }

}
