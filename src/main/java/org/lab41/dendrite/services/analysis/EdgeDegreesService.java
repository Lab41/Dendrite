package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.formats.graphson.GraphSONOutputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseInputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseOutputFormat;
import com.thinkaurelius.faunus.mapreduce.FaunusCompiler;
import com.thinkaurelius.faunus.mapreduce.FaunusJobControl;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.JobStatus;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.lab41.dendrite.jobs.FaunusJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.*;
import org.lab41.dendrite.services.analysis.FaunusPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class EdgeDegreesService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(EdgeDegreesService.class);

    @Autowired
    FaunusPipelineService faunusPipelineService;

    @Async
    public void titanCountDegrees(DendriteGraph graph, String jobId) throws Exception {

        logger.debug("Starting Titan degree counting analysis on "
                + graph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "titan-degrees");
        setJobState(jobId, JobMetadata.RUNNING);

        createIndices(graph);

        TitanTransaction tx = graph.newTransaction();
        for (Vertex vertex: tx.getVertices()) {
            int inDegrees = 0;
            int outDegrees = 0;

            for (Edge edge: vertex.getEdges(Direction.IN)) {
                inDegrees += 1;
            }

            for (Edge edge: vertex.getEdges(Direction.OUT)) {
                outDegrees += 1;
            }

            vertex.setProperty("in_degrees", inDegrees);
            vertex.setProperty("out_degrees", outDegrees);
            vertex.setProperty("degrees", inDegrees + outDegrees);
        }
        tx.commit();

        setJobState(jobId, JobMetadata.DONE);

        logger.debug("titanCountDegrees: finished job: " + jobId);
    }

    @Async
    public void faunusCountDegrees(DendriteGraph graph, String jobId) throws Exception {

        logger.debug("Starting Faunus degree counting analysis on "
                + graph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "faunus-degrees");
        setJobState(jobId, JobMetadata.RUNNING);

        // Make sure the indices exist.
        createIndices(graph);

        try {
            runFaunus(graph, jobId);
        } catch (Exception e) {
            logger.debug("faunusCountDegrees: error: ", e);
            e.printStackTrace();
            setJobState(jobId, JobMetadata.ERROR, e.getMessage());
            throw e;
        }

        setJobState(jobId, JobMetadata.DONE);

        logger.debug("faunusCountDegrees: finished job: " + jobId);
    }


    private void createIndices(DendriteGraph graph) {
        TitanTransaction tx = graph.newTransaction();

        if (tx.getType("in_degrees") == null) {
            tx.makeKey("in_degrees")
                    .dataType(Integer.class)
                    .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                    .make();
        }

        if (tx.getType("out_degrees") == null) {
            tx.makeKey("out_degrees")
                    .dataType(Integer.class)
                    .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                    .make();
        }

        if (tx.getType("degrees") == null) {
            tx.makeKey("degrees")
                    .dataType(Integer.class)
                    .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                    .make();
        }

        tx.commit();
    }

    private void runFaunus(DendriteGraph graph, String jobId) throws Exception {
        // We do the edge counting in two passes. First, we count all the edges and write the graph to a sequence file.
        // Second, we load the graph back in filtering out all the edges. We do this because I haven't figured out a
        // way to count edges and filter them out at the same time.

        FileSystem fs = FileSystem.get(new Configuration());

        // Create the temporary directory.
        Path tmpDir = new Path("dendrite/tmp/" + UUID.randomUUID() + "/");
        fs.mkdirs(tmpDir);
        fs.deleteOnExit(tmpDir);

        try {
            FaunusGraph faunusExportGraph = new FaunusGraph();
            faunusPipelineService.configureGraph(faunusExportGraph, new Path(tmpDir, "export"), graph);

            faunusExportGraph.setGraphInputFormat(TitanHBaseInputFormat.class);

            // FIXME: https://github.com/thinkaurelius/faunus/issues/170. The sequence file would be more efficient,
            // but it doesn't seem to support vertex query filters.
            //faunusExportGraph.setGraphOutputFormat(SequenceFileOutputFormat.class);
            faunusExportGraph.setGraphOutputFormat(GraphSONOutputFormat.class);

            String sideEffect = "{ it ->\n" +
                    "it.in_degrees = it.inE().count()\n" +
                    "it.out_degrees = it.outE().count()\n" +
                    "it.degrees = it.in_degrees + it.out_degrees\n" +
                    "}";

            FaunusPipeline pipeline = new FaunusPipeline(faunusExportGraph);
            pipeline.V().sideEffect(sideEffect);
            pipeline.done();

            logger.debug("starting export of '" + graph.getId() + "'");

            FaunusJob faunusJob = new FaunusJob(metaGraphService.getMetaGraph(), jobId, pipeline);
            faunusJob.call();

            logger.debug("finished export of '" + graph.getId() + "'");

            // Filter out all the edges
            FaunusGraph faunusImportGraph = faunusExportGraph.getNextGraph();
            faunusPipelineService.configureGraph(faunusImportGraph, new Path(tmpDir, "import"), graph);

            faunusImportGraph.setGraphOutputFormat(TitanHBaseOutputFormat.class);
            faunusImportGraph.getConf().set("faunus.graph.input.vertex-query-filter", "v.query().limit(0)");

            pipeline = new FaunusPipeline(faunusImportGraph);
            pipeline.V();
            pipeline.done();

            logger.debug("starting import of '" + graph.getId() + "'");

            faunusJob = new FaunusJob(metaGraphService.getMetaGraph(), jobId, pipeline);
            faunusJob.call();

            logger.debug("finished import of '" + graph.getId() + "'");

        } finally {
            // Clean up after ourselves.
            fs.delete(tmpDir, true);
        }
    }
}
