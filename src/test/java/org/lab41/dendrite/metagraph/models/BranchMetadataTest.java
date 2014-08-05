package org.lab41.dendrite.metagraph.models;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class BranchMetadataTest extends BaseMetadataTest {

    UserMetadata userMetadata;
    ProjectMetadata projectMetadata;
    BranchMetadata branchMetadata;

    @Before
    public void setUp() {
        super.setUp();
        userMetadata = tx.createUser("test");
        projectMetadata = tx.createProject("test", userMetadata);
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

    @Test
    public void timesAreSet() {
        Date creationTime = branchMetadata.getCreationTime();
        Assert.assertNotNull(creationTime);

        Date modificationTime = branchMetadata.getModificationTime();
        Assert.assertNotNull(modificationTime);

        GraphMetadata graphMetadata = tx.createGraph(projectMetadata);
        branchMetadata.setGraph(graphMetadata);

        Assert.assertEquals(creationTime, branchMetadata.getCreationTime());
        Assert.assertNotSame(modificationTime, branchMetadata.getModificationTime());
    }
}
