package org.lab41.dendrite.jobs;

import com.thinkaurelius.titan.core.Mapping;
import com.thinkaurelius.titan.core.Parameter;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import junit.framework.Assert;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.*;
import org.lab41.dendrite.metagraph.*;
import org.lab41.dendrite.metagraph.models.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class BranchCommitSubsetJobTest {

    static MetaGraph metaGraph;
    String branchId;
    String jobId;
    DendriteGraph srcGraph;

    @BeforeClass
    public static void setUpClass() throws IOException {
        Configuration config = new BaseConfiguration();

        final Path esDir = Files.createTempDirectory("temp");

        // Register a runtime hook to delete the directory. We need to do this because the ES threads don't shut down
        // immediately after we close our titan connection.
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileUtils.deleteDirectory(esDir.toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

        UUID clusterName = UUID.randomUUID();

        config.setProperty("metagraph.system.name-prefix", "dendrite-test-");
        config.setProperty("metagraph.system.storage.backend", "inmemory");
        config.setProperty("metagraph.system.storage.index.backend", "elasticsearch");
        config.setProperty("metagraph.system.storage.index.directory", esDir.toString());
        config.setProperty("metagraph.system.storage.index.client-only", "false");
        config.setProperty("metagraph.system.storage.index.local-mode", "false");
        config.setProperty("metagraph.system.storage.index.cluster-name", clusterName.toString());

        config.setProperty("metagraph.template.name-prefix", "dendrite-test-");
        config.setProperty("metagraph.template.storage.backend", "inmemory");
        config.setProperty("metagraph.template.storage.index.backend", "elasticsearch");
        config.setProperty("metagraph.template.storage.index.hostname", "localhost:9300");
        config.setProperty("metagraph.template.storage.index.client-only", "true");
        config.setProperty("metagraph.template.storage.index.local-mode", "false");
        config.setProperty("metagraph.template.storage.index.sniff", "true");
        config.setProperty("metagraph.template.storage.index.cluster-name", clusterName.toString());

        metaGraph = new MetaGraph(config);
    }

    @AfterClass
    public static void tearDownClass() {
        metaGraph.stop();
    }

    @Before
    public void setUp() {
        MetaGraphTx metaGraphTx = metaGraph.newTransaction();

        UserMetadata userMetadata;
        ProjectMetadata projectMetadata;
        BranchMetadata branchMetadata;
        GraphMetadata srcGraphMetadata;
        JobMetadata jobMetadata;

        try {
            userMetadata = metaGraphTx.createUser("test");
            projectMetadata = metaGraphTx.createProject("test", userMetadata);
            branchMetadata = projectMetadata.getCurrentBranch();
            srcGraphMetadata = branchMetadata.getGraph();
            jobMetadata = metaGraphTx.createJob(projectMetadata);
        } finally {
            metaGraphTx.commit();
        }

        branchId = branchMetadata.getId();
        jobId = jobMetadata.getId();
        srcGraph = metaGraph.getGraph(srcGraphMetadata.getId());

        DendriteGraphTx srcTx;

        srcTx = srcGraph.newTransaction();
        try {
            srcTx.makeKey("name")
                    .dataType(String.class)
                    .indexed("search", Vertex.class, Parameter.of(Mapping.MAPPING_PREFIX, Mapping.STRING))
                    .make();
            srcTx.makeKey("age")
                    .dataType(Integer.class)
                    .indexed("search", Vertex.class)
                    .make();
            srcTx.makeLabel("friends").make();
            srcTx.makeLabel("enemies").make();
        } finally {
            srcTx.commit();
        }

        // Create a trivial graph.
        srcTx = srcGraph.newTransaction();
        try {
            Vertex srcAVertex = srcTx.addVertex(null);
            srcAVertex.setProperty("name", "A");
            srcAVertex.setProperty("age", 42);

            Vertex srcBVertex = srcTx.addVertex(null);
            srcBVertex.setProperty("name", "B");
            srcBVertex.setProperty("age", 50);

            Vertex srcCVertex = srcTx.addVertex(null);
            srcCVertex.setProperty("name", "C");
            srcCVertex.setProperty("age", 36);

            Vertex srcDVertex = srcTx.addVertex(null);
            srcDVertex.setProperty("name", "D");
            srcDVertex.setProperty("age", 10);

            srcTx.addEdge(null, srcAVertex, srcBVertex, "friends");
            srcTx.addEdge(null, srcBVertex, srcCVertex, "enemies");
            srcTx.addEdge(null, srcCVertex, srcDVertex, "friends");
        } finally {
            srcTx.commit();
        }
    }

    @After
    public void tearDown() throws IOException {
        srcGraph = null;
        branchId = null;
        jobId = null;
    }

    @Test
    public void testStep0() {
        BranchCommitSubsetJob branchCommitSubsetJob = new BranchCommitSubsetJob(
                metaGraph,
                jobId,
                branchId,
                "name:A",
                0);

        branchCommitSubsetJob.run();

        DendriteGraph dstGraph = metaGraph.getGraph(branchCommitSubsetJob.getDstGraphId());
        Assert.assertNotNull(dstGraph);

        DendriteGraphTx dstTx = dstGraph.newTransaction();

        try {
            Vertex dstAVertex = dstTx.getVertices("name", "A").iterator().next();
            Assert.assertNotNull(dstAVertex);
            Assert.assertEquals(dstAVertex.getProperty("name"), "A");
            Assert.assertEquals(dstAVertex.getProperty("age"), 42);

            Assert.assertFalse(dstAVertex.getEdges(Direction.BOTH).iterator().hasNext());

            Assert.assertFalse(dstTx.getVertices("name", "B").iterator().hasNext());
            Assert.assertFalse(dstTx.getVertices("name", "C").iterator().hasNext());
            Assert.assertFalse(dstTx.getVertices("name", "D").iterator().hasNext());
        } finally {
            dstTx.commit();
        }
    }

    @Test
    public void testStep1() {
        BranchCommitSubsetJob branchCommitSubsetJob = new BranchCommitSubsetJob(
                metaGraph,
                jobId,
                branchId,
                "name:A",
                1);

        branchCommitSubsetJob.run();

        DendriteGraph dstGraph = metaGraph.getGraph(branchCommitSubsetJob.getDstGraphId());
        Assert.assertNotNull(dstGraph);

        DendriteGraphTx dstTx = dstGraph.newTransaction();

        try {
            Vertex dstAVertex = dstTx.getVertices("name", "A").iterator().next();
            Assert.assertNotNull(dstAVertex);
            Assert.assertEquals(dstAVertex.getProperty("name"), "A");
            Assert.assertEquals(dstAVertex.getProperty("age"), 42);

            Vertex dstBVertex = dstTx.getVertices("name", "B").iterator().next();
            Assert.assertNotNull(dstBVertex);
            Assert.assertEquals(dstBVertex.getProperty("name"), "B");
            Assert.assertEquals(dstBVertex.getProperty("age"), 50);

            Edge dstABEdge = dstAVertex.getEdges(Direction.OUT).iterator().next();
            Assert.assertNotNull(dstABEdge);
            Assert.assertEquals(dstABEdge.getLabel(), "friends");

            Assert.assertFalse(dstBVertex.getEdges(Direction.OUT).iterator().hasNext());

            Assert.assertFalse(dstTx.getVertices("name", "C").iterator().hasNext());
            Assert.assertFalse(dstTx.getVertices("name", "D").iterator().hasNext());
        } finally {
            dstTx.commit();
        }
    }

    @Test
    public void testStep2() {
        BranchCommitSubsetJob branchCommitSubsetJob = new BranchCommitSubsetJob(
                metaGraph,
                jobId,
                branchId,
                "name:A",
                2);

        branchCommitSubsetJob.run();

        DendriteGraph dstGraph = metaGraph.getGraph(branchCommitSubsetJob.getDstGraphId());
        Assert.assertNotNull(dstGraph);

        DendriteGraphTx dstTx = dstGraph.newTransaction();

        try {
            Vertex dstAVertex = dstTx.getVertices("name", "A").iterator().next();
            Assert.assertNotNull(dstAVertex);
            Assert.assertEquals(dstAVertex.getProperty("name"), "A");
            Assert.assertEquals(dstAVertex.getProperty("age"), 42);

            Vertex dstBVertex = dstTx.getVertices("name", "B").iterator().next();
            Assert.assertNotNull(dstBVertex);
            Assert.assertEquals(dstBVertex.getProperty("name"), "B");
            Assert.assertEquals(dstBVertex.getProperty("age"), 50);

            Edge dstABEdge = dstAVertex.getEdges(Direction.OUT).iterator().next();
            Assert.assertNotNull(dstABEdge);
            Assert.assertEquals(dstABEdge.getLabel(), "friends");

            Vertex dstCVertex = dstTx.getVertices("name", "C").iterator().next();
            Assert.assertNotNull(dstCVertex);
            Assert.assertEquals(dstCVertex.getProperty("name"), "C");
            Assert.assertEquals(dstCVertex.getProperty("age"), 36);

            Edge dstBCEdge = dstBVertex.getEdges(Direction.OUT).iterator().next();
            Assert.assertNotNull(dstBCEdge);
            Assert.assertEquals(dstBCEdge.getLabel(), "enemies");

            Assert.assertFalse(dstCVertex.getEdges(Direction.OUT).iterator().hasNext());

            Assert.assertFalse(dstTx.getVertices("name", "D").iterator().hasNext());
        } finally {
            dstTx.commit();
        }
    }
}
