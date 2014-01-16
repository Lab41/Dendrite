package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.formats.adjacency.AdjacencyFileOutputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseInputFormat;
import com.thinkaurelius.faunus.mapreduce.FaunusCompiler;
import com.thinkaurelius.faunus.mapreduce.FaunusJobControl;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Vertex;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.JobStatus;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class GraphLabService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(GraphLabService.class);

    @Autowired
    MetaGraphService metaGraphService;

    @Async
    public void graphLabAlgorithm(DendriteGraph graph, String algorithm, String jobId) throws Exception {

        logger.debug("Starting GraphLab "
                + algorithm + " analysis on "
                + graph.getId()
                + " job " + jobId
                + " " + Thread.currentThread().getName());

        setJobName(jobId, "graphlab_"+algorithm);
        setJobState(jobId, JobMetadata.RUNNING);

        // Make sure the indices exist.
        createIndices(graph, algorithm);

        try {
            FaunusCounter faunusCounter = new FaunusCounter(graph, jobId, algorithm);
            faunusCounter.run();
        } catch (Exception e) {
            logger.debug("graphlab" + algorithm + ": error: ", e);
            e.printStackTrace();
            setJobState(jobId, JobMetadata.ERROR, e.getMessage());
            throw e;
        }

        setJobState(jobId, JobMetadata.DONE);

        logger.debug("GraphLab " + algorithm + ": finished job: " + jobId);
    }

    private void createIndices(DendriteGraph graph, String algorithm) {
        TitanTransaction tx = graph.newTransaction();

        if (tx.getType("graphlab_"+algorithm) == null) {
            tx.makeKey("graphlab_"+algorithm)
                    .dataType(Double.class)
                    .indexed("search", Vertex.class)
                    .make();
        }

        tx.commit();
    }

    private class FaunusCounter {
        private DendriteGraph graph;
        private String jobId;
        private String algorithm;
        private Map<JobID, String> jobMap = new HashMap<>();
        private Set<JobID> doneJobs = new HashSet<>();

        public FaunusCounter(DendriteGraph graph, String jobId, String algorithm) throws IOException {
            this.graph = graph;
            this.jobId = jobId;
            this.algorithm = algorithm;
        }

        public void run() throws Exception {
            FileSystem fs = FileSystem.get(new Configuration());

            // Create the temporary directories.
            UUID tmpDirUUID = UUID.randomUUID();
            UUID analysisUUID = UUID.randomUUID();
            Path tmpDir = new Path("/tmp/dendrite/" + tmpDirUUID + "/");
            Path jobDir = new Path("/tmp/dendrite/" + tmpDirUUID + "/job-0/");
            fs.mkdirs(tmpDir);
            //fs.deleteOnExit(tmpDir);
            try {

                FaunusGraph faunusGraph = new FaunusGraph();
                FaunusPipeline exportPipeline = graphPipeline(faunusGraph, tmpDir);

                logger.debug("starting graphlab analysis of '" + graph.getId() + "'");
                runPipeline(exportPipeline);

                // feed output to graphlab as input
                // !! NOTE requires the mpiexec client be on the dendrite server
                String cmd3 = "for i in `hadoop classpath | sed \"s/:/ /g\"` ; do echo $i;" +
                              " done | xargs | sed \"s/ /:/g\" > /tmp/classpath; " +
                              "export CLASSPATH=`cat /tmp/classpath`; "+
                              "mpiexec " +
                              "-n 1 " +
                              "-hostfile ~/graphlab_machine.txt " +
                              "-x CLASSPATH=$CLASSPATH " +
                              "~/graphlab/release/toolkits/graph_analytics/" +
                              algorithm + 
                              " --graph hdfs:///tmp/dendrite/" +
                              tmpDirUUID + "/job-0/ " + 
                              "--saveprefix hdfs:///tmp/dendrite/" + 
                              tmpDirUUID + "/" + analysisUUID;
                Process p3 = Runtime.getRuntime().exec(new String[]{"bash","-c",cmd3});
                p3.waitFor();
                    
                fs.delete(jobDir);
                // FIXME this is due to the AdjacencyFileInputFormat not properly creating edges
                FileStatus[] status = fs.listStatus(tmpDir);
                TitanTransaction ttx = graph.newTransaction();
                for (int i=0;i<status.length;i++){
                    BufferedReader br=new BufferedReader(new InputStreamReader(fs.open(status[i].getPath())));
                    String line;
                    line=br.readLine();
                    while (line != null){
                        String[] algo_array = line.split("\t");
                        // feed graphlab output as input for updating each vertex
                        Vertex vertex = ttx.getVertex(algo_array[0]);
                        vertex.setProperty("graphlab_"+algorithm, algo_array[1]);

                        line=br.readLine();
                    }
                }

                ttx.commit(); 

                logger.debug("finished graphlab analysis of '" + graph.getId() + "'");

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
                //fs.delete(tmpDir, true);
            }
        }

        private FaunusPipeline graphPipeline(FaunusGraph faunusGraph, Path tmpDir) {
            org.apache.commons.configuration.Configuration config = graph.getConfiguration();

            faunusGraph.setGraphInputFormat(TitanHBaseInputFormat.class);

            faunusGraph.getConf().set("mapred.jar", "../faunus/target/faunus-0.4.1-Lab41-SNAPSHOT-job.jar");

            Configuration faunusConfig = faunusGraph.getConf();

            setProp(faunusConfig, "faunus.graph.input.titan.storage.backend", config.getString("storage.backend", null));
            setProp(faunusConfig, "faunus.graph.input.titan.storage.hostname", config.getString("storage.hostname", null));
            setProp(faunusConfig, "faunus.graph.input.titan.storage.port", config.getString("storage.port", null));
            setProp(faunusConfig, "faunus.graph.input.titan.storage.tablename", config.getString("storage.tablename", null));

            setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.backend", config.getString("storage.index.search.backend", null));
            setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.client-only", config.getString("storage.index.search.client-only", null));
            setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.cluster-name", config.getString("storage.index.search.cluster-name", null));
            setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.directory", config.getString("storage.index.search.directory", null));
            setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.hostname", config.getString("storage.index.search.hostname", null));
            setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.index-name", config.getString("storage.index.search.index-name", null));
            setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.local-mode", config.getString("storage.index.search.local-mode", null));
            faunusGraph.setSideEffectOutputFormat(TextOutputFormat.class);
            faunusGraph.setGraphOutputFormat(AdjacencyFileOutputFormat.class);
            faunusGraph.setOutputLocation(tmpDir);
            faunusGraph.setOutputLocationOverwrite(true);

            setProp(faunusConfig, "faunus.graph.output.titan.storage.backend", config.getString("storage.backend", null));
            setProp(faunusConfig, "faunus.graph.output.titan.storage.hostname", config.getString("storage.hostname", null));
            setProp(faunusConfig, "faunus.graph.output.titan.storage.port", config.getString("storage.port", null));
            setProp(faunusConfig, "faunus.graph.output.titan.storage.tablename", config.getString("storage.tablename", null));
            faunusConfig.setBoolean("faunus.graph.output.titan.storage.read-only", false);
            faunusConfig.setBoolean("faunus.graph.output.titan.storage.batch-loading", false);

            // FIXME: https://github.com/thinkaurelius/faunus/issues/167. I would prefer to leave this off, but we end up tripping over an exception.
            //faunusConfig.setBoolean("faunus.graph.output.titan.infer-schema", false);
            faunusConfig.setBoolean("faunus.graph.output.titan.infer-schema", true);

            setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.backend", config.getString("storage.index.search.backend", null));
            setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.client-only", config.getString("storage.index.search.client-only", null));
            setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.cluster-name", config.getString("storage.index.search.cluster-name", null));
            setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.directory", config.getString("storage.index.search.directory", null));
            setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.hostname", config.getString("storage.index.search.hostname", null));
            setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.index-name", config.getString("storage.index.search.index-name", null));
            setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.local-mode", config.getString("storage.index.search.local-mode", null));

            faunusGraph.getConf().set("faunus.graph.output.blueprints.script-file", "dendrite/dendrite-import.groovy");

            return (new FaunusPipeline(faunusGraph))._();
        }

        private void setProp(Configuration config, String key, String value) {
            if (value != null) {
                config.set(key, value);
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
