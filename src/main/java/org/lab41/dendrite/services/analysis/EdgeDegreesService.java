package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseInputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseOutputFormat;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.lab41.dendrite.models.Job;
import org.lab41.dendrite.services.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EdgeDegreesService {

    Logger logger = LoggerFactory.getLogger(EdgeDegreesService.class);

    @Autowired
    MetadataService metadataService;

    @Async
    public void countDegrees(String graphName, TitanGraph graph, Job job) throws Exception {

        logger.debug("Starting degree counting analysis on " + graphName + " " + Thread.currentThread().getName());

        // Make sure our indexes exist.
        if (graph.getType("in_degrees") == null) {
            graph.makeKey("in_degrees").dataType(Integer.class).indexed(Vertex.class).make();
        }

        if (graph.getType("out_degrees") == null) {
            graph.makeKey("out_degrees").dataType(Integer.class).indexed(Vertex.class).make();
        }

        if (graph.getType("degrees") == null) {
            graph.makeKey("degrees").dataType(Integer.class).indexed(Vertex.class).make();
        }

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

        String sideEffect =
                "{ it ->\n" +
                "it.in_degrees = it.inE().count()\n" +
                "it.out_degrees = it.outE().count()\n" +
                "it.degrees = it.in_degrees + it.out_degrees\n" +
                "}";
        faunusPipeline = (new FaunusPipeline(faunusGraph)).V().sideEffect(sideEffect);

        job.setStatus("running");
        metadataService.commit();

        logger.debug("Submitting job");

        faunusPipeline.submit();

        logger.debug("Job finished");

        job.setStatus("done");
        metadataService.commit();
    }

}
