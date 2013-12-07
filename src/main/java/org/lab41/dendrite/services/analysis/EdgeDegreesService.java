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
import org.lab41.dendrite.models.*;
import org.lab41.dendrite.rexster.DendriteRexsterApplication;
import org.lab41.dendrite.services.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class EdgeDegreesService {

    final static String NAME = "degrees";

    Logger logger = LoggerFactory.getLogger(EdgeDegreesService.class);

    @Autowired
    DendriteRexsterApplication application;

    @Autowired
    MetadataService metadataService;

    @Async
    public void countDegrees(GraphMetadata graphMetadata, JobMetadata jobMetadata) throws Exception {

        jobMetadata.setState(JobMetadata.State.RUNNING);
        metadataService.commit();

        String graphName = graphMetadata.getName();
        TitanGraph graph = (TitanGraph) application.getGraph(graphName);

        if (graph == null) {
            jobMetadata.setState(JobMetadata.State.ERROR);
            jobMetadata.setMessage("graph does not exist");
            metadataService.commit();
            return;
        }

        // Make sure our indexes exist.
        createIndices(graph);

        logger.debug("Starting degree counting analysis on " + graphName + " " + Thread.currentThread().getName());

        // Run the counter.
        Counter counter = new Counter(graphMetadata, jobMetadata);
        counter.run();
    }

    private void createIndices(TitanGraph graph) {
        boolean createdIndices = false;

        if (graph.getType("in_degrees") == null) {
            createdIndices = true;
            graph.makeKey("in_degrees").dataType(Integer.class).indexed(Vertex.class).make();
        }

        if (graph.getType("out_degrees") == null) {
            createdIndices = true;
            graph.makeKey("out_degrees").dataType(Integer.class).indexed(Vertex.class).make();
        }

        if (graph.getType("degrees") == null) {
            createdIndices = true;
            graph.makeKey("degrees").dataType(Integer.class).indexed(Vertex.class).make();
        }

        if (createdIndices) {
            graph.commit();
        }
    }

    private class Counter {
        private GraphMetadata graphMetadata;
        private JobMetadata jobMetadata;
        private Path tmpDir = createTempDir();
        private Map<JobID, JobMetadata> jobMap = new HashMap<>();
        private Set<JobID> doneJobs = new HashSet<>();

        public Counter(GraphMetadata graphMetadata, JobMetadata jobMetadata) throws IOException {
            this.graphMetadata = graphMetadata;
            this.jobMetadata = jobMetadata;
        }

        public void run() throws Exception {
            FaunusGraph faunusGraph = new FaunusGraph();
            FaunusPipeline exportPipeline =  exportGraphPipeline(faunusGraph);
            runPipeline(exportPipeline);

            logger.debug("EdgeDegreesJobMetadata export finished");

            faunusGraph = faunusGraph.getNextGraph();
            FaunusPipeline importPipeline = importGraphPipeline(faunusGraph);
            runPipeline(importPipeline);

            logger.debug("EdgeDegreesJobMetadata import finished");

            jobMetadata.setProgress(1);
            jobMetadata.setState(JobMetadata.State.DONE);
            metadataService.commit();
        }

        private Path createTempDir() throws IOException {
            Path tmpDir = new Path("dendrite/tmp/" + UUID.randomUUID() + "/");
            FileSystem fs = FileSystem.get(new Configuration());
            fs.mkdirs(tmpDir);
            fs.deleteOnExit(tmpDir);

            return tmpDir;
        }

        private FaunusPipeline exportGraphPipeline(FaunusGraph faunusGraph) {
            faunusGraph.getConf().set("mapred.jar", "../faunus/target/faunus-0.4.1-Lab41-SNAPSHOT-job.jar");

            faunusGraph.setGraphInputFormat(TitanHBaseInputFormat.class);
            faunusGraph.getConf().set("faunus.graph.input.titan.storage.backend", graphMetadata.getBackend());
            faunusGraph.getConf().set("faunus.graph.input.titan.storage.hostname", graphMetadata.getHostname());
            faunusGraph.getConf().setInt("faunus.graph.input.titan.storage.port", graphMetadata.getPort());
            faunusGraph.getConf().set("faunus.graph.input.titan.storage.tablename", graphMetadata.getTablename());

            faunusGraph.setGraphOutputFormat(SequenceFileOutputFormat.class);
            faunusGraph.setSideEffectOutputFormat(TextOutputFormat.class);
            faunusGraph.setOutputLocation(new Path(tmpDir, "export"));
            faunusGraph.setOutputLocationOverwrite(true);

            return (new FaunusPipeline(faunusGraph)).V();
        }

        private FaunusPipeline importGraphPipeline(FaunusGraph faunusGraph) {
            faunusGraph.setGraphOutputFormat(TitanHBaseOutputFormat.class);
            faunusGraph.getConf().set("faunus.graph.output.titan.storage.backend", graphMetadata.getBackend());
            faunusGraph.getConf().set("faunus.graph.output.titan.storage.hostname", graphMetadata.getHostname());
            faunusGraph.getConf().setInt("faunus.graph.output.titan.storage.port", graphMetadata.getPort());
            faunusGraph.getConf().set("faunus.graph.output.titan.storage.tablename", graphMetadata.getTablename());
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

        private void runPipeline(FaunusPipeline faunusPipeline) throws Exception {
            faunusPipeline.done();
            FaunusCompiler compiler = faunusPipeline.getCompiler();
            FaunusJobControl jobControl = new FaunusJobControl(faunusPipeline.getGraph(), compiler.getJobs());

            // Create nodes for all the job ids.
            for (Job hadoopJob: jobControl.getJobsInProgress()) {
                JobID jobId = hadoopJob.getJobID();
                JobMetadata childJobMetadata = metadataService.createJob(jobMetadata);
                childJobMetadata.setMapreduceJobId(jobId.toString());

                jobMap.put(jobId, childJobMetadata);
            }

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

                checkJobControl(jobControl);
            }

            checkJobControl(jobControl);
        }

        private void checkJobControl(FaunusJobControl jobControl) throws IOException, InterruptedException {
            for (Job hadoopJob: jobControl.getSuccessfulJobs()) {
                JobID jobId = hadoopJob.getJobID();

                if (!doneJobs.contains(jobId)) {
                    doneJobs.add(jobId);

                    JobMetadata hadoopJobMetadata = jobMap.get(jobId);
                    hadoopJobMetadata.setProgress(1);
                    hadoopJobMetadata.setState(JobMetadata.State.DONE);
                }
            }

            for (Job hadoopJob: jobControl.getFailedJobs()) {
                JobID jobId = hadoopJob.getJobID();

                if (!doneJobs.contains(jobId)) {
                    doneJobs.add(jobId);

                    JobMetadata jobMetadata = jobMap.get(jobId);
                    jobMetadata.setState(JobMetadata.State.ERROR);
                }
            }

            Job hadoopJob = jobControl.getRunningJob();

            if (hadoopJob != null) {
                JobID jobId = hadoopJob.getJobID();

                JobStatus status = hadoopJob.getStatus();
                float progress =
                        status.getSetupProgress() +
                                status.getMapProgress() +
                                status.getReduceProgress() +
                                status.getCleanupProgress();

                JobMetadata hadoopJobMetdata = jobMap.get(jobId);
                hadoopJobMetdata.setProgress(progress * 0.25f);
                metadataService.commit();
            }
        }
    }
}
