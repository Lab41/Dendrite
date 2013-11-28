package org.lab41.dendrite.web.controller.algorithms;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.formats.graphson.GraphSONOutputFormat;
import com.thinkaurelius.faunus.formats.noop.NoOpOutputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseInputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseOutputFormat;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.graphdb.types.TypeAttribute;
import com.tinkerpop.blueprints.Graph;

import com.tinkerpop.blueprints.Vertex;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
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

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Controller
public class DegreeController {

    @Autowired
    DendriteRexsterApplication application;

    @RequestMapping(value = "/api/{graphName}/algorithms/degree", method = RequestMethod.GET)
    public ResponseEntity<String> pageRank(@PathVariable String graphName) throws Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject json = new JSONObject();

        TitanGraph graph = (TitanGraph) application.getGraph(graphName);

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

        // Make sure we have a "degrees" index made.
        graph.makeKey("degrees").dataType(Integer.class).indexed(Vertex.class).make();

        FaunusGraph faunusGraph = new FaunusGraph();

        Path tmpDir = new Path("dendrite/tmp/" + UUID.randomUUID() + "/");
        FileSystem fs = FileSystem.get(faunusGraph.getConf());
        fs.mkdirs(tmpDir);
        fs.deleteOnExit(tmpDir);

        faunusGraph.getConf().set("mapred.jar", "../faunus/target/faunus-0.4.1-Lab41-SNAPSHOT-job.jar");

        faunusGraph.setGraphInputFormat(TitanHBaseInputFormat.class);
        faunusGraph.getConf().set("faunus.graph.input.titan.storage.backend", "hbase");
        faunusGraph.getConf().set("faunus.graph.input.titan.storage.hostname", "localhost");
        faunusGraph.getConf().set("faunus.graph.input.titan.storage.port", "2181");
        faunusGraph.getConf().set("faunus.graph.input.titan.storage.tablename", graphName);

        faunusGraph.setGraphOutputFormat(SequenceFileOutputFormat.class);
        faunusGraph.setSideEffectOutputFormat(TextOutputFormat.class);
        faunusGraph.setOutputLocation(new Path(tmpDir, "export"));
        faunusGraph.setOutputLocationOverwrite(true);

        FaunusPipeline faunusPipeline = (new FaunusPipeline(faunusGraph)).V();
        faunusPipeline.submit();

        faunusGraph = faunusGraph.getNextGraph();
        faunusGraph.setGraphOutputFormat(TitanHBaseOutputFormat.class);
        faunusGraph.getConf().set("faunus.graph.output.titan.storage.backend", "hbase");
        faunusGraph.getConf().set("faunus.graph.output.titan.storage.hostname", "localhost");
        faunusGraph.getConf().set("faunus.graph.output.titan.storage.port", "2181");
        faunusGraph.getConf().set("faunus.graph.output.titan.storage.tablename", graphName);
        faunusGraph.getConf().setBoolean("faunus.graph.output.titan.storage.read-only", false);
        faunusGraph.getConf().setBoolean("faunus.graph.output.titan.storage.batch-loading", false);
        faunusGraph.getConf().setBoolean("faunus.graph.output.titan.infer-schema", false);
        faunusGraph.getConf().set("faunus.graph.output.blueprints.script-file", "dendrite/dendrite-import.groovy");

        // Filter out all the edges
        faunusGraph.getConf().set("faunus.graph.input.vertex-query-filter", "v.query().limit(0)");

        faunusPipeline = (new FaunusPipeline(faunusGraph)).
                V().
                sideEffect("{ it.degrees = it.bothE().count() }");
        faunusPipeline.submit();

        return new ResponseEntity<String>(json.toString(), responseHeaders, HttpStatus.OK);
    }

}
