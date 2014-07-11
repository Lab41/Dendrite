package org.lab41.dendrite.services.analysis;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.formats.adjacency.AdjacencyFileOutputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseInputFormat;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
                    .dataType(FullDouble.class)
                    .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
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
        fs.setPermission(tmpDir, new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.ALL, true));
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
        exportDir = new Path(exportDir, "job-0");
        importDir = new Path(importDir, "output");

        String graphlabTwillPath = config.getString("graphlab.twill.path") + "/bin/graphlab-twill";
        String zookeeperPath = config.getString("graphlab.twill.zookeeper.url");
        String algorithmPath = config.getString("graphlab.algorithm.path") + "/" + algorithm;
        String clusterSize = config.getString("graphlab.cluster-size");

        List<String> args = Lists.newArrayList(
                graphlabTwillPath,
                "-i", clusterSize,
                zookeeperPath,
                algorithmPath,
                exportDir.toString(),
                "adj",
                importDir.toString()
        );

        logger.debug("executing: " + args);

        ProcessBuilder processBuilder = new ProcessBuilder(args)
                .redirectErrorStream(true);

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charsets.US_ASCII))) {
            String line = reader.readLine();
            while (line != null) {
                logger.info(line);
                line = reader.readLine();
            }
        }

        int exitCode = process.waitFor();

        logger.debug("process exited with " + exitCode);
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
