package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.formats.edgelist.EdgeListOutputFormat;
import com.thinkaurelius.faunus.formats.titan.hbase.TitanHBaseInputFormat;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
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

import java.io.*;
import java.net.URI;
import java.util.*;

@Service
public class SnapService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(SnapService.class);
    private org.apache.commons.configuration.Configuration config;

    private static List<String> algorithms = Arrays.asList(
        "centrality"
    );

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    MetaGraphService metaGraphService;

    @Autowired
    FaunusPipelineService faunusPipelineService;

    @Value("${snap.properties}")
    String pathToProperties;

    @Async
    public void snapAlgorithm(DendriteGraph graph, String algorithm, String jobId) throws Exception {
        try {
            if (!algorithms.contains(algorithm)) {
                throw new Exception("invalid algorithm selected");
            }

            Resource resource = resourceLoader.getResource(pathToProperties);
            config = new PropertiesConfiguration(resource.getFile());

            logger.debug("Starting Snap "
                    + algorithm + " analysis on "
                    + graph.getId()
                    + " job " + jobId
                    + " " + Thread.currentThread().getName());

            setJobName(jobId, "snap_"+algorithm);
            setJobState(jobId, JobMetadata.RUNNING);

            // Make sure the indices exist.
            createIndices(graph, algorithm);

            run(graph, jobId, algorithm);
        } catch (Exception e) {
            logger.debug("snap-" + algorithm + ": error: ", e);
            e.printStackTrace();
            setJobState(jobId, JobMetadata.ERROR, e.getMessage());
            throw e;
        }

        setJobState(jobId, JobMetadata.DONE);

        logger.debug("Snap " + algorithm + ": finished job: " + jobId);
    }

    private void createIndices(DendriteGraph graph, String algorithm) {
        TitanTransaction tx = graph.newTransaction();

        if (algorithm.equals("centrality")) {
            if (tx.getType("snap_degree") == null) {
                tx.makeKey("snap_degree")
                        .dataType(FullDouble.class)
                        .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                        .make();
            }
            if (tx.getType("snap_closeness") == null) {
                tx.makeKey("snap_closeness")
                        .dataType(FullDouble.class)
                        .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                        .make();
            }
            if (tx.getType("snap_betweenness") == null) {
                tx.makeKey("snap_betweenness")
                        .dataType(FullDouble.class)
                        .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                        .make();
            }
            if (tx.getType("snap_eigenvector") == null) {
                tx.makeKey("snap_eigenvector")
                        .dataType(FullDouble.class)
                        .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                        .make();
            }
            if (tx.getType("snap_network_constraint") == null) {
                tx.makeKey("snap_network_constraint")
                        .dataType(FullDouble.class)
                        .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                        .make();
            }
            if (tx.getType("snap_clustering_coefficient") == null) {
                tx.makeKey("snap_clustering_coefficient")
                        .dataType(FullDouble.class)
                        .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                        .make();
            }
            if (tx.getType("snap_pagerank") == null) {
                tx.makeKey("snap_pagerank")
                        .dataType(FullDouble.class)
                        .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                        .make();
            }
            if (tx.getType("snap_hub_score") == null) {
                tx.makeKey("snap_hub_score")
                        .dataType(FullDouble.class)
                        .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                        .make();
            }
            if (tx.getType("snap_authority_score") == null) {
                tx.makeKey("snap_authority_score")
                        .dataType(FullDouble.class)
                        .indexed(DendriteGraph.INDEX_NAME, Vertex.class)
                        .make();
            }
        }

        tx.commit();
    }

    private void run(DendriteGraph graph, String jobId, String algorithm) throws Exception {
        logger.debug("starting snap analysis of '" + graph.getId() + "'");

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
            fs.setPermission(importDir, new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.ALL, true));

            runExport(graph, jobId, exportDir);
            runSnap(fs, exportDir, importDir, algorithm);

            // We don't need the export directory at this point.
            //fs.delete(exportDir, true);

            runImport(graph, fs, importDir, algorithm);
        } finally {
            // Clean up after ourselves.
            fs.delete(tmpDir, true);

            logger.debug("finished snap analysis of '" + graph.getId() + "'");
        }
    }

    private void runExport(DendriteGraph graph, String jobId, Path exportDir) throws Exception {
        FaunusGraph faunusGraph = new FaunusGraph();
        faunusGraph.setGraphInputFormat(TitanHBaseInputFormat.class);
        faunusGraph.setGraphOutputFormat(EdgeListOutputFormat.class);

        faunusPipelineService.configureGraph(faunusGraph, exportDir, graph);
        FaunusPipeline exportPipeline = new FaunusPipeline(faunusGraph);
        exportPipeline._();

        exportPipeline.done();
        FaunusJob faunusJob = new FaunusJob(metaGraphService.getMetaGraph(), jobId, exportPipeline);
        faunusJob.call();
    }

    private void runSnap(FileSystem fs, Path exportDir, Path importDir, String algorithm) throws Exception {
        URI uriImport = URI.create("file:///tmp/" + UUID.randomUUID().toString());
        URI uriExport = URI.create("file:///tmp/" + UUID.randomUUID().toString());
        Path tmpImportFile = new Path(uriImport);
        Path tmpExportFile = new Path(uriExport);
 
        exportDir = new Path(exportDir, "job-0/part-m-00000");
        importDir = new Path(importDir, "graph");
 
        fs.copyToLocalFile(exportDir, tmpExportFile);

        try {
            // feed output to snap as input
            String cmd = new Path(config.getString("metagraph.template.snap.algorithm-path"), algorithm) +
                         " -i:" + tmpExportFile.toString().substring(5) +
                         " -o:" + tmpImportFile.toString().substring(5);
                         
            logger.debug("running: " + cmd);

            Process p = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});

            int exitStatus = p.waitFor();

            logger.debug("snap finished with ", exitStatus);

            if (exitStatus != 0) {
                String stdout = IOUtils.toString(p.getInputStream());
                String stderr = IOUtils.toString(p.getErrorStream());

                throw new Exception("Snap process failed: [" + exitStatus + "]:\n" + stdout + "\n" + stderr);
            }
            fs.copyFromLocalFile(tmpImportFile, importDir);
        } finally {
            //tmpFile.delete();
        }
    }

    private void runImport(DendriteGraph graph, FileSystem fs, Path importDir, String algorithm) throws IOException {
        // FIXME this is due to the AdjacencyFileInputFormat not properly creating edges
        TitanTransaction tx = graph.newTransaction();

        try {
            for (FileStatus status: fs.listStatus(importDir)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(status.getPath())));
                String line;

                // get rid of header
                line = br.readLine();
                for (int i = 0; i < 3; i++) {
                    line = br.readLine();
                }

                while (line != null) {
                    String[] parts;
                    parts = line.split("\t");

                    String id = parts[0];
                    if (algorithm.equals("centrality")) {
                        double degree = Double.valueOf(parts[1]);
                        double closeness = Double.valueOf(parts[2]);
                        double betweenness = Double.valueOf(parts[3]);
                        double eigenvector = Double.valueOf(parts[4]);
                        double networkConstraint = Double.valueOf(parts[5]);
                        double clusteringCoefficient = Double.valueOf(parts[6]);
                        double pagerank = Double.valueOf(parts[7]);
                        double hubScore = Double.valueOf(parts[8]);
                        double authorityScore = Double.valueOf(parts[9]);

                        // feed snap output as input for updating each vertex
                        Vertex vertex = tx.getVertex(id);
                        vertex.setProperty("snap_degree", degree);
                        vertex.setProperty("snap_closeness", closeness);
                        vertex.setProperty("snap_betweenness", betweenness);
                        vertex.setProperty("snap_eigenvector", eigenvector);
                        vertex.setProperty("snap_network_constraint", networkConstraint);
                        vertex.setProperty("snap_clustering_coefficient", clusteringCoefficient);
                        vertex.setProperty("snap_pagerank", pagerank);
                        vertex.setProperty("snap_hub_score", hubScore);
                        vertex.setProperty("snap_authority_score", authorityScore);
                    } else {
                        double value = Double.valueOf(parts[1]);

                        // feed snap output as input for updating each vertex
                        Vertex vertex = tx.getVertex(id);
                        vertex.setProperty("snap_" + algorithm, value);
                    }
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
