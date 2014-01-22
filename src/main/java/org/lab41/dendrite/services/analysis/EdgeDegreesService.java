package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
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
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.*;
import org.lab41.dendrite.services.analysis.FaunusPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class EdgeDegreesService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(EdgeDegreesService.class);

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
            FaunusCounter faunusCounter = new FaunusCounter(graph, jobId);
            faunusCounter.run();
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
                    .indexed("search", Vertex.class)
                    .make();
        }

        if (tx.getType("out_degrees") == null) {
            tx.makeKey("out_degrees")
                    .dataType(Integer.class)
                    .indexed("search", Vertex.class)
                    .make();
        }

        if (tx.getType("degrees") == null) {
            tx.makeKey("degrees")
                    .dataType(Integer.class)
                    .indexed("search", Vertex.class)
                    .make();
        }

        tx.commit();
    }

    private class FaunusCounter {
        private DendriteGraph graph;
        private String jobId;
        private Map<JobID, String> jobMap = new HashMap<>();
        private Set<JobID> doneJobs = new HashSet<>();

        public FaunusCounter(DendriteGraph graph, String jobId) throws IOException {
            this.graph = graph;
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

                faunusGraph.setGraphInputFormat(TitanHBaseInputFormat.class);
                faunusGraph.setGraphOutputFormat(TitanHBaseOutputFormat.class);

                // Filter out all the edges
                faunusGraph.getConf().set("faunus.graph.input.vertex-query-filter", "v.query().limit(0)");

                String sideEffect =
                        "{ it ->\n" +
                                "it.in_degrees = it.inE().count()\n" +
                                "it.out_degrees = it.outE().count()\n" +
                                "it.degrees = it.in_degrees + it.out_degrees\n" +
                                "}";
                FaunusPipelineService faunusPipelineService = new FaunusPipelineService();
                FaunusPipeline exportPipeline = faunusPipelineService.graphPipeline(faunusGraph, tmpDir, graph);
                exportPipeline.V().sideEffect(sideEffect);

                logger.debug("starting export/import of '" + graph.getId() + "'");
                runPipeline(exportPipeline);
                logger.debug("finished export/import of '" + graph.getId() + "'");

                MetaGraphTx tx = metaGraphService.newTransaction();
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
                        MetaGraphTx tx = metaGraphService.newTransaction();
                        JobMetadata jobMetadata = tx.getJob(jobId);
                        JobMetadata childJobMetadata = tx.createJob(jobMetadata);
                        childJobMetadata.setName("faunus-hadoop-job");
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
                    MetaGraphTx tx = metaGraphService.newTransaction();
                    JobMetadata jobMetadata = tx.getJob(jobId);
                    JobMetadata childJobMetadata = tx.createJob(jobMetadata);
                    childJobMetadata.setName("faunus-hadoop-job");
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
                    MetaGraphTx tx = metaGraphService.newTransaction();
                    JobMetadata jobMetadata = tx.getJob(jobId);
                    JobMetadata childJobMetadata = tx.createJob(jobMetadata);
                    childJobMetadata.setName("faunus-hadoop-job");
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
