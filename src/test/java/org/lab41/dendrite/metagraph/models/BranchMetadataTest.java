package org.lab41.dendrite.metagraph.models;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BranchMetadataTest extends BaseMetadataTest {

    ProjectMetadata projectMetadata;
    BranchMetadata branchMetadata;

    @Before
    public void setUp() {
        super.setUp();
        projectMetadata = tx.createProject("test");
        branchMetadata = projectMetadata.getCurrentBranch();
    }

    @After
    public void tearDown() {
        projectMetadata = null;
        branchMetadata = null;
        super.tearDown();
    }

    @Test
    public void typeIsCorrect() {
        Assert.assertEquals(branchMetadata.asVertex().getProperty("type"), "branch");
    }
}
