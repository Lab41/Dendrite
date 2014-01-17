package org.lab41.dendrite.metagraph.models;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.matchers.JUnitMatchers.hasItem;

public class GraphMetadataTest extends BaseMetadataTest {

    ProjectMetadata projectMetadata;
    GraphMetadata graphMetadata;

    @Before
    public void setUp() {
        super.setUp();
        projectMetadata = tx.createProject("test");
        graphMetadata = projectMetadata.getCurrentGraph();
    }

    @After
    public void tearDown() {
        projectMetadata = null;
        graphMetadata = null;
        super.tearDown();
    }

    @Test
    public void typeIsCorrect() {
        Assert.assertEquals(graphMetadata.asVertex().getProperty("type"), "graph");
    }

    @Test
    public void timesAreSet() {
        Assert.assertNotNull(graphMetadata.getCreationTime());
    }
}
