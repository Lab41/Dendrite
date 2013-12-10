package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseInputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseOutputFormat;
import com.thinkaurelius.faunus.mapreduce.FaunusCompiler;
import com.thinkaurelius.faunus.mapreduce.FaunusJobControl;
import com.thinkaurelius.titan.core.TitanException;
import com.thinkaurelius.titan.core.TitanGraph;
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
import org.lab41.dendrite.models.*;
import org.lab41.dendrite.rexster.DendriteRexsterApplication;
import org.lab41.dendrite.services.MetadataService;
import org.lab41.dendrite.services.MetadataTx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class EdgeDegreesService {

    Logger logger = LoggerFactory.getLogger(EdgeDegreesService.class);

    @Autowired
    DendriteRexsterApplication application;

    @Autowired
    MetadataService metadataService;

    @Async
    public void titanCountDegrees(String graphId, String jobId) throws Exception {

        logger.debug("Starting Titan degree counting analysis on "
                + graphId
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "titan-degrees");
        setJobState(jobId, JobMetadata.RUNNING);

        try {
            MetadataTx tx = metadataService.newTransaction();
            GraphMetadata graphMetadata = tx.getGraph(graphId);
            if (graphMetadata == null) {
                logger.debug("titanCountDegrees: could not find graph metadata", graphId);
                setJobState(jobId, JobMetadata.ERROR, "graph does not exist");
                return;
            }

            TitanGraph titanGraph = getTitanGraph(graphMetadata.getName());
            tx.commit();

            TitanCounter titanCounter = new TitanCounter(titanGraph, jobId);
            titanCounter.run();
        } catch (Exception e) {
            logger.debug("titanCountDegrees: error: ", e);
            e.printStackTrace();
            setJobState(jobId, JobMetadata.ERROR, e.getMessage());
            throw e;
        }

        setJobState(jobId, JobMetadata.DONE);

        logger.debug("titanCountDegrees: finished job: " + jobId);
    }

    @Async
    public void faunusCountDegrees(String graphId, String jobId) throws Exception {

        logger.debug("Starting Faunus degree counting analysis on "
                + graphId
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "faunus-degrees");
        setJobState(jobId, JobMetadata.RUNNING);

        try {
            MetadataTx tx = metadataService.newTransaction();
            GraphMetadata graphMetadata = tx.getGraph(graphId);
            JobMetadata jobMetadata = tx.getJob(jobId);

            // Make sure the indices exist.
            getTitanGraph(graphMetadata.getName());
            tx.commit();

            FaunusCounter faunusCounter = new FaunusCounter(graphId, jobId);
            faunusCounter.run();

            logger.debug("faunusCountDegrees: finished running job: " + jobMetadata.getId());
        } catch (Exception e) {
            logger.debug("faunusCountDegrees: error: ", e);
            e.printStackTrace();
            setJobState(jobId, JobMetadata.ERROR, e.getMessage());
            throw e;
        }

        setJobState(jobId, JobMetadata.DONE);

        logger.debug("faunusCountDegrees: finished job: " + jobId);
    }

    private void setJobName(String jobId, String name) {
        MetadataTx tx = metadataService.newTransaction();
        try {
            JobMetadata jobMetadata = tx.getJob(jobId);
            jobMetadata.setName(name);
            tx.commit();
        } catch (TitanException e) {
            logger.debug("exception", e);
            throw e;
        }
    }

    private void setJobState(String jobId, String state) {
        setJobState(jobId, state, null);
    }

    private void setJobState(String jobId, String state, String msg) {
        MetadataTx tx = metadataService.newTransaction();
        try {
            JobMetadata jobMetadata = tx.getJob(jobId);
            jobMetadata.setState(state);
            jobMetadata.setMessage(msg);

            if (state.equals(JobMetadata.DONE)) {
                jobMetadata.setProgress(1.0f);
            }

            tx.commit();
        } catch (TitanException e) {
            logger.debug("exception", e);
            throw e;
        }
    }

    private void setJobProgress(String jobId, float progress) {
        MetadataTx tx = metadataService.newTransaction();
        try {
            JobMetadata jobMetadata = tx.getJob(jobId);
            jobMetadata.setProgress(progress);
            tx.commit();
        } catch (TitanException e) {
            logger.debug("exception", e);
            throw e;
        }
    }

    private TitanGraph getTitanGraph(String graphName) throws Exception {
        TitanGraph graph = (TitanGraph) application.getGraph(graphName);
        if (graph == null) {
            throw new Exception("graph '" + graphName + "' does not exist");
        }

        if (graph.getType("in_degrees") == null) {
            graph.makeKey("in_degrees")
                    .dataType(Integer.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (graph.getType("out_degrees") == null) {
            graph.makeKey("out_degrees")
                    .dataType(Integer.class)
                    .indexed(Vertex.class)
                    .make();
        }

        if (graph.getType("degrees") == null) {
            graph.makeKey("degrees")
                    .dataType(Integer.class)
                    .indexed(Vertex.class)
                    .make();
        }

        return graph;
    }

    private class TitanCounter {
        private TitanGraph titanGraph;
        private String jobId;

        public TitanCounter(TitanGraph titanGraph, String jobId) {
            this.titanGraph = titanGraph;
            this.jobId = jobId;
        }

        public void run() {

            for (Vertex vertex: titanGraph.getVertices()) {
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

            titanGraph.commit();
        }
    }

    private class FaunusCounter {
        private String graphId;
        private String jobId;
        private Map<JobID, String> jobMap = new HashMap<>();
        private Set<JobID> doneJobs = new HashSet<>();

        public FaunusCounter(String graphId, String jobId) throws IOException {
            this.graphId = graphId;
            this.jobId = jobId;
        }

        public void run() throws Exception {
            FileSystem fs = FileSystem.get(new Configuration());

            // Create the temporary directory.
            Path tmpDir = new Path("dendrite/tmp/" + UUID.randomUUID() + "/");
            fs.mkdirs(tmpDir);
            fs.deleteOnExit(tmpDir);

            try {
                FaunusGraph faunusGraph = new FaunusGraph();
                FaunusPipeline exportPipeline =  exportGraphPipeline(faunusGraph, tmpDir);
                runPipeline(exportPipeline);

                logger.debug("EdgeDegreesJobMetadata export finished");

                faunusGraph = faunusGraph.getNextGraph();
                FaunusPipeline importPipeline = importGraphPipeline(faunusGraph);
                runPipeline(importPipeline);

                logger.debug("EdgeDegreesJobMetadata import finished");

                MetadataTx tx = metadataService.newTransaction();
                JobMetadata jobMetadata = tx.getJob(jobId);
                jobMetadata.setProgress(1);
                jobMetadata.setState(JobMetadata.DONE);
                tx.commit();
            } catch (Exception e) {
                logger.debug("exception", e);
                e.printStackTrace();
                throw e;
            } finally {
                // Clean up after ourselves.
                fs.delete(tmpDir, true);
            }
        }

        private FaunusPipeline exportGraphPipeline(FaunusGraph faunusGraph, Path tmpDir) {
            MetadataTx tx = metadataService.newTransaction();
            GraphMetadata graphMetadata = tx.getGraph(graphId);

            faunusGraph.getConf().set("mapred.jar", "../faunus/target/faunus-0.4.1-Lab41-SNAPSHOT-job.jar");

            faunusGraph.setGraphInputFormat(TitanHBaseInputFormat.class);
            faunusGraph.getConf().set("faunus.graph.input.titan.storage.backend", graphMetadata.getBackend());
            faunusGraph.getConf().set("faunus.graph.input.titan.storage.hostname", graphMetadata.getHostname());
            faunusGraph.getConf().setInt("faunus.graph.input.titan.storage.port", graphMetadata.getPort());
            faunusGraph.getConf().set("faunus.graph.input.titan.storage.tablename", graphMetadata.getTablename());

            faunusGraph.setGraphOutputFormat(SequenceFileOutputFormat.class);
            faunusGraph.setSideEffectOutputFormat(TextOutputFormat.class);
            faunusGraph.setOutputLocation(tmpDir);
            faunusGraph.setOutputLocationOverwrite(true);

            tx.commit();

            return (new FaunusPipeline(faunusGraph)).V();
        }

        private FaunusPipeline importGraphPipeline(FaunusGraph faunusGraph) {
            MetadataTx tx = metadataService.newTransaction();
            GraphMetadata graphMetadata = tx.getGraph(graphId);

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

            tx.commit();

            return (new FaunusPipeline(faunusGraph)).V().sideEffect(sideEffect);
        }

        private void runPipeline(FaunusPipeline faunusPipeline) throws Exception {
            faunusPipeline.done();
            FaunusCompiler compiler = faunusPipeline.getCompiler();
            FaunusJobControl jobControl = new FaunusJobControl(faunusPipeline.getGraph(), compiler.getJobs());

            Thread thread = new Thread(jobControl);
            thread.start();

            logger.debug("Submitted job");

            while (!jobControl.allFinished()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ignore
                }

                checkJobControl(jobControl);
            }

            checkJobControl(jobControl);
        }

        private void checkJobControl(FaunusJobControl jobControl) throws Exception {
            logger.debug("checking jobs");

            List<Job> jobsInProgress = jobControl.getJobsInProgress();
            List<Job> successfulJobs = jobControl.getSuccessfulJobs();
            List<Job> failedJobs = jobControl.getFailedJobs();

            for (Job hadoopJob: successfulJobs) {
                JobID hadoopJobId = hadoopJob.getJobID();
                logger.debug("found successful hadoop job:", hadoopJobId.toString());

                if (!doneJobs.contains(hadoopJobId)) {
                    doneJobs.add(hadoopJobId);

                    if (jobMap.containsKey(hadoopJobId)) {
                        setJobState(jobMap.get(hadoopJobId), JobMetadata.DONE);
                    } else {
                        MetadataTx tx = metadataService.newTransaction();
                        JobMetadata jobMetadata = tx.getJob(jobId);
                        JobMetadata childJobMetadata = tx.createJob(jobMetadata);
                        childJobMetadata.setState(JobMetadata.DONE);
                        childJobMetadata.setProgress(1.0f);
                        childJobMetadata.setMapreduceJobId(hadoopJobId.toString());
                        tx.commit();

                        jobMap.put(hadoopJobId, childJobMetadata.getId());
                    }
                }
            }

            for (Job hadoopJob: failedJobs) {
                JobID hadoopJobId = hadoopJob.getJobID();
                logger.debug("found failed hadoop job:", hadoopJobId.toString());

                if (jobMap.containsKey(hadoopJobId)) {
                    setJobState(jobMap.get(hadoopJobId), JobMetadata.ERROR);
                } else {
                    MetadataTx tx = metadataService.newTransaction();
                    JobMetadata jobMetadata = tx.getJob(jobId);
                    JobMetadata childJobMetadata = tx.createJob(jobMetadata);
                    childJobMetadata.setMapreduceJobId(hadoopJobId.toString());
                    childJobMetadata.setState(JobMetadata.ERROR);
                    tx.commit();

                    jobMap.put(hadoopJobId, childJobMetadata.getId());
                }
            }

            float totalProgress;

            // Don't divide by zero if we don't have any jobs in progress.
            if (jobsInProgress.isEmpty()) {
                totalProgress = 1;
            } else {
                totalProgress = ((float) successfulJobs.size()) / ((float) jobsInProgress.size());
            }

            Job hadoopRunningJob = jobControl.getRunningJob();
            if (hadoopRunningJob != null) {
                JobID hadoopJobId = hadoopRunningJob.getJobID();
                logger.debug("found active hadoop job:", hadoopJobId.toString());

                JobStatus status = hadoopRunningJob.getStatus();
                float progress = 0.25f * (
                        status.getSetupProgress() +
                        status.getMapProgress() +
                        status.getReduceProgress() +
                        status.getCleanupProgress());

                logger.debug("found progress: "
                        + status.getSetupProgress() + " "
                        + status.getMapProgress() + " "
                        + status.getReduceProgress() + " "
                        + status.getCleanupProgress() + " "
                        + progress);

                if (jobMap.containsKey(hadoopJobId)) {
                    setJobProgress(jobMap.get(hadoopJobId), progress);
                } else {
                    MetadataTx tx = metadataService.newTransaction();
                    JobMetadata jobMetadata = tx.getJob(jobId);
                    JobMetadata childJobMetadata = tx.createJob(jobMetadata);
                    childJobMetadata.setProgress(progress);
                    childJobMetadata.setState(JobMetadata.RUNNING);
                    childJobMetadata.setMapreduceJobId(hadoopJobId.toString());
                    tx.commit();

                    jobMap.put(hadoopJobId, childJobMetadata.getId());
                }

                String jobMetadataId = jobMap.get(hadoopJobId);
                setJobProgress(jobMetadataId, progress);
                totalProgress += (progress / ((float) jobsInProgress.size()));

                if (!failedJobs.isEmpty()) {
                    setJobState(jobMetadataId, JobMetadata.ERROR);
                }
            }

            setJobProgress(jobId, totalProgress);

            if (!failedJobs.isEmpty()) {
                throw new Exception("hadoop job failed");
            }
        }
    }
}
