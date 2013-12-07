package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseInputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseOutputFormat;
import com.thinkaurelius.faunus.mapreduce.FaunusCompiler;
import com.thinkaurelius.faunus.mapreduce.FaunusJobControl;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.JobStatus;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.lab41.dendrite.models.EdgeDegreesJobMetadata;
import org.lab41.dendrite.models.HadoopJobMetadata;
import org.lab41.dendrite.models.JobMetadata;
import org.lab41.dendrite.models.ProjectMetadata;
import org.lab41.dendrite.services.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
public class EdgeDegreesService {

    final static String NAME = "degrees";

    Logger logger = LoggerFactory.getLogger(EdgeDegreesService.class);

    @Autowired
    MetadataService metadataService;

    @Async
    public void countDegrees(String graphName, TitanGraph graph) throws Exception {

        ProjectMetadata project = metadataService.getProject(graphName);
        JobMetadata jobMetadata = metadataService.createJob(project);
        jobMetadata.setState(JobMetadata.State.RUNNING);
        metadataService.commit();

        logger.debug("Starting degree counting analysis on " + graphName + " " + Thread.currentThread().getName());

        createIndices(graph);

        // Create our temporary workspace
        Path tmpDir = new Path("dendrite/tmp/" + UUID.randomUUID() + "/");
        FileSystem fs = FileSystem.get(new Configuration());
        fs.mkdirs(tmpDir);
        fs.deleteOnExit(tmpDir);

        FaunusGraph faunusGraph = new FaunusGraph();
        FaunusPipeline exportPipeline =  exportGraphPipeline(graphName, faunusGraph, tmpDir);
        runPipeline(jobMetadata, exportPipeline);

        logger.debug("EdgeDegreesJobMetadata export finished");

        jobMetadata.setProgress(0);

        faunusGraph = faunusGraph.getNextGraph();
        FaunusPipeline importPipeline = importGraphPipeline(graphName, faunusGraph);
        runPipeline(jobMetadata, importPipeline);

        logger.debug("EdgeDegreesJobMetadata import finished");

        jobMetadata.setProgress(1);
        jobMetadata.setState(JobMetadata.State.DONE);
        metadataService.commit();
    }

    private void createIndices(TitanGraph graph) {

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
    }

    private FaunusPipeline exportGraphPipeline(String tableName, FaunusGraph faunusGraph, Path tmpDir) throws Exception {
        faunusGraph.getConf().set("mapred.jar", "../faunus/target/faunus-0.4.1-Lab41-SNAPSHOT-job.jar");

        faunusGraph.setGraphInputFormat(TitanHBaseInputFormat.class);
        faunusGraph.getConf().set("faunus.graph.input.titan.storage.backend", "hbase");
        faunusGraph.getConf().set("faunus.graph.input.titan.storage.hostname", "localhost");
        faunusGraph.getConf().set("faunus.graph.input.titan.storage.port", "2181");
        faunusGraph.getConf().set("faunus.graph.input.titan.storage.tablename", tableName);

        faunusGraph.setGraphOutputFormat(SequenceFileOutputFormat.class);
        faunusGraph.setSideEffectOutputFormat(TextOutputFormat.class);
        faunusGraph.setOutputLocation(new Path(tmpDir, "export"));
        faunusGraph.setOutputLocationOverwrite(true);

        return (new FaunusPipeline(faunusGraph)).V();
    }

    private FaunusPipeline importGraphPipeline(String tableName, FaunusGraph faunusGraph) throws Exception {
        faunusGraph.setGraphOutputFormat(TitanHBaseOutputFormat.class);
        faunusGraph.getConf().set("faunus.graph.output.titan.storage.backend", "hbase");
        faunusGraph.getConf().set("faunus.graph.output.titan.storage.hostname", "localhost");
        faunusGraph.getConf().set("faunus.graph.output.titan.storage.port", "2181");
        faunusGraph.getConf().set("faunus.graph.output.titan.storage.tablename", tableName);
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

        return (new FaunusPipeline(faunusGraph)).V().sideEffect(sideEffect);
    }

    private void runPipeline(JobMetadata jobMetadata, FaunusPipeline faunusPipeline) throws Exception {
        faunusPipeline.done();
        FaunusCompiler compiler = faunusPipeline.getCompiler();
        FaunusJobControl jobControl = new FaunusJobControl(faunusPipeline.getGraph(), compiler.getJobs());

        // Create nodes for all the job ids.
        for (Job hadoopJob: jobControl.getJobsInProgress()) {
            JobID jobId = hadoopJob.getJobID();
            metadataService.createHadoopJob(jobMetadata, jobId.toString());
        }

        jobMetadata.setProgress(0);
        metadataService.commit();

        Thread thread = new Thread(jobControl);
        thread.start();

        logger.debug("Submitted job");

        jobMetadata.setState(JobMetadata.State.RUNNING);
        metadataService.commit();

        while (jobControl.allFinished()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // ignore
            }

            Job hadoopJob = jobControl.getRunningJob();

            if (hadoopJob != null) {
                JobStatus status = hadoopJob.getStatus();
                float progress =
                        status.getSetupProgress() +
                        status.getMapProgress() +
                        status.getReduceProgress() +
                        status.getCleanupProgress();
                jobMetadata.setProgress(progress / 4);
                metadataService.commit();
            }
        }
    }
}
