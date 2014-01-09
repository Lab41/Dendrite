package org.lab41.dendrite.metagraph;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;

public class BranchMetadataTest extends BaseMetadataTest {

    ProjectMetadata projectMetadata;
    BranchMetadata branchMetadata;

    @Before
    public void setUp() {
        projectMetadata = tx.createProject("test");
        branchMetadata = projectMetadata.getCurrentBranch();
    }

    @After
    public void tearDown() {
        projectMetadata = null;
        branchMetadata = null;
    }


    @Test
    public void typeIsCorrect() {
        Assert.assertEquals(branchMetadata.asVertex().getProperty("type"), "branch");
    }
}
