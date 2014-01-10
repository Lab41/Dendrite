package org.lab41.dendrite.metagraph;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;

import java.util.Set;

import static org.junit.matchers.JUnitMatchers.hasItem;

public class GraphMetadataTest extends BaseMetadataTest {

    ProjectMetadata projectMetadata;
    GraphMetadata graphMetadata;

    @Before
    public void setUp() {
        projectMetadata = tx.createProject("test");
        graphMetadata = projectMetadata.getCurrentGraph();
    }

    @After
    public void tearDown() {
        projectMetadata = null;
        graphMetadata = null;
    }

    @Test
    public void typeIsCorrect() {
        Assert.assertEquals(graphMetadata.asVertex().getProperty("type"), "graph");
    }
}
