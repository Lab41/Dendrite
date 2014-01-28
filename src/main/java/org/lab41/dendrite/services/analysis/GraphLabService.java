package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.formats.adjacency.AdjacencyFileOutputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseInputFormat;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.lab41.dendrite.jobs.FaunusJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class GraphLabService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(GraphLabService.class);
    private org.apache.commons.configuration.Configuration config;

    private static List<String> algorithms = Arrays.asList(
        //"approximate_diameter",
        "connected_component",
        "connected_component_stats",
        //"directed_triangle_count",
        //"eigen_vector_normalization",
        //"graph_laplacian",
        //"kcore",
        "pagerank",
        "partitioning",
        "simple_coloring",
        //"simple_undirected_triangle_count",
        "sssp",
        "TSC"
        //"undirected_triangle_count"
    );

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    MetaGraphService metaGraphService;

    @Autowired
    FaunusPipelineService faunusPipelineService;

    @Value("${graphlab.properties}")
    String pathToProperties;

    @Async
    public void graphLabAlgorithm(DendriteGraph graph, String algorithm, String jobId) throws Exception {
        try {
            if (!algorithms.contains(algorithm)) {
                throw new Exception("invalid algorithm selected");
            }

            Resource resource = resourceLoader.getResource(pathToProperties);
            config = new PropertiesConfiguration(resource.getFile());

            logger.debug("Starting GraphLab "
                    + algorithm + " analysis on "
                    + graph.getId()
                    + " job " + jobId
                    + " " + Thread.currentThread().getName());

            setJobName(jobId, "graphlab_"+algorithm);
            setJobState(jobId, JobMetadata.RUNNING);

            // Make sure the indices exist.
            createIndices(graph, algorithm);

            run(graph, jobId, algorithm);
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

    private void run(DendriteGraph graph, String jobId, String algorithm) throws Exception {
        logger.debug("starting graphlab analysis of '" + graph.getId() + "'");

        FileSystem fs = FileSystem.get(new Configuration());

        // Create the temporary directories.
        Path tmpDir = new Path(
                new Path(new Path(fs.getHomeDirectory(), "dendrite"), "tmp"),
                UUID.randomUUID().toString());

        fs.mkdirs(tmpDir);
        //fs.deleteOnExit(tmpDir);
        try {
            Path exportDir = new Path(tmpDir, "export");
            Path importDir = new Path(tmpDir, "import");

            fs.mkdirs(exportDir);
            fs.mkdirs(importDir);

            runExport(graph, jobId, exportDir);
            runGraphLab(fs, exportDir, importDir, algorithm);

            // We don't need the export directory at this point.
            //fs.delete(exportDir, true);

            runImport(graph, fs, importDir, algorithm);
        } finally {
            // Clean up after ourselves.
            fs.delete(tmpDir, true);

            logger.debug("finished graphlab analysis of '" + graph.getId() + "'");
        }
    }

    private void runExport(DendriteGraph graph, String jobId, Path exportDir) throws Exception {
        FaunusGraph faunusGraph = new FaunusGraph();
        faunusGraph.setGraphInputFormat(TitanHBaseInputFormat.class);
        faunusGraph.setGraphOutputFormat(AdjacencyFileOutputFormat.class);

        faunusPipelineService.configureGraph(faunusGraph, exportDir, graph);
        FaunusPipeline exportPipeline = new FaunusPipeline(faunusGraph);
        exportPipeline._();

        exportPipeline.done();
        FaunusJob faunusJob = new FaunusJob(metaGraphService.getMetaGraph(), jobId, exportPipeline);
        faunusJob.call();
    }

    private void runGraphLab(FileSystem fs, Path exportDir, Path importDir, String algorithm) throws Exception {
        File tmpFile = File.createTempFile("temp", "");

        exportDir = new Path(exportDir, "job-0");
        importDir = new Path(importDir, "output");

        try {
            // feed output to graphlab as input
            // !! NOTE requires the mpiexec client be on the dendrite server
            String cmd = "for i in `hadoop classpath | sed \"s/:/ /g\"` ;" +
                    " do echo $i;" +
                    " done | xargs | sed \"s/ /:/g\" > " +
                    tmpFile + " && " +
                    "export GRAPHLAB_CLASSPATH=`cat " +
                    tmpFile + "` && "+
                    "mpiexec " +
                    "-n " + config.getString("metagraph.template.graphlab.cluster-size") +
                    " -hostfile " + config.getString("metagraph.template.graphlab.hosts-file") +
                    " -x CLASSPATH=$GRAPHLAB_CLASSPATH " +
                    new Path(config.getString("metagraph.template.graphlab.algorithm-path"), algorithm) +
                    " --format adj" +
                    " --graph " + exportDir;

            // simple coloring uses a different cli syntax to declare the output.
            if (algorithm.equals("simple_coloring")) {
                cmd += " --output " + importDir;
            } else if (algorithm.equals("TSC")) {
                // do nothing.
            } else {
                cmd += " --saveprefix " + importDir;
            }

            logger.debug("running: " + cmd);

            Process p = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});

            // TSC outputs the results to stdout, so copy the results back into hdfs.
            if (algorithm.equals("TSC")) {
                FSDataOutputStream os = fs.create(exportDir);
                try {
                    IOUtils.copy(p.getInputStream(), os);
                } finally {
                    os.close();
                }
            }

            int exitStatus = p.waitFor();

            logger.debug("graphlab finished with ", exitStatus);

            if (exitStatus == 0) {
                if (algorithm.equals("TSC")) {

                }
            } else {
                String stdout = IOUtils.toString(p.getInputStream());
                String stderr = IOUtils.toString(p.getErrorStream());

                throw new Exception("GraphLab process failed: [" + exitStatus + "]:\n" + stdout + "\n" + stderr);
            }
        } finally {
            tmpFile.delete();
        }
    }

    private void runImport(DendriteGraph graph, FileSystem fs, Path importDir, String algorithm) throws IOException {
        // FIXME this is due to the AdjacencyFileInputFormat not properly creating edges
        TitanTransaction tx = graph.newTransaction();

        try {
            for (FileStatus status: fs.listStatus(importDir)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(status.getPath())));
                String line;
                line = br.readLine();
                while (line != null) {
                    String[] parts;
                    if (algorithm.equals("connected_component") || algorithm.equals("connected_component_stats")) {
                        parts = line.split(",");
                    } else {
                        parts = line.split("\t");
                    }

                    String id = parts[0];
                    double value = Double.valueOf(parts[1]);

                    // feed graphlab output as input for updating each vertex
                    Vertex vertex = tx.getVertex(id);
                    vertex.setProperty("graphlab_" + algorithm, value);

                    line = br.readLine();
                }
            }
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        tx.commit();
    }
}
